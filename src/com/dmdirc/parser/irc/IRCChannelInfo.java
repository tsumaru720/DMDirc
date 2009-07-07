/*
 * Copyright (c) 2006-2009 Chris Smith, Shane Mc Cormack, Gregory Holmes
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.dmdirc.parser.irc;

import com.dmdirc.parser.interfaces.ChannelClientInfo;
import com.dmdirc.parser.interfaces.ChannelInfo;
import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.Parser;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

/**
 * Contains Channel information.
 * 
 * @author Shane Mc Cormack
 * @author Chris Smith
 * @see IRCParser
 */
public class IRCChannelInfo implements ChannelInfo {
	/**
	 * Boolean repreenting the status of names requests.
	 * When this is false, any new names reply will cause current known channelclients to be removed.
	 */
	private boolean bAddingNames = true;
	
	/** Unixtimestamp representing time when the channel was created. */
	private long nCreateTime = 0;
	
	/** Current known topic in the channel. */
	private String sTopic = "";
	/** Last known user to set the topic (Full host where possible). */
	private String sTopicUser = "";
	/** Unixtimestamp representing time when the topic was set. */
	private long nTopicTime = 0;
	
	/** Known boolean-modes for channel. */
	private long nModes;
	/** Reference to the parser object that owns this channel, Used for modes. */
	private final IRCParser myParser; // Reference to parser object that owns this channel. Used for Modes
	
	/** Channel Name. */
	private final String sName;
	
	/** Hashtable containing references to ChannelClients. */
	private final Map<String, IRCChannelClientInfo> hChannelUserList = new Hashtable<String, IRCChannelClientInfo>();
	/** Hashtable storing values for modes set in the channel that use parameters. */
	private final Map<Character, String> hParamModes = new Hashtable<Character, String>();
	/** Hashtable storing list modes. */
	private final Map<Character, ArrayList<ChannelListModeItem>> hListModes = new Hashtable<Character, ArrayList<ChannelListModeItem>>();
	/**
	 * LinkedList storing status of mode adding.
	 * if an item is in this list for a mode, we are expecting new items for the list
	 */
	private final List<Character> lAddingModes = new LinkedList<Character>();
	/** Modes waiting to be sent to the server. */
	private final List<String> lModeQueue = new LinkedList<String>();
	/** A Map to allow applications to attach misc data to this object */
	private Map myMap;
	
	/** Queue of requested list modes */
	private final Queue<Character> listModeQueue = new LinkedList<Character>();
	/** Listmode Queue Time */
	private long listModeQueueTime = System.currentTimeMillis();
	/** Have we asked the server for the list modes for this channel yet? */
	private boolean askedForListModes = false;
	/** Has OnChannelGotListModes ever been called for this channel? */
	private boolean hasGotListModes = false;

	/**
	 * Create a new channel object.
	 *
	 * @param tParser Refernce to parser that owns this channelclient (used for modes)	 
	 * @param name Channel name.
	 */
	public IRCChannelInfo(final IRCParser tParser, final String name) {
		myMap = new HashMap<Object, Object>();
		myParser = tParser;
		sName = name;
	}
	
	/**
	 * Get the listModeQueue.
	 *
	 * @return The listModeQueue
	 */
	public Queue<Character> getListModeQueue() {
		Queue<Character> result = listModeQueue;
		final long now = System.currentTimeMillis();
		// Incase of breakage, if getListModeQueue() was last called greater than
		// 60 seconds ago, we reset the list.
		if (now-(30*1000) > listModeQueueTime) {
			result = new LinkedList<Character>();
			myParser.callDebugInfo(IRCParser.DEBUG_LMQ, "Resetting LMQ");
		}
		listModeQueueTime = now;
		return result;
	}
	
