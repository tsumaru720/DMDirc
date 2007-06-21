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

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.font.LineBreakMeasurer;
import java.awt.font.TextLayout;
import java.text.AttributedCharacterIterator;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.UIManager;
import javax.swing.event.MouseInputListener;

/** Canvas object to draw text. */
class TextPaneCanvas extends JPanel implements MouseInputListener {
    
    /**
     * A version number for this class. It should be changed whenever the
     * class structure is changed (or anything else that would prevent
     * serialized objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 5;
    
    /** IRCDocument. */
    private final transient IRCDocument document;
    /** parent textpane. */
    private final TextPane textPane;
    /** Position -> TextLayout. */
    private final Map<Rectangle, TextLayout> positions;
    /** TextLayout -> Line numbers. */
    private final Map<TextLayout, LineInfo> textLayouts;
    
    /** Selection event types. */
    private enum MouseEventType { CLICK, DRAG, RELEASE, };
    
    /** position of the scrollbar. */
    private int scrollBarPosition;
    /** Start line of the selection. */
    private int selStartLine;
    /** Start character of the selection. */
    private int selStartChar;
    /** End line of the selection. */
    private int selEndLine;
    /** End character of the selection. */
    private int selEndChar;
    
    /** First visible line. */
    private int firstVisibleLine;
    /** Last visible line. */
    private int lastVisibleLine;
    
    /**
     * Creates a new text pane canvas.
     *
     * @param parent parent text pane for the canvas
     * @param document IRCDocument to be displayed
     */
    public TextPaneCanvas(final TextPane parent, final IRCDocument document) {
        super();
        this.document = document;
        scrollBarPosition = 0;
        textPane = parent;
        this.setDoubleBuffered(true);
        this.setOpaque(true);
        textLayouts = new HashMap<TextLayout, LineInfo>();
        positions = new HashMap<Rectangle, TextLayout>();
        this.addMouseListener(this);
        this.addMouseMotionListener(this);
    }
    
