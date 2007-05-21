/*
 * Copyright (c) 2006-2007 Chris Smith, Shane Mc Cormack, Gregory Holmes
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
 *
 * SVN: $Id$
 */

package com.dmdirc.parser;

import com.dmdirc.parser.callbacks.CallbackOnNickInUse;
import com.dmdirc.parser.callbacks.interfaces.INickInUse;

/**
 * Process a NickInUse message.
 * Parser implements handling of this if Pre-001 and no other handler found,
 * adding the NickInUse handler (addNickInUse) after 001 is prefered over before.<br><br>
 * <br>
 * If the first nickname is in use, and a NickInUse message is recieved before 001, we
 * will attempt to use the altnickname instead.<br>
 * If this also fails, we will start prepending _ (or the value of me.cPrepend) to the main nickname.
 */
public class ProcessNickInUse extends IRCProcessor {
	/**
	 * Process a NickInUse message.
	 * Parser implements handling of this if Pre-001 and no other handler found,
	 * adding the NickInUse handler (addNickInUse) after 001 is prefered over before.<br><br>
	 * <br>
	 * If the first nickname is in use, and a NickInUse message is recieved before 001, we
	 * will attempt to use the altnickname instead.<br>
	 * If this also fails, we will start prepending _ (or the value of me.cPrepend) to the main nickname.
	 *
	 * @param sParam Type of line to process ("433")
	 * @param token IRCTokenised line to process
	 */
	public void process(String sParam, String[] token) {
		if (!callNickInUse(token[3])) {
			// Manually handle nick in use.
			callDebugInfo(myParser.DEBUG_INFO,"No Nick in use Handler.");
			if (!myParser.got001) {
				callDebugInfo(myParser.DEBUG_INFO,"Using inbuilt handler");
				// If this is before 001 we will try and get a nickname, else we will leave the nick as-is
				if (!myParser.triedAlt) { myParser.setNickname(myParser.me.getAltNickname()); myParser.triedAlt = true; }
				else {
					if (myParser.sThinkNickname.equalsIgnoreCase(myParser.me.getAltNickname())) { myParser.sThinkNickname = myParser.me.getNickname(); }
					myParser.setNickname(myParser.me.getPrependChar()+myParser.sThinkNickname);
				}
			}
		}
	}
	
	/**
	 * Callback to all objects implementing the NickInUse Callback.
	 *
	 * @param nickname Nickname that was wanted.
	 * @see INickInUse
	 */
	protected boolean callNickInUse(final String nickname) {
		CallbackOnNickInUse cb = (CallbackOnNickInUse)getCallbackManager().getCallbackType("OnNickInUse");
		if (cb != null) { return cb.call(nickname); }
		return false;
	}
	
	/**
	 * What does this IRCProcessor handle.
	 *
	 * @return String[] with the names of the tokens we handle.
	 */
	public String[] handles() {
		String[] iHandle = new String[1];
		iHandle[0] = "433";
		return iHandle;
	} 
	
	/**
	 * Create a new instance of the IRCProcessor Object
	 *
	 * @param parser IRCParser That owns this IRCProcessor
	 * @param manager ProcessingManager that is in charge of this IRCProcessor
	 */
	protected ProcessNickInUse (IRCParser parser, ProcessingManager manager) { super(parser, manager); }
	
	/**
	 * Get SVN Version information.
	 *
	 * @return SVN Version String
	 */
	public static String getSvnInfo () { return "$Id$"; }	
}
