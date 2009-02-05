/*
 * Copyright (c) 2006-2009 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

package com.dmdirc.addons.time;

import com.dmdirc.actions.interfaces.ActionMetaType;

import java.util.Calendar;

/**
 * Encapsulates the meta types used by the various time actions.
 * @author chris
 */
public enum TimeActionMetaType implements ActionMetaType {
    
    /** Time type. */
    TIME_TIME {
        /** {@inheritDoc} */
        public int getArity() { return 1; }
        /** {@inheritDoc} */
        public Class[] getArgTypes() { return new Class[]{Calendar.class}; }
        /** {@inheritDoc} */
        public String[] getArgNames() { return new String[]{"Date"}; }
    };
    
    /** {@inheritDoc} */
    public abstract int getArity();
    
    /** {@inheritDoc} */
    public abstract Class[] getArgTypes();
    
    /** {@inheritDoc} */
    public abstract String[] getArgNames();
    
    /** {@inheritDoc} */
    public String getGroup() {
        return "Time Events";
    }    
    
}