	/**
	 * Ask the server for all the list modes for this channel.
	 */
	public void requestListModes() {
		final IRCChannelClientInfo me = getChannelClient(myParser.getLocalClient());
                
		if (me == null) {
			// In a normal situation of non bouncer-brokenness this won't happen
			return;
		}
		
		askedForListModes = true;
		
		final String thisIRCD = myParser.getIRCD(true).toLowerCase();
		final boolean isFreenode = (thisIRCD.equals("hyperion") || thisIRCD.equals("dancer"));
		final boolean isUnreal = thisIRCD.equals("unreal");
		final boolean isStarChat = thisIRCD.equals("starchat");
		final boolean isHybrid = thisIRCD.equals("hybrid") || thisIRCD.equals("oftc-hybrid");
		final boolean isCharybdis = thisIRCD.equals("charybdis");
		
		// We are considered opped if we have a mode higher than voice (or if we have any modes if voice doesn't exist)
		long voiceValue = 0;
		if (myParser.prefixModes.get('v') != null) { voiceValue = myParser.prefixModes.get('v');}
		final boolean isOpped = me.getImportantModeValue() > voiceValue;
		
		int modecount = 1;
		if (!isUnreal && myParser.h005Info.containsKey("MODES")) {
			try {
				modecount = Integer.parseInt(myParser.h005Info.get("MODES"));
			} catch (NumberFormatException e) { /* use default modecount */}
		}
		
		// Support for potential future decent mode listing in the protocol
		//
		// See my proposal: http://shane.dmdirc.com/listmodes.php
		// Add listmode handler
		final boolean supportLISTMODE = myParser.h005Info.containsKey("LISTMODE");
		
		String listmodes = "";
		int i = 0;
		for (Character cTemp : myParser.chanModesOther.keySet()) {
			final int nTemp = myParser.chanModesOther.get(cTemp);
			if (nTemp == IRCParser.MODE_LIST) {
				if ((isFreenode || isHybrid || isCharybdis) && (cTemp == 'e' || cTemp == 'I') && !isOpped) {
					// IRCD doesn't allow non-ops to ask for these modes.
					continue;
				} else if (isStarChat && cTemp == 'H') {
					// IRCD Denies the mode exists
					continue;
				}
				i++;
				listmodes = listmodes + cTemp;
				if (i >= modecount && !supportLISTMODE) {
					myParser.sendString("MODE "+getName()+" "+listmodes);
					i = 0;
					listmodes = "";
				}
			}
		}
		if (i > 0) {
			if (supportLISTMODE) {
				myParser.sendString("LISTMODE "+getName()+" "+listmodes);
			} else {
				myParser.sendString("MODE "+getName()+" "+listmodes);
			}
		}
	}
	
	/**
	 * Have we ever asked the server for this channels listmodes?
	 *
	 * @return True if requestListModes() has ever been used, else false
	 */
	public boolean hasAskedForListModes() {
		return askedForListModes;
	}
	
	/**
	 * Returns true if OnChannelGotListModes ever been called for this channel.
	 *
	 * @return True if OnChannelGotListModes ever been called for this channel.
	 */
	public boolean hasGotListModes() {
		return hasGotListModes;
	}
	
	/**
	 * Set if OnChannelGotListModes ever been called for this channel.
	 *
	 * @param newValue new value for if OnChannelGotListModes ever been called for this channel.
	 */
	protected void setHasGotListModes(final boolean newValue) {
		hasGotListModes = newValue;
	}
	
	/**
	 * Set the Map object attatched to this object.
	 *
	 * @param newMap New Map to attatch.
	 */
	public void setMap(final Map newMap) {
		myMap = newMap;
	}
	
	/**
	 * Get the Map object attatched to this object.
	 *
	 * @return Map to attatched to this.
	 */
	public Map getMap() {
		return myMap;
	}
	
	/**
	 * Set if we are getting a names request or not.
	 *
	 * @param newValue if false, any new names reply will cause current known channelclients to be removed.
	 */
	public void setAddingNames(final boolean newValue) { bAddingNames = newValue; }
	
	/**
	 * Get if we are getting a names request or not.
	 *
	 * @return if false, any new names reply will cause current known channelclients to be removed.
	 */
	public boolean isAddingNames() { return bAddingNames; }
	
	/** {@inheritDoc} */
        @Override
	public String getName() { return sName; }

        /** {@inheritDoc} */
        @Override
	public int getChannelClientCount() { return hChannelUserList.size(); }
	
