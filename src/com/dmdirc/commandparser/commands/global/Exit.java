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
import com.dmdirc.Main;
import com.dmdirc.commandparser.CommandArguments;
import com.dmdirc.commandparser.CommandInfo;
import com.dmdirc.commandparser.CommandType;
import com.dmdirc.commandparser.commands.Command;
import com.dmdirc.commandparser.commands.context.CommandContext;

/**
 * The exit command allows the user to quit DMDirc with a custom quit message.
 * When the client quits, it disconnects all servers (with the quit message
 * supplied) and saves the config file.
 */
public class Exit extends Command implements CommandInfo {

    /** {@inheritDoc} */
    @Override
    public void execute(final FrameContainer origin,
            final CommandArguments args, final CommandContext context) {
        Main.quit(args.getArguments().length > 0 ? args.getArgumentsAsString()
                : origin.getConfigManager().getOption("general", "closemessage"));
    }

    /** {@inheritDoc}. */
    @Override
    public String getName() {
        return "exit";
    }

    /** {@inheritDoc}. */
    @Override
    public boolean showInHelp() {
        return true;
    }

    /** {@inheritDoc} */
    @Override
    public CommandType getType() {
        return CommandType.TYPE_GLOBAL;
    }

    /** {@inheritDoc}. */
    @Override
    public String getHelp() {
        return "exit [reason] - exits the client";
    }

}
