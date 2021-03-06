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

package com.dmdirc.commandparser.validators;

import com.dmdirc.commandparser.CommandManager;
import com.dmdirc.util.validators.RegexStringValidator;
import com.dmdirc.util.validators.ValidatorChain;

import java.util.regex.Pattern;

/**
 * Validates command names.
 *
 * @since 0.6.3m1rc3
 */
public class CommandNameValidator extends ValidatorChain<String> {

    /**
     * Instantiates a new command name validator.
     */
    @SuppressWarnings("unchecked")
    public CommandNameValidator() {
        super(new RegexStringValidator("^[^\\s]*$", "Cannot contain spaces"),
                new RegexStringValidator("^[^"
                + Pattern.quote(String.valueOf(CommandManager.getCommandManager().getCommandChar()))
                + "].*$", "Cannot start with a " + CommandManager.getCommandManager().getCommandChar()));
    }

}