	/** {@inheritDoc} */
        @Override
	public Collection<ChannelClientInfo> getChannelClients() {
		return new ArrayList<ChannelClientInfo>(hChannelUserList.values());
	}
	
	/**
	 * Empty the channel (Remove all known channelclients).
	 */
	protected void emptyChannel() {
		IRCClientInfo cTemp = null;
		for (IRCChannelClientInfo client : hChannelUserList.values()) {
			cTemp = client.getClient();
			cTemp.delChannelClientInfo(client);
			if (cTemp != myParser.getLocalClient() && !cTemp.checkVisibility()) {
				myParser.removeClient(cTemp);
			}
		}
		hChannelUserList.clear();
	}

	/** {@inheritDoc} */
        @Override
	public IRCChannelClientInfo getChannelClient(final String sWho) {
		return getChannelClient(sWho, false);
	}
	
	/** {@inheritDoc} */
        @Override
	public IRCChannelClientInfo getChannelClient(final String sWho, final boolean createFake) {
		final String who = myParser.getStringConverter().toLowerCase(IRCClientInfo.parseHost(sWho));
		if (hChannelUserList.containsKey(who)) {
			return hChannelUserList.get(who);
		}
		if (createFake) {
			return new IRCChannelClientInfo(myParser, (new IRCClientInfo(myParser, sWho)).setFake(true), this);
		} else {
			return null;
		}
	}
	
	/** {@inheritDoc} */
        @Override
	public IRCChannelClientInfo getChannelClient(final ClientInfo cWho) {
		for (IRCChannelClientInfo client : hChannelUserList.values()) {
			if (client.getClient() == cWho) {
				return client;
			}
		}
		return null;
	}
	
	/**
	 * Get the ChannelClientInfo object associated with a ClientInfo object.
	 *
	 * @param cClient Client object to be added to channel
	 * @return ChannelClientInfo object added, or an existing object if already known on channel
	 */
	protected IRCChannelClientInfo addClient(final IRCClientInfo cClient) {
		IRCChannelClientInfo cTemp = getChannelClient(cClient);
		if (cTemp == null) { 
			cTemp = new IRCChannelClientInfo(myParser, cClient, this);
			hChannelUserList.put(myParser.getStringConverter().toLowerCase(cTemp.getClient().getNickname()), cTemp);
		}
		return cTemp;
	}
	
	/**
	 * Remove ChannelClientInfo object associated with a ClientInfo object.
	 *
	 * @param cClient Client object to be removed from channel
	 */	
	protected void delClient(final IRCClientInfo cClient) {
		IRCChannelClientInfo cTemp = getChannelClient(cClient);
		if (cTemp != null) {
			final IRCClientInfo clTemp = cTemp.getClient();
			clTemp.delChannelClientInfo(cTemp);
			if (clTemp != myParser.getLocalClient() && !clTemp.checkVisibility()) {
				myParser.removeClient(clTemp);
			}
			hChannelUserList.remove(myParser.getStringConverter().toLowerCase(cTemp.getClient().getNickname()));
		}
	}	
	
	/**
	 * Rename a channelClient.
	 *
	 * @param oldNickname Nickname client used to be known as
	 * @param cChannelClient ChannelClient object with updated client object
	 */	
	protected void renameClient(final String oldNickname, final IRCChannelClientInfo cChannelClient) {
		if (hChannelUserList.containsKey(oldNickname)) {
			final IRCChannelClientInfo cTemp = hChannelUserList.get(oldNickname);
			if (cTemp == cChannelClient) {
				// Remove the old key
				hChannelUserList.remove(oldNickname);
				// Add with the new key. (getNickname will return the new name not the
				// old one)
				hChannelUserList.put(myParser.getStringConverter().toLowerCase(cTemp.getClient().getNickname()), cTemp);
			}
		}
	}
	
	/**
	 * Set the create time.
	 *
	 * @param nNewTime New unixtimestamp time for the channel creation (Seconds since epoch, not milliseconds)
	 */
	protected void setCreateTime(final long nNewTime) { nCreateTime = nNewTime; }
	/**
	 * Get the Create time.
	 *
	 * @return Unixtimestamp time for the channel creation (Seconds since epoch, not milliseconds)
	 */
	public long getCreateTime() { return nCreateTime; }	
	
