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

package com.dmdirc.ui.messages;

import com.dmdirc.config.IdentityManager;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class StyliserTest extends junit.framework.TestCase {
    
    @Before
    public void setUp() {
        IdentityManager.load();
    }
    
    @Test
    public void testStripControlCodes1() {
        String input = "This"+((char) 2)+" is "+((char) 17)+"a test";
        
        String expResult = "This is a test";
        String result = Styliser.stipControlCodes(input);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testStripControlCodes2() {
        String input = "This is "+((char) 3)+"5a "+((char) 4)+"FF0000test";
        
        String expResult = "This is a test";
        String result = Styliser.stipControlCodes(input);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testReadUntilControl1() {
        String input = "This"+((char) 2)+" is "+((char) 17)+"a test";
        String expResult = "This";
        String result = Styliser.readUntilControl(input);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testReadUntilControl2() {
        String input = "This"+((char) 17)+" is "+((char) 17)+"a test";
        String expResult = "This";
        String result = Styliser.readUntilControl(input);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testReadUntilControl3() {
        String input = ((char) 31)+" is "+((char) 17)+"a test";
        String expResult = "";
        String result = Styliser.readUntilControl(input);
        assertEquals(expResult, result);
    }
    
    @Test
    public void testLinking() {
        final char h = Styliser.CODE_HYPERLINK;
        
        final String[][] tests = {
            {"no links here!", "no links here!"},
            {"www.google.com", "~www.google.com~"},
            {"http://www.google.com", "~http://www.google.com~"},
            {"www.google.com www.google.com", "~www.google.com~ ~www.google.com~"},
            {"http://www.google.com:80/test#flub", "~http://www.google.com:80/test#flub~"},
            {"(www.google.com)", "(~www.google.com~)"},
            {"(foo: www.google.com)", "(foo: ~www.google.com~)"},
            {"(foo: 'www.google.com')", "(foo: '~www.google.com~')"},
            {"foo: www.google.com, bar", "foo: ~www.google.com~, bar"},
            {"\"foo\" www.google.com \"www.google.com\"",
                     "\"foo\" ~www.google.com~ \"~www.google.com~\"",
            },
        };
        
        for (String[] testcase : tests) {
            assertEquals(testcase[1], Styliser.doLinks(testcase[0]).replace(h, '~'));
        }
    }
    
}
