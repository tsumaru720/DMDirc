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

package com.dmdirc.ui.textpane;

import java.text.AttributedString;
import java.util.ArrayList;
import java.util.List;

/**
 * Data contained in a TextPane.
 */
public final class IRCDocument {
    
    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 2;
    
    /** List of stylised lines of text. */
    private List<AttributedString> iterators;
    
    /** Creates a new instance of IRCDocument. */
    public IRCDocument() {
        iterators = new ArrayList<AttributedString>();
    }
    
    /**
     * Returns the number of lines in this document.
     *
     * @return Number of lines
     */
    public int getNumLines() {
        return iterators.size();
    }
    
    /**
     * Returns the Line at the specified number.
     *
     * @param lineNumber Line number to retrieve
     *
     * @return Line at the specified number
     */
    public AttributedString getLine(final int lineNumber) {
        return iterators.get(lineNumber);
    }
    
    /**
     * Adds the stylised string to the canvas.
     * @param text stylised string to add to the text
     */
    public void addText(final AttributedString text) {
        synchronized (iterators) {
            iterators.add(text);
        }
    }
}

