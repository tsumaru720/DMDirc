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

import com.dmdirc.parser.interfaces.ClientInfo;
import com.dmdirc.parser.interfaces.LocalClientInfo;
import com.dmdirc.parser.interfaces.Parser;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedList;
import java.util.Hashtable;
import java.util.HashMap;
import java.util.Map;

/**
 * Contains information about known users.
 * 
 * @author Shane Mc Cormack
 * @author Chris Smith
 * @see IRCParser
 */
public class IRCClientInfo implements LocalClientInfo {
	/** Known nickname of client. */
	private String sNickname = "";
	/** Known ident of client. */
	private String sIdent = "";	
	/** Known host of client. */
	private String sHost = "";
	/** Known user modes of client. */
	private long nModes;
	/** Known Away Reason of client. */
	private String myAwayReason = "";
	/** Known RealName of client. */
	private String sRealName = "";
	/** Known away state for client. */
	private boolean bIsAway;
	/** Is this a fake client created just for a callback? */
	private boolean bIsFake;
	/** Reference to the parser object that owns this channel, Used for modes. */
	private final IRCParser myParser;
	/** A Map to allow applications to attach misc data to this object */
	private Map<Object, Object> myMap;
	/** List of ChannelClientInfos that point to this */
	private final Map<String, IRCChannelClientInfo> myChannelClientInfos = new Hashtable<String, IRCChannelClientInfo>();
	/** Modes waiting to be sent to the server. */
	private final List<String> lModeQueue = new LinkedList<String>();

	/**
	 * Create a new client object from a hostmask.
	 *
 	 * @param tParser Refernce to parser that owns this channelclient (used for modes)	 
	 * @param sHostmask Hostmask parsed by parseHost to get nickname
	 * @see ClientInfo#parseHost
	 */
	public IRCClientInfo(final IRCParser tParser, final String sHostmask) {
		myMap = new HashMap<Object, Object>();
		setUserBits(sHostmask, true);
		myParser = tParser;
	}

	/**
	 * Set the Map object attatched to this object.
	 *
	 * @param newMap New Map to attatch.
	 */
	public void setMap(final Map<Object, Object> newMap) {
		myMap = newMap;
	}
	
	/** {@inheritDoc} */
        @Override
	public Map<Object, Object> getMap() {
            return myMap;
	}

	/**
	 * Check if this is a fake client.
	 *
	 * @return True if this is a fake client, else false
	 */
	public boolean isFake() { return bIsFake; }
	/**
	 * Check if this client is actually a server.
	 *
	 * @return True if this client is actually a server.
	 */
	public boolean isServer() { return !(sNickname.indexOf(':') == -1); }
	/**
	 * Set if this is a fake client.
	 * This returns "this" and thus can be used in the construction line.
	 *
	 * @param newValue new value for isFake - True if this is a fake client, else false
	 * @return this Object
	 */
	public IRCClientInfo setFake(final boolean newValue) { bIsFake = newValue; return this; }

	/**
	 * Get a nickname of a user from a hostmask.
	 * Hostmask must match (?:)nick(?!ident)(?@host)
	 *
	 * @param sWho Hostname to parse
	 * @return nickname of user
	 */
	public static String parseHost(final String sWho) {
		// Get the nickname from the string.
		return parseHostFull(sWho)[0];
	}
	
	/**
	 * Get a nick ident and host of a user from a hostmask.
	 * Hostmask must match (?:)nick(?!ident)(?@host)
	 *
	 * @param sWho Hostname to parse
	 * @return Array containing details. (result[0] -> Nick | result[1] -> Ident | result[2] -> Host)
	 */
	public static String[] parseHostFull(String sWho) {
		String[] sTemp = null;
		final String[] result = new String[3];
		if (!sWho.isEmpty() && sWho.charAt(0) == ':') { sWho = sWho.substring(1); }
		sTemp = sWho.split("@", 2);
		if (sTemp.length == 1) { result[2] = ""; } else { result[2] = sTemp[1]; }
		sTemp = sTemp[0].split("!", 2);
		if (sTemp.length == 1) { result[1] = ""; } else { result[1] = sTemp[1]; }
		result[0] = sTemp[0];
		
		return result;
	}

