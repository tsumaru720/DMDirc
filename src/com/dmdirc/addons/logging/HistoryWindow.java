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

package com.dmdirc.addons.logging;

import com.dmdirc.FrameContainer;
import com.dmdirc.Main;
import com.dmdirc.Server;
import com.dmdirc.config.IdentityManager;
import com.dmdirc.ui.WindowManager;
import com.dmdirc.ui.interfaces.Window;

import java.util.Stack;

/**
 * Displays an extended history of a window.
 *
 * @author Chris
 */
public class HistoryWindow extends FrameContainer {
    
    /** The title of our window. */
    private final String title;
       
    /** The window we're using. */
    private Window window;
    
    /** Our parent window. */
    private Window parent;
    
    /**
     * Creates a new HistoryWindow.
     *
     * @param title The title of the window
     * @param reader The reader to use to get the history
     * @param parent The window this history window was opened from
     */
    public HistoryWindow(final String title, final ReverseFileReader reader, final Window parent) {
        super("raw", parent.getConfigManager());
        System.out.println("New history window");
        this.title = title;
        this.parent = parent;
        
        window = Main.getUI().getWindow(this);
        
        WindowManager.addWindow(parent, window);
        System.out.println("New history window 2");
        window.setTitle(title);
        window.setVisible(true);
        System.out.println("New history window 3");
        final int historyLineCount = IdentityManager.getGlobalConfig().getOptionInt(
                "plugin-Logging", "history.lines", 50000);
        final int frameBufferSize = IdentityManager.getGlobalConfig().getOptionInt(
                "ui", "frameBufferSize", 10000);
//        window.addLine(reader.getLinesAsString(Math.min(frameBufferSize, historyLineCount)), false);
        System.out.println("New history window 4");
        final long starttime = System.currentTimeMillis();
        System.out.println(reader.getLinesAsString(Math.min(frameBufferSize, historyLineCount)));
        final long endtime = System.currentTimeMillis();
        System.out.println("Sysout Time taken: "+(endtime-starttime)+"ms");
        
        final long starttime2 = System.currentTimeMillis();
        window.addLine(reader.getLinesAsString(Math.min(frameBufferSize, historyLineCount)), false);
        final long endtime2 = System.currentTimeMillis();
        System.out.println("UI Time taken: "+(endtime2-starttime2)+"ms");
    }
    
    /** {@inheritDoc} */
    @Override
    public Window getFrame() {
        return window;
    }
    
    /** {@inheritDoc} */
    @Override
    public String toString() {
        return title;
    }
    
    /** {@inheritDoc} */
    @Override
    public void windowClosing() {
        // 1: Make the window non-visible
        window.setVisible(false);
        
        // 2: Remove any callbacks or listeners
        // 3: Trigger any actions neccessary
        // 4: Trigger action for the window closing
        // 5: Inform any parents that the window is closing
        
        // 6: Remove the window from the window manager
        WindowManager.removeWindow(window);
        
        // 7: Remove any references to the window and parents
        window = null; // NOPMD
        parent = null; // NOPMD
    }
    
    /** {@inheritDoc} */
    @Override
    public Server getServer() {
        return parent.getContainer().getServer();
    }

}