    /**
     * Paints the text onto the canvas.
     * @param g graphics object to draw onto
     */
    public void paintComponent(final Graphics graphics) {
        final Graphics2D g = (Graphics2D) graphics;
        
        final float formatWidth = getWidth() - 6;
        final float formatHeight = getHeight();
        final int lineHeight = getFont().getSize() + 2;
        
        int paragraphStart;
        int paragraphEnd;
        LineBreakMeasurer lineMeasurer;
        
        g.setColor(textPane.getBackground());
        g.fill(g.getClipBounds());
        
        textLayouts.clear();
        positions.clear();
        
        //check theres something to draw
        if (document.getNumLines() == 0) {
            return;
        }
        
        float drawPosY = formatHeight;
        
        int startLine = scrollBarPosition;
        
        // Check the start line is in range
        if (startLine >= document.getNumLines()) {
            startLine = document.getNumLines() - 1;
        }
        
        if (startLine <= 0) {
            startLine = 0;
        }
        
        //sets the last visible line
        lastVisibleLine = startLine;
        firstVisibleLine = startLine;
        
        // We use these for drawing rather than the actual
        // sel{Start,End}{Line,Char} vars defined in the highlightEvent
        // This alllows for highlight in both directions.
        int useStartLine;
        int useStartChar;
        int useEndLine;
        int useEndChar;
        
        if (selStartLine > selEndLine) {
            // Swap both
            useStartLine = selEndLine;
            useStartChar = selEndChar;
            useEndLine = selStartLine;
            useEndChar = selStartChar;
        } else if (selStartLine == selEndLine && selStartChar > selEndChar) {
            // Just swap the chars
            useStartLine = selStartLine;
            useStartChar = selEndChar;
            useEndLine = selEndLine;
            useEndChar = selStartChar;
        } else {
            // Swap nothing
            useStartLine = selStartLine;
            useStartChar = selStartChar;
            useEndLine = selEndLine;
            useEndChar = selEndChar;
        }
        
        // Iterate through the lines
        for (int i = startLine; i >= 0; i--) {
            final AttributedCharacterIterator iterator = document.getLine(i).getIterator();
            paragraphStart = iterator.getBeginIndex();
            paragraphEnd = iterator.getEndIndex();
            lineMeasurer = new LineBreakMeasurer(iterator, g.getFontRenderContext());
            lineMeasurer.setPosition(paragraphStart);
            
            int wrappedLine = 0;
            int height = 0;
            
            // Work out the number of lines this will take
            while (lineMeasurer.getPosition() < paragraphEnd) {
                if (formatWidth < 0) {
                    lineMeasurer.nextLayout(0);
                } else {
                    lineMeasurer.nextLayout(formatWidth);
                }
                
                height += lineHeight;
                wrappedLine++;
            }
            
            // Get back to the start
            lineMeasurer.setPosition(paragraphStart);
            paragraphStart = iterator.getBeginIndex();
            paragraphEnd = iterator.getEndIndex();
            
            if (wrappedLine > 1) {
                drawPosY -= height;
            }
            
            int j = 0;
            int chars = 0;
            // Loop through each wrapped line
            while (lineMeasurer.getPosition() < paragraphEnd) {
                
                final TextLayout layout;
                if (formatWidth < 0) {
                    layout = lineMeasurer.nextLayout(0);
                } else {
                    layout = lineMeasurer.nextLayout(formatWidth);
                }
                
                // Calculate the Y offset
                if (wrappedLine == 1) {
                    drawPosY -= lineHeight;
                } else if (j != 0) {
                    drawPosY += lineHeight;
                }
                
                float drawPosX;
                // Calculate the initial X position
                if (layout.isLeftToRight()) {
                    drawPosX = 3;
                } else {
                    drawPosX = formatWidth - layout.getAdvance();
                }
                
                // Check if the target is in range
                if (drawPosY >= 0 || drawPosY <= formatHeight) {
                    
                    // If the selection includes this line
                    if (useStartLine <= i && useEndLine >= i) {
                        int firstChar;
                        int lastChar;
                        
                        // Determine the first char we care about
                        if (useStartLine < i || useStartChar < chars) {
                            firstChar = chars;
                        } else {
                            firstChar = useStartChar;
                        }
                        
                        // ... And the last
                        if (useEndLine > i || useEndChar > chars + layout.getCharacterCount()) {
                            lastChar = chars + layout.getCharacterCount();
                        } else {
                            lastChar = useEndChar;
                        }
                        
                        // If the selection includes the chars we're showing
                        if (lastChar > chars && firstChar < chars + layout.getCharacterCount()) {
                            final int trans = (int) (lineHeight / 2f + drawPosY);
                            final Shape shape = layout.getLogicalHighlightShape(firstChar - chars, lastChar - chars);
                            
                            g.setColor(UIManager.getColor("TextPane.selectionBackground"));
                            g.setBackground(UIManager.getColor("TextPane.selectionForeground"));
                            
                            g.translate(3, trans);
                            g.fill(shape);
                            g.translate(-3, -1 * trans);
                        }
                    }
                    
                    g.setColor(textPane.getForeground());
                    
                    layout.draw(g, drawPosX, drawPosY + lineHeight / 2f);
                    firstVisibleLine = i;
                    textLayouts.put(layout, new LineInfo(i, j));
                    positions.put(new Rectangle(0, (int) drawPosY,
                            (int) formatWidth, lineHeight), layout);
                }
                
                j++;
                chars += layout.getCharacterCount();
            }
            if (j > 1) {
                drawPosY -= height - lineHeight;
            }
            if (drawPosY <= 0) {
                break;
            }
        }
    }
    
    /**
     * sets the position of the scroll bar, and repaints if required.
     * @param position scroll bar position
     */
    protected void setScrollBarPosition(final int position) {
        if (scrollBarPosition != position) {
            scrollBarPosition = position;
            if (textPane.isVisible()) {
                repaint();
            }
        }
    }
    