	/**
	 * Set the topic time.
	 *
	 * @param nNewTime New unixtimestamp time for the topic (Seconds since epoch, not milliseconds)
	 */
	protected void setTopicTime(final long nNewTime) { nTopicTime = nNewTime; }

        /** {@inheritDoc} */
        @Override
	public long getTopicTime() { return nTopicTime; }	
	
	/**
	 * Set the topic.
	 *
	 * @param sNewTopic New contents of topic
	 */	
	protected void setTopic(final String sNewTopic) { sTopic = sNewTopic; }
	
        /** {@inheritDoc} */
        @Override
	public String getTopic() { return sTopic; }	

	/**
	 * Set the topic creator.
	 *
	 * @param sNewUser New user who set the topic (nickname if gotten on connect, full host if seen by parser)
	 */	
	protected void setTopicUser(final String sNewUser) { sTopicUser = sNewUser; }
	
        /** {@inheritDoc} */
        @Override
	public String getTopicSetter() { return sTopicUser; }
	
	/**
	 * Set the channel modes (as an integer).
	 *
	 * @param nNewMode new long representing channel modes. (Boolean only)
	 */	
	protected void setMode(final long nNewMode) { nModes = nNewMode; }
	/**
	 * Get the channel modes (as an integer).
	 *
	 * @return long representing channel modes. (Boolean only)
	 */	
	public long getMode() { return nModes; }	
	
	/** {@inheritDoc} */
        @Override
	public String getModes() {
		final StringBuilder sModes = new StringBuilder("+");
		final StringBuilder sModeParams = new StringBuilder();
		String sTemp = "";
		long nTemp = 0;
		final long nChanModes = this.getMode();
		for (char cTemp : myParser.chanModesBool.keySet()) {
			nTemp = myParser.chanModesBool.get(cTemp);
			if ((nChanModes & nTemp) == nTemp) { sModes.append(cTemp); }
		}
		for (char cTemp : hParamModes.keySet()) {
			sTemp = hParamModes.get(cTemp);
			if (!sTemp.isEmpty()) {
				sModes.append(cTemp);
				sModeParams.append(" ").append(this.getMode(cTemp));
 			}
		}
		
		return sModes.append(sModeParams).toString();
	}	
	
	/**
	 * Set a channel mode that requires a parameter.
	 *
	 * @param cMode Character representing mode
	 * @param sValue String repreenting value (if "" mode is unset)
	 */	
	protected void setModeParam(final Character cMode, final String sValue) { 
		if (sValue.isEmpty()) {
			if (hParamModes.containsKey(cMode)) {
				hParamModes.remove(cMode);
			}
		} else {
			hParamModes.put(cMode, sValue);
		}
	}
        
	/** {@inheritDoc} */
        @Override
	public String getMode(final char cMode) {
		if (hParamModes.containsKey(cMode)) { 
			return hParamModes.get(cMode); 
		}
		return "";
	}
	
	/**
	 * Add/Remove a value to a channel list.
	 *
	 * @param givenMode Character representing mode
	 * @param givenItem ChannelListModeItem representing the item
	 * @param bAdd Add or remove the value. (true for add, false for remove)
	 */
	protected void setListModeParam(final Character givenMode, final ChannelListModeItem givenItem, final boolean bAdd) { 
		Character cMode = givenMode;
		ChannelListModeItem newItem = givenItem;
		if (!myParser.chanModesOther.containsKey(cMode) || myParser.chanModesOther.get(cMode) != IRCParser.MODE_LIST) { return; }
		
		// Hyperion sucks.
		if (cMode == 'b' || cMode == 'q') {
			final String thisIRCD = myParser.getIRCD(true).toLowerCase();
			if ((thisIRCD.equals("hyperion") || thisIRCD.equals("dancer"))) {
				if (cMode == 'b' && givenItem.getItem().charAt(0) == '%') {
					cMode = 'q';
				} else if (cMode == 'q' && givenItem.getItem().charAt(0) != '%') {
					cMode = 'b';
				}
				if (givenItem.getItem().charAt(0) == '%') {
					newItem = new ChannelListModeItem(givenItem.getItem().substring(1), givenItem.getOwner(), givenItem.getTime());
				}
			}
		}
		
		if (!hListModes.containsKey(cMode)) { 
			hListModes.put(cMode, new ArrayList<ChannelListModeItem>());	
		}
		final ArrayList<ChannelListModeItem> lModes = hListModes.get(cMode);
		for (int i = 0; i < lModes.size(); i++) {
			if (myParser.getStringConverter().equalsIgnoreCase(lModes.get(i).getItem(), newItem.getItem())) {
				if (bAdd) { return; }
				else { 
					lModes.remove(i);
					break;
				}
			}
		}
		if (bAdd) { lModes.add(newItem); }
	}
	