	/**
	 * Set the nick/ident/host of this client.
	 *
	 * @param sHostmask takes a host (?:)nick(?!ident)(?@host) and sets nick/host/ident variables
	 * @param bUpdateNick if this is false, only host/ident will be updated.
	 */	
	public void setUserBits(final String sHostmask, final boolean bUpdateNick) {
		setUserBits(sHostmask, bUpdateNick, false);
	}
	
	/**
	 * Set the nick/ident/host of this client.
	 *
	 * @param sHostmask takes a host (?:)nick(?!ident)(?@host) and sets nick/host/ident variables
	 * @param bUpdateNick if this is false, only host/ident will be updated.
	 * @param allowBlank if this is true, ident/host will be set even if
	 *                   parseHostFull returns empty values for them
	 */	
	public void setUserBits(final String sHostmask, final boolean bUpdateNick, final boolean allowBlank) {
		final String[] sTemp = parseHostFull(sHostmask);
		if (!sTemp[2].isEmpty() || allowBlank) { sHost = sTemp[2]; }
		if (!sTemp[1].isEmpty() || allowBlank) { sIdent = sTemp[1]; }
		if (bUpdateNick) { sNickname = sTemp[0]; }
	}
	
	/**
	 * Get a string representation of the user.
	 *
	 * @return String representation of the user.
	 */
	@Override
	public String toString() { return sNickname + "!" + sIdent + "@" + sHost; }
	
	/** {@inheritDoc} */
        @Override
	public String getNickname() { return sNickname; }
	
	/** {@inheritDoc} */
        @Override
        public String getUsername() { return sIdent; }
	
	/** {@inheritDoc} */
        @Override
	public String getHostname() { return sHost; }
	
	/**
	 * Set the away state of a user.
	 * Automatically sets away reason to "" if set to false
	 *
	 * @param bNewState Boolean representing state. true = away, false = here
	 */	
	protected void setAwayState(final boolean bNewState) {
		bIsAway = bNewState;
		if (!bIsAway) { myAwayReason = ""; }
	}
	
	/**
	 * Get the away state of a user.
	 *
	 * @return Boolean representing state. true = away, false = here
	 */	
	public boolean getAwayState() { return bIsAway; }
	
	/**
	 * Get the Away Reason for this user.
	 *
	 * @return Known away reason for user.
	 */
	public String getAwayReason() { return myAwayReason; }
	
	/**
	 * Set the Away Reason for this user.
	 * Automatically set to "" if awaystate is set to false
	 *
	 * @param newValue new away reason for user.
	 */
	protected void setAwayReason(final String newValue) { myAwayReason = newValue; }
	
	/** {@inheritDoc} */
        @Override
	public String getRealname() { return sRealName; }
	
	/**
	 * Set the RealName for this user.
	 *
	 * @param newValue new RealName for user.
	 */
	protected void setRealName(final String newValue) { sRealName = newValue; }
	
	/**
	 * Set the user modes (as an integer).
	 *
	 * @param nNewMode new long representing channel modes. (Boolean only)
	 */	
	protected void setUserMode(final long nNewMode) { nModes = nNewMode; }
	
	/**
	 * Get the user modes (as an integer).
	 *
	 * @return long representing channel modes. (Boolean only)
	 */	
	public long getUserMode() { return nModes; }	
	
	/** {@inheritDoc} */
        @Override
	public String getModes() {
		final StringBuilder sModes = new StringBuilder("+");
		long nTemp = 0;
		final long nChanModes = this.getUserMode();
		
		for (char cTemp : myParser.userModes.keySet()) {
			nTemp = myParser.userModes.get(cTemp);
			if ((nChanModes & nTemp) == nTemp) { sModes.append(cTemp); }
		}
		
		return sModes.toString();
	}
	
	/**
	 * Is this client an oper?
	 * This is a guess currently based on user-modes and thus only works on the
	 * parsers own client.
	 *
	 * @return True/False if this client appears to be an oper
	 */
	public boolean isOper() {
		final String modestr = getModes();
		return (modestr.indexOf('o') > -1) || (modestr.indexOf('O') > -1);
	}
	
