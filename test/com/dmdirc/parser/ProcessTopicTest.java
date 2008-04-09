/*
 * Copyright (c) 2006-2008 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

package com.dmdirc.parser;

import com.dmdirc.parser.callbacks.CallbackNotFoundException;
import com.dmdirc.parser.callbacks.interfaces.IChannelTopic;
import org.junit.Test;
import static org.junit.Assert.*;

public class ProcessTopicTest extends junit.framework.TestCase {
    
    @Test
    public void testBasicTopic() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();
        final ICTTest test = new ICTTest();
        parser.injectConnectionStrings();
        parser.getCallbackManager().addCallback("OnChannelTopic", test);
        
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 332 nick #DMDirc_testing :This be a topic");
        parser.injectLine(":server 333 nick #DMDirc_testing Q 1207350306");
        
        assertTrue(test.triggered);
        assertTrue(test.isJoin);
        assertEquals("#DMDirc_testing", test.channel.getName());
        assertEquals("This be a topic", test.channel.getTopic());
        assertEquals("Q", test.channel.getTopicUser());
        assertEquals(1207350306l, test.channel.getTopicTime());
    }
    
    @Test
    public void testTopicChange() throws CallbackNotFoundException {
        final TestParser parser = new TestParser();
        final ICTTest test = new ICTTest();
        parser.injectConnectionStrings();
        
        parser.injectLine(":nick JOIN #DMDirc_testing");
        parser.injectLine(":server 332 nick #DMDirc_testing :This be a topic");
        parser.injectLine(":server 333 nick #DMDirc_testing Q 1207350306");
        
        parser.getCallbackManager().addCallback("OnChannelTopic", test);
        
        parser.injectLine(":foobar TOPIC #DMDirc_testing :New topic here");
        
        assertTrue(test.triggered);
        assertFalse(test.isJoin);
        assertEquals("#DMDirc_testing", test.channel.getName());
        assertEquals("New topic here", test.channel.getTopic());
        assertEquals("foobar", test.channel.getTopicUser());
        assertTrue(1207350306l < test.channel.getTopicTime());
    }    
    
    private class ICTTest implements IChannelTopic {
        
        public boolean triggered;
        public boolean isJoin;
        public ChannelInfo channel;

        public void onChannelTopic(IRCParser tParser, ChannelInfo cChannel,
                                   boolean bIsJoinTopic) {
            triggered = true;
            isJoin = bIsJoinTopic;
            channel = cChannel;
        }
        
        
    }

}
