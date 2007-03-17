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

package uk.org.ownage.dmdirc.commandparser;

import uk.org.ownage.dmdirc.Query;
import uk.org.ownage.dmdirc.Server;

/**
 * A command parser that is tailored for use in a query environment. Handles
 * both query and server commands.
 * @author chris
 */
public final class QueryCommandParser extends CommandParser {
    
    /**
     * The server instance that this parser is attached to.
     */
    private final Server server;
    /**
     * The query instance that this parser is attached to.
     */
    private final Query query;
    
    /**
     * Creates a new instance of QueryCommandParser.
     * @param newServer The server instance that this parser is attached to
     * @param newQuery The query instance that this parser is attached to
     */
    public QueryCommandParser(final Server newServer, final Query newQuery) {
        super();
        
        server = newServer;
        query = newQuery;
    }
    
    /** Loads the relevant commands into the parser. */
    protected void loadCommands() {
        CommandManager.loadQueryCommands(this);
        CommandManager.loadServerCommands(this);
    }
    
    /**
     * Executes the specified command with the given arguments.
     * @param origin The window in which the command was typed
     * @param command The command to be executed
     * @param args The arguments to the command
     */
    protected void executeCommand(final CommandWindow origin, 
            final Command command, final String... args) {
        if (command instanceof QueryCommand) {
            ((QueryCommand) command).execute(origin, server, query, args);
        } else {
            ((ServerCommand) command).execute(origin, server, args);
        }
    }
    
    /**
     * Called when the user attempted to issue a command (i.e., used the command
     * character) that wasn't found. It could be that the command has a different
     * arity, or that it plain doesn't exist.
     * @param origin The window in which the command was typed
     * @param command The command the user tried to execute
     * @param args The arguments passed to the command
     */
    protected void handleInvalidCommand(final CommandWindow origin, 
            final String command, final String... args) {
        origin.addLine("Unknown command: " + command + "/" + args.length);
    }
    
    /**
     * Called when the input was a line of text that was not a command. This normally
     * means it is sent to the server/channel/user as-is, with no further processing.
     * @param origin The window in which the command was typed
     * @param line The line input by the user
     */
    protected void handleNonCommand(final CommandWindow origin, final String line) {
        query.sendLine(line);
    }
    
}