	/** {@inheritDoc} */
        @Override
	public Collection<ChannelListModeItem> getListMode(final char cMode) {
		if (!myParser.chanModesOther.containsKey(cMode) || myParser.chanModesOther.get(cMode) != IRCParser.MODE_LIST) { return null; }
		
		if (!hListModes.containsKey(cMode)) { 
			hListModes.put(cMode, new ArrayList<ChannelListModeItem>());
		}
		return hListModes.get(cMode);
	}
	
	/**
	 * Get the "adding state" of a list mode.
	 * 
	 * @param cMode Character representing mode 
	 * @return false if we are not expecting a 367 etc, else true.
	 */
	public boolean getAddState(final Character cMode) { 
		synchronized (lAddingModes) {
			return lAddingModes.contains(cMode);
		}
	}
	
	/**
	 * Get the "adding state" of a list mode.
	 * 
	 * @param cMode Character representing mode
	 * @param newState change the value returned by getAddState
	 */
	protected void setAddState(final Character cMode, final boolean newState) { 
		synchronized (lAddingModes) {
			if (newState) {
				lAddingModes.add(cMode);
			} else {
				if (lAddingModes.contains(cMode)) { lAddingModes.remove(cMode); }
			}
		}
	}
	
	/**
	 * Reset the "adding state" of *all* list modes.
	 */
	protected void resetAddState() {
		synchronized (lAddingModes) {
			lAddingModes.clear();
		}
	}
	
	/** {@inheritDoc} */
        @Override
	public void alterMode(final boolean positive, final Character mode, final String parameter) { 
		int modecount = 1;
		int modeint = 0;
		String modestr = "";
		if (myParser.h005Info.containsKey("MODES")) {
			try {
				modecount = Integer.parseInt(myParser.h005Info.get("MODES")); 
			} catch (NumberFormatException e) { 
				modecount = 1; 
			}
		}
		if (!myParser.isUserSettable(mode)) { return; }

		modestr = ((positive) ? "+" : "-") + mode;
		if (myParser.chanModesBool.containsKey(mode)) {
			final String teststr = ((positive) ? "-" : "+") + mode;
			if (lModeQueue.contains(teststr)) {
				lModeQueue.remove(teststr);
				return;
			} else if (lModeQueue.contains(modestr)) {
				return;
			}
		} else {
			// May need a param
			if (myParser.prefixModes.containsKey(mode)) {
				modestr = modestr + " " + parameter;
			} else {
				modeint = myParser.chanModesOther.get(mode);
				if ((modeint & IRCParser.MODE_LIST) == IRCParser.MODE_LIST) {
					modestr = modestr + " " + parameter;
				} else if (!positive && ((modeint & IRCParser.MODE_UNSET) == IRCParser.MODE_UNSET)) {
					modestr = modestr + " " + parameter;
				} else if (positive && ((modeint & IRCParser.MODE_SET) == IRCParser.MODE_SET)) {
					// Does mode require a param to unset aswell?
					// We might need to queue an unset first
					if (((modeint & IRCParser.MODE_UNSET) == IRCParser.MODE_UNSET)) {
						final String existingParam = getMode(mode);
						if (!existingParam.isEmpty()) {
							final String reverseModeStr = "-" + mode + " " + existingParam;
							
							myParser.callDebugInfo(IRCParser.DEBUG_INFO, "Queueing mode: %s", reverseModeStr);
							lModeQueue.add(reverseModeStr);
							if (lModeQueue.size() == modecount) { flushModes(); }
						}
					}
					modestr = modestr + " " + parameter;
				}
			}
		}
		myParser.callDebugInfo(IRCParser.DEBUG_INFO, "Queueing mode: %s", modestr);
		lModeQueue.add(modestr);
		if (lModeQueue.size() == modecount) { flushModes(); }
	}
	