	/**
	 * Add a ChannelClientInfo as a known reference to this client.
	 *
	 * @param cci ChannelClientInfo to add as a known reference
	 */	
	public void addChannelClientInfo(final IRCChannelClientInfo cci) {
		final String key = myParser.getStringConverter().toLowerCase(cci.getChannel().getName());
		if (!myChannelClientInfos.containsKey(key)) {
			myChannelClientInfos.put(key, cci);
		}
	}
	
	/**
	 * Remove a ChannelClientInfo as a known reference to this client.
	 *
	 * @param cci ChannelClientInfo to remove as a known reference
	 */	
	public void delChannelClientInfo(final IRCChannelClientInfo cci) {
		final String key = myParser.getStringConverter().toLowerCase(cci.getChannel().getName());
		if (myChannelClientInfos.containsKey(key)) {
			myChannelClientInfos.remove(key);
		}
	}
	
	/**
	 * Check to see if a client is still known on any of the channels we are on.
	 *
	 * @return Boolean to see if client is still visable.
	 */
	public boolean checkVisibility() {
		return !myChannelClientInfos.isEmpty();
	}
	
	/** {@inheritDoc} */
        @Override
	public int getChannelCount() {
		return myChannelClientInfos.size();
	}
	
	/**
	 * Get a list of channelClients that point to this object.
	 *
	 * @return int with the count of known channels
	 */	
	public List<IRCChannelClientInfo> getChannelClients() {
		final List<IRCChannelClientInfo> result = new ArrayList<IRCChannelClientInfo>();
		for (IRCChannelClientInfo cci : myChannelClientInfos.values()) {
			result.add(cci);
		}
		return result;
	}
	
	/** {@inheritDoc} */
        @Override
	public void alterMode(final boolean add, final Character mode) {
		if (isFake()) { return; }
		int modecount = 1;
		String modestr = "";
		if (myParser.h005Info.containsKey("MODES")) {
			try { 
				modecount = Integer.parseInt(myParser.h005Info.get("MODES")); 
			} catch (NumberFormatException e) { 
				modecount = 1;
			}
		}
		modestr = ((add) ? "+" : "-") + mode;
		if (!myParser.userModes.containsKey(mode)) { return; }
		final String teststr = ((add) ? "-" : "+") + mode;
		if (lModeQueue.contains(teststr)) {
			lModeQueue.remove(teststr);
			return;
		} else if (lModeQueue.contains(modestr)) {
			return;
		}
		myParser.callDebugInfo(IRCParser.DEBUG_INFO, "Queueing user mode: %s", modestr);
		lModeQueue.add(modestr);
		if (lModeQueue.size() == modecount) { flushModes(); }
	}
	
	/** {@inheritDoc} */
        @Override
	public void flushModes() {
		if (lModeQueue.isEmpty()) { return; }
		final StringBuilder positivemode = new StringBuilder();
		final StringBuilder negativemode = new StringBuilder();
		final StringBuilder sendModeStr = new StringBuilder();
		String modestr;
		boolean positive;
		for (int i = 0; i < lModeQueue.size(); ++i) {
			modestr = lModeQueue.get(i);
			positive = modestr.charAt(0) == '+';
			if (positive) {
				positivemode.append(modestr.charAt(1));
			} else {
				negativemode.append(modestr.charAt(1));
			}
		}
		if (negativemode.length() > 0) { sendModeStr.append("-").append(negativemode); }
		if (positivemode.length() > 0) { sendModeStr.append("+").append(positivemode); }
		myParser.callDebugInfo(IRCParser.DEBUG_INFO, "Sending mode: %s", sendModeStr.toString());
		myParser.sendRawMessage("MODE " + sNickname + " " + sendModeStr.toString());
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
	public Parser getParser() { return myParser; }

        /** {@inheritDoc} */
        @Override
        public void setNickname(final String name) {
            if (myParser.getLocalClient().equals(this)) {
                myParser.setNickname(name);
                sNickname = name;
            } else {
                throw new UnsupportedOperationException("Cannot call setNickname on non-local client");
            }
        }

}