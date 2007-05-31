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

import com.dmdirc.logger.ErrorLevel;
import com.dmdirc.logger.Logger;

import java.awt.Color;

/**
 * The colour manager manages the colour scheme for the IRC client. It allows
 * other components to use IRC colour codes instead of absolute colours.
 * @author chris
 */
public final class ColourManager {
    
    /** Creates a new instance of ColourManager. */
    private ColourManager() {
    }
    
    /**
     * Parses either a 1-2 digit IRC colour, or a 6 digit hex colour from the
     * target string, and returns the corresponding colour. Returns the
     * specified fallback colour if the spec can't be parsed.
     * @param spec The string to parse
     * @param fallback The colour to use if the spec isn't valid
     * @return A colour representation of the specified string
     */
    public static Color parseColour(final String spec, final Color fallback) {
        Color res = null;
        
        if (spec.length() < 3) {
            int num;
            
            try {
                num = Integer.parseInt(spec);
            } catch (NumberFormatException ex) {
                num = -1;
            }
            
            if (num >= 0 && num <= 15) {
                res = getColour(num);
            }
        } else if (spec.length() == 6) {
            res = getColour(spec);
        }
        
        if (res == null) {
            Logger.error(ErrorLevel.WARNING, "Invalid colour format: " + spec);
            res = fallback;
        }
        
        return res;
    }
    
    /**
     * Parses either a 1-2 digit IRC colour, or a 6 digit hex colour from the
     * target string, and returns the corresponding colour. Returns white if the
     * spec can't be parsed.
     * @param spec The string to parse
     * @return A colour representation of the specified string
     */
    public static Color parseColour(final String spec) {
        return parseColour(spec, Color.WHITE);
    }    
        
    /**
     * Returns a Color object that corresponds to the specified 6-digit hex
     * string. If the string is invalid, logs a warning and returns white.
     * @param hex The hex string to convert into a Color
     * @return A Color object corresponding to the hex input
     */
    public static Color getColour(final String hex) {
        try {
            return Color.decode("#" + hex);
        } catch (NumberFormatException ex) {
            Logger.error(ErrorLevel.WARNING, "Invalid colour #" + hex, ex);
            return Color.WHITE;
        }
    }
    
    /**
     * Returns a Color object that represents the colour associated with the
     * specified IRC colour code. If the code is not found, a warning is logged
     * with the client's Logger class, and white is returned.
     * @param number The IRC colour code to look up
     * @return The corresponding Color object
     */
    public static Color getColour(final int number) {
        switch (number) {
            case 0:
                return Color.WHITE;
            case 1:
                return Color.BLACK;
            case 2:
                return new Color(0, 0, 127);
            case 3:
                return new Color(0, 141, 0);
            case 4:
                return Color.RED;
            case 5:
                return new Color(127, 0, 0);
            case 6:
                return new Color(160, 15, 160);
            case 7:
                return new Color(252, 127, 0);
            case 8:
                return Color.YELLOW;
            case 9:
                return new Color(0, 252, 0);
            case 10:
                return new Color(0, 128, 128);
            case 11:
                return new Color(0, 255, 255);
            case 12:
                return Color.BLUE;
            case 13:
                return new Color(255, 0, 255);
            case 14:
                return Color.GRAY;
            case 15:
                return Color.LIGHT_GRAY;
            default:
                Logger.error(ErrorLevel.WARNING, "Invalid colour: " + number);
                return Color.WHITE;
        }
    }

    /**
     * Retrieves the hex representation of the specified colour.
     * @param colour The colour to be parsed
     * @return A 6-digit hex string representing the colour
     */
    public static String getHex(final Color colour) {
        final int r = colour.getRed();
        final int g = colour.getGreen();
        final int b = colour.getBlue();
        
        return toHex(r) + toHex(g) + toHex(b);
    }
    
    /**
     * Converts the specified integer (in the range 0-255) into a hex string.
     * @param value The integer to convert
     * @return A char digit hex string representing the specified integer
     */
    private static String toHex(final int value) {
        final char[] chars = {
            '0', '1', '2', '3', '4', '5', '6', '7', 
            '8', '9', 'A', 'B', 'C', 'D', 'E', 'F',
        };
        
        return ("" + chars[value / 16]) + chars[value % 16];
    }    
    
}
