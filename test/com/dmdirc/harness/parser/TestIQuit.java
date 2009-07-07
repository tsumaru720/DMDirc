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

package com.dmdirc.harness.parser;

import com.dmdirc.parser.irc.*;
import com.dmdirc.parser.interfaces.callbacks.ChannelQuitListener;
import com.dmdirc.parser.interfaces.callbacks.QuitListener;

public class TestIQuit implements ChannelQuitListener, QuitListener {

    public IRCChannelInfo channel;

    public IRCChannelClientInfo cclient;

    public IRCClientInfo client;

    public String reason;

    public int count = 0;

    public void onChannelQuit(IRCParser tParser, IRCChannelInfo cChannel,
                              IRCChannelClientInfo cChannelClient, String sReason) {
        this.channel = cChannel;
        this.cclient = cChannelClient;
        this.reason = sReason;
        this.count++;
    }

    public void onQuit(IRCParser tParser, IRCClientInfo cClient, String sReason) {
        this.client = cClient;
        this.reason = sReason;
        this.count++;
    }
}
