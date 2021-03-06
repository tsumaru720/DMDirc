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
package com.dmdirc.commandparser.commands.server;

import com.dmdirc.FrameContainer;
import com.dmdirc.Server;
import com.dmdirc.commandparser.CommandArguments;
import com.dmdirc.commandparser.commands.context.ServerCommandContext;
import com.dmdirc.config.Identity;
import com.dmdirc.config.IdentityManager;

import java.net.URI;
import java.net.URISyntaxException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.*;

public class ChangeServerTest {

    private final ChangeServer command = new ChangeServer();
    private FrameContainer tiw;
    private Identity profile;
    private Server server;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        IdentityManager.load();
    }

    @Before
    public void setUp() {
        tiw = mock(FrameContainer.class);
        profile = mock(Identity.class);
        server = mock(Server.class);
        when(server.getProfile()).thenReturn(profile);
    }

    @Test
    public void testUsageNoArgs() {
        command.execute(tiw, new CommandArguments("/server"),
                new ServerCommandContext(null, command, server));
        
        verify(tiw).addLine(eq("commandUsage"), anyChar(), anyString(), anyString());
    }
    
    @Test
    public void testInvalidPort() {
        command.execute(tiw, new CommandArguments("/server foo:abc"),
                new ServerCommandContext(null, command, server));
        
        verify(tiw).addLine(eq("commandError"), anyString());
    }
    
    @Test
    public void testOutOfRangePort1() {
        command.execute(tiw, new CommandArguments("/server foo:0"),
                new ServerCommandContext(null, command, server));
        
        verify(tiw).addLine(eq("commandError"), anyString());
    }
    
    @Test
    public void testOutOfRangePort2() {
        command.execute(tiw, new CommandArguments("/server foo:65537"),
                new ServerCommandContext(null, command, server));
        
        verify(tiw).addLine(eq("commandError"), anyString());
    }

    @Test
    public void testExecuteBasic() throws URISyntaxException {
        command.execute(tiw, new CommandArguments("/server foo:1234"),
                new ServerCommandContext(null, command, server));

        verify(server).connect(eq(new URI("irc://foo:1234")), same(profile));
    }

    @Test
    public void testExecuteNoPort() throws URISyntaxException {
        command.execute(tiw, new CommandArguments("/server foo"),
                new ServerCommandContext(null, command, server));

        verify(server).connect(eq(new URI("irc://foo:6667")), same(profile));
    }

    @Test
    public void testDeprecatedSSL() throws URISyntaxException {
        command.execute(tiw, new CommandArguments("/server --ssl foo"),
                new ServerCommandContext(null, command, server));

        verify(server).connect(eq(new URI("ircs://foo:6667")), same(profile));
    }

    @Test
    public void testExecuteComplex() throws URISyntaxException {
        command.execute(tiw, new CommandArguments("/server foo:+1234 password"),
                new ServerCommandContext(null, command, server));

        verify(server).connect(eq(new URI("ircs://password@foo:1234")), same(profile));
    }

}