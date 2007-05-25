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

package com.dmdirc.ui.dialogs;

import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.WindowConstants;

import com.dmdirc.BrowserLauncher;
import com.dmdirc.Main;
import com.dmdirc.ui.MainFrame;

import static com.dmdirc.ui.UIUtilities.LARGE_BORDER;
import static com.dmdirc.ui.UIUtilities.SMALL_BORDER;

/**
 * About dialog.
 */
public final class AboutDialog extends JDialog implements ActionListener,
        MouseListener {
    
    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 2;
    
    /** Previously created instance of AboutDialog. */
    private static AboutDialog me;
    
    /** link label. */
    private JLabel linkLabel;
    
    /** Creates a new instance of AboutDialog. */
    private AboutDialog() {
        super(MainFrame.getMainFrame(), false);
        initComponents();
        setLocationRelativeTo(MainFrame.getMainFrame());
        this.setVisible(true);
    }
    
    /** Creates the dialog if one doesn't exist, and displays it. */
    public static synchronized void showAboutDialog() {
        if (me == null) {
            me = new AboutDialog();
        } else {
            me.setVisible(true);
            me.requestFocus();
        }
    }
    
    /** Initialises the main UI components. */
    private void initComponents() {
        final GridBagConstraints constraints = new GridBagConstraints();
        linkLabel = new JLabel();
        final JLabel about = new JLabel();
        final JLabel authors = new JLabel();
        final JButton okButton = new JButton();
        
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        getContentPane().setLayout(new GridBagLayout());
        setTitle("About DMDirc");
        setResizable(false);
        
        linkLabel.setText("<html>"
                + "<a href=\"http://www.dmdirc.com\">http://www.dmdirc.com</a>"
                + "</html>");
        linkLabel.addMouseListener(this);
        
        authors.setText("<html>"
                + "Chris 'MD87' Smith<br>"
                + "Greg 'Greboid' Holmes<br>"
                + "Shane 'Dataforce' Mc Cormack."
                + "</html>");
        
        about.setText("<html>DMDirc - Cross platform IRC client.<br>" 
                + " Version: " + Main.VERSION + "</html>");
        
        okButton.setText("OK");
        okButton.addActionListener(this);
        okButton.setActionCommand("OK");
        
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(LARGE_BORDER, LARGE_BORDER,
                SMALL_BORDER, LARGE_BORDER);
        getContentPane().add(about, constraints);
        
        constraints.anchor = GridBagConstraints.CENTER;
        constraints.gridx = 0;
        constraints.gridy = 1;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, LARGE_BORDER,
                LARGE_BORDER, LARGE_BORDER);
        getContentPane().add(linkLabel, constraints);
        
        constraints.anchor = GridBagConstraints.LINE_START;
        constraints.gridx = 0;
        constraints.gridy = 2;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.BOTH;
        constraints.insets = new Insets(0, LARGE_BORDER,
                LARGE_BORDER, LARGE_BORDER);
        getContentPane().add(authors, constraints);
        
        constraints.weighty = 0.0;
        constraints.gridx = 0;
        constraints.gridy = 3;
        constraints.gridwidth = 1;
        constraints.insets = new Insets(0, 0,
                0, 0);
        getContentPane().add(Box.createHorizontalGlue(), constraints);
        
        constraints.gridx = 2;
        constraints.insets = new Insets(0, LARGE_BORDER,
                LARGE_BORDER, LARGE_BORDER);
        getContentPane().add(okButton, constraints);
        
        pack();
    }
    
    /** {@inheritDoc}. */
    public void actionPerformed(final ActionEvent e) {
        if (e.getActionCommand().equals("OK")) {
            setVisible(false);
            dispose();
        }
    }
    
    /** {@inheritDoc}. */
    public void mouseClicked(final MouseEvent e) {
        if (e.getSource() == linkLabel) {
            BrowserLauncher.openURL("http://www.dmdirc.com");
        }
    }
    
    /** {@inheritDoc}. */
    public void mousePressed(final MouseEvent e) {
        //Ignore
    }
    
    /** {@inheritDoc}. */
    public void mouseReleased(final MouseEvent e) {
        //Ignore
    }
    
    /** {@inheritDoc}. */
    public void mouseEntered(final MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
    }
    
    /** {@inheritDoc}. */
    public void mouseExited(final MouseEvent e) {
        setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
    }
    
}
