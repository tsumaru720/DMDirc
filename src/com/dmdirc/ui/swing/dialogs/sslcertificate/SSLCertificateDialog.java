/*
 * Copyright (c) 2006-2008 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

package com.dmdirc.ui.swing.dialogs.sslcertificate;

import com.dmdirc.ui.core.dialogs.sslcertificate.SSLCertificateDialogModel;
import com.dmdirc.ui.swing.SwingController;
import com.dmdirc.ui.swing.components.StandardDialog;
import com.dmdirc.ui.swing.components.TextLabel;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.JButton;

import net.miginfocom.swing.MigLayout;

/**
 * SSL Certificate information dialog. Also provides the ability to accept and
 * reject certificates whilst connecting to an SSL server.
 */
public class SSLCertificateDialog extends StandardDialog implements ActionListener {
    
    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 1;
    /** SSL Certificate dialog model. */
    private SSLCertificateDialogModel model;
    /** Panel listing actions that can be taken on a certificate. */
    private ActionsPanel actions;
    /** Panel showing the certificate chain of a certificate. */
    private CertificateChainPanel chain;
    /** Panel showing the information available for a certificate. */
    private CertificateInfoPanel info;
    /** Panel showing a summary of the certificate information. */
    private SummaryPanel summary;
    /** Informational blurb. */
    private TextLabel blurb;

    /** 
     * Creates a new instance of ActionsManagerDialog.
     * 
     * @param model dialog model
     */
    public  SSLCertificateDialog(final SSLCertificateDialogModel model) {
        super(SwingController.getMainFrame(), false);
        
        this.model = model;

        initComponents();
        addListeners();
        layoutComponents();

        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setTitle("DMDirc: Certificate Information");
        setResizable(false);
    }

    /**
     * Packs, locates and shows the dialog.
     */
    public void display() {
        pack();
        setLocationRelativeTo(SwingController.getMainFrame());
        setVisible(true);
    }

    private void addListeners() {
        getOkButton().addActionListener(this);
        addWindowListener(new WindowAdapter() {

            /** {@inheritDoc} */
            @Override
            public void windowClosing(final WindowEvent e) {
                getCancelButton().doClick();
            }
        });
    }

    private void initComponents() {
        orderButtons(new JButton(), new JButton());
        actions = new ActionsPanel();
        chain = new CertificateChainPanel();
        info = new CertificateInfoPanel();
        summary = new SummaryPanel();
        blurb = new TextLabel();
    }

    private void layoutComponents() {
        setLayout(new MigLayout("wrap 2"));
        
        add(blurb, "span 2");
        add(chain, "w! 20");
        add(info, "w! 80");
        add(summary, "span 2");
        add(actions, "span 2");
        add(getOkButton(), "skip, right");
    }

    /** 
     * {@inheritDoc}.
     * 
     * @param e Action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        dispose();
    }

}