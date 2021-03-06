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

package com.dmdirc.harness;

import com.dmdirc.Server;
import com.dmdirc.WritableFrameContainer;
import com.dmdirc.commandparser.parsers.GlobalCommandParser;
import com.dmdirc.config.ConfigManager;
import com.dmdirc.config.IdentityManager;
import com.dmdirc.ui.input.TabCompleter;
import com.dmdirc.ui.interfaces.InputWindow;
import java.util.Collections;

public class TestWritableFrameContainer extends WritableFrameContainer {

    private final int lineLength;

    public TestWritableFrameContainer(final int lineLength,
            final ConfigManager cm, final Class<? extends InputWindow> clazz) {
        super("raw", "Raw", "(Raw)", clazz, cm,
                GlobalCommandParser.getGlobalCommandParser(),
                Collections.<String>emptySet());

        this.lineLength = lineLength;
    }

    public TestWritableFrameContainer(final int lineLength, final Class<? extends InputWindow> clazz) {
        this(lineLength, IdentityManager.getGlobalConfig(), clazz);
    }

    @Override
    public void sendLine(final String line) {
        // Do nothing
    }

    @Override
    public int getMaxLineLength() {
        return lineLength;
    }

    @Override
    public void windowClosing() {
        System.out.println("windowClosing");
    }

    @Override
    public void windowClosed() {
        // DO nothing
    }

    @Override
    public Server getServer() {
        return null;
    }

    @Override
    public TabCompleter getTabCompleter() {
        return new TabCompleter();
    }
}
