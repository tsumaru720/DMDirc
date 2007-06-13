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
 */

package com.dmdirc.commandparser;

import com.dmdirc.logger.ErrorLevel;
import com.dmdirc.logger.Logger;
import com.dmdirc.ui.interfaces.InputWindow;

/**
 * The command parser used for global commands.
 * @author chris
 */
public final class GlobalCommandParser extends CommandParser {
    
    /**
     * The singleton instance of this command parser.
     */
    private static GlobalCommandParser me;
    
    /**
     * Creates a new instance of the GlobalCommandParser.
     */
    private GlobalCommandParser() {
        super();
    }
    
    /**
     * Retrieves a singleton instance of the global command parser.
     * @return A GlobalCommandParser
     */
    public static synchronized GlobalCommandParser getGlobalCommandParser() {
        if (me == null) {
            me = new GlobalCommandParser();
        }
        
        return me;
    }
    
    /** Loads the relevant commands into the parser. */
    protected void loadCommands() {
        CommandManager.loadGlobalCommands(this);
    }
    
    /** {@inheritDoc} */
    protected void executeCommand(final InputWindow origin,
            final boolean isSilent, final Command command, final String... args) {
        ((GlobalCommand) command).execute(origin, isSilent, args);
    }
    
    /**
     * Called when the input was a line of text that was not a command. This normally
     * means it is sent to the server/channel/user as-is, with no further processing.
     * @param origin The window in which the command was typed
     * @param line The line input by the user
     */
    protected void handleNonCommand(final InputWindow origin, final String line) {
        if (origin == null) {
            Logger.error(ErrorLevel.WARNING, "Invalid global command: " + line);
        } else {
            origin.addLine("commandError", "Invalid global command: " + line);
        }
    }
    
}