    /** {@inheritDoc}. */
    public void mouseClicked(final MouseEvent e) {
        String clickedText = "";
        final int start;
        final int end;
        final int[] info = getClickPosition(this.getMousePosition());
        
        if (info[0] != -1) {
            final AttributedCharacterIterator iterator = document.getLine(info[0]).getIterator();
            iterator.setIndex(info[2]);
            final Object linkattr = iterator.getAttributes().get(IRCTextAttribute.HYPERLINK);
            if (linkattr instanceof String) {
                fireHyperlinkClicked((String) linkattr);
                return;
            }
            
            clickedText = textPane.getTextFromLine(info[0]);
            
            if (info[2] == -1) {
                start = -1;
                end = -1;
            } else {
                final int[] extent = getSurroundingWordIndexes(clickedText, info[2]);
                start = extent[0];
                end = extent[1];
            }
            
            if (e.getClickCount() == 2) {
                selStartLine = info[0];
                selEndLine = info[0];
                selStartChar = start;
                selEndChar = end;
            } else if (e.getClickCount() == 3) {
                selStartLine = info[0];
                selEndLine = info[0];
                selStartChar = 0;
                selEndChar = clickedText.length();
            } else if (start != -1 && end != -1) {
                checkClickedText(clickedText.substring(start, end));
            }
            
        }
        e.setSource(textPane);
        textPane.dispatchEvent(e);
    }
    
    /**
     * Returns the indexes for the word surrounding the index in the specified
     * string.
     *
     * @param text Text to get word from
     * @param index Index to get surrounding word
     *
     * @return Indexes of the word surrounding the index (start, end)
     */
    private int[] getSurroundingWordIndexes(final String text, final int index) {
        final int start = getSurroundingWordStart(text, index);
        final int end = getSurroundingWordEnd(text, index);
        
        if (start < 0 || end > text.length() || start > end) {
            return new int[]{0, 0};
        }
        
        return new int[]{start, end};
        
    }
    
    /**
     * Returns the start index for the word surrounding the index in the
     * specified string.
     *
     * @param text Text to get word from
     * @param index Index to get surrounding word
     *
     * @return Start index of the word surrounding the index
     */
    private int getSurroundingWordStart(final String text, final int index) {
        int start = index;
        
        // Traverse backwards
        while (start > 0 && start < text.length() && text.charAt(start) != ' ') {
            start--;
        }
        if (start + 1 < text.length() && text.charAt(start) == ' ') {
            start++;
        }
        
        return start;
    }
    
    /**
     * Returns the end index for the word surrounding the index in the
     * specified string.
     *
     * @param text Text to get word from
     * @param index Index to get surrounding word
     *
     * @return End index of the word surrounding the index
     */
    private int getSurroundingWordEnd(final String text, final int index) {
        int end = index;
        
        // And forwards
        while (end < text.length() && end > 0 && text.charAt(end) != ' ') {
            end++;
        }
        
        return end;
    }
    
    /** {@inheritDoc}. */
    public void mousePressed(final MouseEvent e) {
        if (e.getButton() == e.BUTTON1) {
            highlightEvent(MouseEventType.CLICK, e);
        }
        e.setSource(textPane);
        textPane.dispatchEvent(e);
    }
    
    /** {@inheritDoc}. */
    public void mouseReleased(final MouseEvent e) {
        if (e.getButton() == e.BUTTON1) {
            highlightEvent(MouseEventType.RELEASE, e);
        }
        e.setSource(textPane);
        textPane.dispatchEvent(e);
    }
    
    /** {@inheritDoc}. */
    public void mouseDragged(final MouseEvent e) {
        if (e.getModifiersEx() == e.BUTTON1_DOWN_MASK) {
            highlightEvent(MouseEventType.DRAG, e);
        }
        
        e.setSource(textPane);
        textPane.dispatchEvent(e);
    }
    
    /** {@inheritDoc}. */
    public void mouseEntered(final MouseEvent e) {
        //Ignore
    }
    
    /** {@inheritDoc}. */
    public void mouseExited(final MouseEvent e) {
        //Ignore
    }
    
    /** {@inheritDoc}. */
    public void mouseMoved(final MouseEvent e) {
        //Ignore
    }
    