	/** {@inheritDoc} */
        @Override
	public void flushModes() {
		if (lModeQueue.isEmpty()) { return; }
		final StringBuilder positivemode = new StringBuilder();
		final StringBuilder positiveparam = new StringBuilder();
		final StringBuilder negativemode = new StringBuilder();
		final StringBuilder negativeparam = new StringBuilder();
		final StringBuilder sendModeStr = new StringBuilder();
		String modestr;
		String[] modeparam;
		boolean positive;
		for (int i = 0; i < lModeQueue.size(); ++i) {
			modeparam = lModeQueue.get(i).split(" ");
			modestr = modeparam[0];
			positive = modestr.charAt(0) == '+';
			if (positive) {
				positivemode.append(modestr.charAt(1));
				if (modeparam.length > 1) { positiveparam.append(" ").append(modeparam[1]); }
			} else {
				negativemode.append(modestr.charAt(1));
				if (modeparam.length > 1) { negativeparam.append(" ").append(modeparam[1]); }
			}
		}
		if (negativemode.length() > 0) { sendModeStr.append("-").append(negativemode); }
		if (positivemode.length() > 0) { sendModeStr.append("+").append(positivemode); }
		if (negativeparam.length() > 0) { sendModeStr.append(negativeparam); }
		if (positiveparam.length() > 0) { sendModeStr.append(positiveparam); }
		myParser.callDebugInfo(IRCParser.DEBUG_INFO, "Sending mode: %s", sendModeStr.toString());
		myParser.sendRawMessage("MODE " + sName + " " + sendModeStr.toString());
		clearModeQueue();
	}
	
	/**
	 * This function will clear the mode queue (WITHOUT Sending).
	 */
	public void clearModeQueue() { 
		lModeQueue.clear();
	}
	
	/** {@inheritDoc} */
        @Override
	public void sendMessage(final String message) {
		if (message.isEmpty()) { return; }
		
		myParser.sendString("PRIVMSG " + sName + " :" + message);
	}
	
	/**
	 * Send a notice message to a target.
	 *
	 * @param sMessage Message to send
	 */
	public void sendNotice(final String sMessage) { 
		if (sMessage.isEmpty()) { return; }
		
		myParser.sendString("NOTICE " + sName + " :" + sMessage);	
	}

	/** {@inheritDoc} */
        @Override
	public void sendAction(final String action) {
		if (action.isEmpty()) { return; }
		sendCTCP("ACTION", action);
	}
	
	/**
	 * Send a CTCP to a target.
	 *
	 * @param sType Type of CTCP
	 * @param sMessage Optional Additional Parameters
	 */
	public void sendCTCP(final String sType, String sMessage) { 
		if (sType.isEmpty()) { return; }
		final char char1 = (char) 1;
		if (!sMessage.isEmpty()) { sMessage = " " + sMessage; }
		sendMessage(char1 + sType.toUpperCase() + sMessage + char1);
	}
	
	/**
	 * Send a CTCPReply to a target.
	 *
	 * @param sType Type of CTCP
	 * @param sMessage Optional Additional Parameters
	 */
	public void sendCTCPReply(final String sType, String sMessage) { 
		if (sType.isEmpty()) { return; }
		final char char1 = (char) 1;
		if (!sMessage.isEmpty()) { sMessage = " " + sMessage; }
		sendNotice(char1 + sType.toUpperCase() + sMessage + char1);	
	}
	
	/**
	 * Get a string representation of the Channel.
	 *
	 * @return String representation of the Channel.
	 */
	@Override
	public String toString() { return sName; }
	
	/** {@inheritDoc} */
        @Override
	public Parser getParser() { return myParser; }

        /** {@inheritDoc} */
        @Override
        public void part(final String reason) {
            myParser.partChannel(sName, reason);
        }

}
