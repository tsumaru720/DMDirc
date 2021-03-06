/*
 * Copyright (c) 2006-2011 Chris Smith, Shane Mc Cormack, Gregory Holmes
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
package com.dmdirc.commandparser.commands.global;

import com.dmdirc.FrameContainer;
import com.dmdirc.commandparser.CommandArguments;
import com.dmdirc.commandparser.commands.context.CommandContext;
import com.dmdirc.config.IdentityManager;

import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class NewServerTest {

    @BeforeClass
    public static void setUpClass() throws Exception {
        IdentityManager.load();
    }

    private final NewServer command = new NewServer();

    @Test
    public void testUsageNoArgs() {
        final FrameContainer tiw = mock(FrameContainer.class);
        command.execute(tiw, new CommandArguments("/foo"),
                new CommandContext(null, command));
        
        verify(tiw).addLine(eq("commandUsage"), anyChar(), anyString(), anyString());
    }
    
    @Test
    public void testInvalidPort() {
        final FrameContainer tiw = mock(FrameContainer.class);
        command.execute(tiw, new CommandArguments("/foo foo:abc"),
                new CommandContext(null, command));
        
        verify(tiw).addLine(eq("commandError"), anyString());
    }
    
    @Test
    public void testOutOfRangePort1() {
        final FrameContainer tiw = mock(FrameContainer.class);
        command.execute(tiw, new CommandArguments("/foo foo:0"),
                new CommandContext(null, command));
        
        verify(tiw).addLine(eq("commandError"), anyString());
    }
    
    @Test
    public void testOutOfRangePort2() {
        final FrameContainer tiw = mock(FrameContainer.class);
        command.execute(tiw, new CommandArguments("/foo foo:65537"),
                new CommandContext(null, command));
        
        verify(tiw).addLine(eq("commandError"), anyString());
    }

}