    /**
     * Sets the selection for the given event.
     *
     * @param type mouse event type
     * @param e responsible mouse event
     */
    private void highlightEvent(final MouseEventType type, final MouseEvent e) {
        final Point point = this.getMousePosition();
        
        if (point == null) {
            if (e.getXOnScreen() > getLocationOnScreen().getX()
            && e.getXOnScreen() < (getLocationOnScreen().getX() + getWidth())) {
                if (getLocationOnScreen().getY() > e.getYOnScreen()) {
                    textPane.setScrollBarPosition(scrollBarPosition - 1);
                } else {
                    textPane.setScrollBarPosition(scrollBarPosition + 1);
                }
            }
        } else {
            final int[] info = getClickPosition(point);
            if (info[0] != -1 && info[1] != -1) {
                if (type == MouseEventType.CLICK) {
                    selStartLine = info[0];
                    selStartChar = info[2];
                }
                selEndLine = info[0];
                selEndChar = info[2];
                
                this.repaint();
            }
        }
    }
    
    /**
     * Checks the clicked text and fires the appropriate events.
     *
     * @param clickedText Text clicked
     */
    public void checkClickedText(final String clickedText) {
        if (textPane.isValidChannel(clickedText)) {
            fireChannelClicked(clickedText);
        }
    }
    
    /**
     *
     * Returns the line information from a mouse click inside the textpane.
     *
     * @param point mouse position
     *
     * @return line number, line part, position in whole line
     */
    public int[] getClickPosition(final Point point) {
        int lineNumber = -1;
        int linePart = -1;
        int pos = 0;
        
        if (point != null) {
            for (Map.Entry<Rectangle, TextLayout> entry : positions.entrySet()) {
                if (entry.getKey().contains(point)) {
                    lineNumber = textLayouts.get(entry.getValue()).getLine();
                    linePart = textLayouts.get(entry.getValue()).getPart();
                }
            }
            
            for (Map.Entry<Rectangle, TextLayout> entry : positions.entrySet()) {
                if (textLayouts.get(entry.getValue()).getLine() == lineNumber) {
                    if (textLayouts.get(entry.getValue()).getPart() < linePart) {
                        pos += entry.getValue().getCharacterCount();
                    } else if (textLayouts.get(entry.getValue()).getPart() == linePart) {
                        pos += entry.getValue().hitTestChar((int) point.getX(),
                                (int) point.getY()).getInsertionIndex();
                    }
                }
            }
        }
        
        return new int[]{lineNumber, linePart, pos};
    }
    
    /**
     * Informs listeners when a word has been clicked on.
     * @param text word clicked on
     */
    private void fireHyperlinkClicked(final String text) {
        textPane.fireHyperlinkClicked(text);
    }
    
    /**
     * Informs listeners when a word has been clicked on.
     * @param text word clicked on
     */
    private void fireChannelClicked(final String text) {
        textPane.fireChannelClicked(text);
    }
    
    /**
     * Returns the selected range info.
     *  <ul>
     *    <li>0 = start line</li>
     *    <li>1 = start char</li>
     *    <li>2 = end line</li>
     *    <li>3 = end char</li>
     *  </ul>
     *
     * @return Selected range info
     */
    protected int[] getSelectedRange() {
        if (selStartLine > selEndLine) {
            // Swap both
            return new int[]{selEndLine, selEndChar, selStartLine, selStartChar, };
        } else if (selStartLine == selEndLine && selStartChar > selEndChar) {
            // Just swap the chars
            return new int[]{selStartLine, selEndChar, selEndLine, selStartChar, };
        } else {
            // Swap nothing
            return new int[]{selStartLine, selStartChar, selEndLine, selEndChar, };
        }
    }
    
    /** Clears the selection. */
    protected void clearSelection() {
        selEndLine = selStartLine;
        selEndChar = selStartChar;
    }
    
    /**
     * Selects the specified region of text.
     *
     * @param startLine Start line
     * @param startChar Start char
     * @param endLine End line
     * @param endChar End char
     */
    public void setSelectedRange(final int startLine, final int startChar,
            final int endLine, final int endChar) {
        selStartLine = startLine;
        selStartChar = startChar;
        selEndLine = endLine;
        selEndChar = endChar;
        repaint();
    }
    
    /**
     * Returns the first visible line.
     *
     * @return the line number of the first visible line
     */
    public int getFirstVisibleLine() {
        return firstVisibleLine;
    }
    
    /**
     * Returns the last visible line.
     *
     * @return the line number of the last visible line
     */
    public int getLastVisibleLine() {
        return lastVisibleLine;
    }
}
