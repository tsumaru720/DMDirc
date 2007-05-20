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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.DefaultComboBoxModel;
import javax.swing.InputVerifier;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.WindowConstants;

import com.dmdirc.Config;
import com.dmdirc.Server;
import com.dmdirc.ServerManager;
import com.dmdirc.identities.ConfigSource;
import com.dmdirc.identities.IdentityManager;
import com.dmdirc.logger.ErrorLevel;
import com.dmdirc.logger.Logger;
import com.dmdirc.ui.MainFrame;
import com.dmdirc.ui.components.StandardDialog;

import static com.dmdirc.ui.UIUtilities.LARGE_BORDER;
import static com.dmdirc.ui.UIUtilities.SMALL_BORDER;

/**
 * Dialog that allows the user to enter details of a new server to connect to.
 */
public final class NewServerDialog extends StandardDialog {
    
    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 7;
    
    /**
     * A previously created instance of NewServerDialog.
     */
    private static NewServerDialog me;
    
    /** checkbox. */
    private JCheckBox newServerWindowCheck;
    
    /** checkbox. */
    private JCheckBox rememberPasswordCheck;
    
    /** checkbox. */
    private JCheckBox autoConnectCheck;
    
    /** checkbox. */
    private JCheckBox sslCheck;
    
    /** label. */
    private JLabel serverLabel;
    
    /** label. */
    private JLabel instructionLabel;
    
    /** label. */
    private JLabel portLabel;
    
    /** label. */
    private JLabel passwordLabel;
    
    /** text field. */
    private JTextField serverField;
    
    /** text field. */
    private JTextField portField;
    
    /** text field. */
    private JTextField passwordField;
    
    /** label. */
    private JLabel identityLabel;
    
    /** combo box. */
    private JComboBox identityField;
    
    /** label. */
    private JLabel serverListLabel;
    
    /** combo box. */
    private JComboBox serverListField;
    
    /** button. */
    private JButton editProfileButton;
    
    /**
     * Creates a new instance of the dialog.
     */
    private NewServerDialog() {
        super(MainFrame.getMainFrame(), false);
        
        initComponents();
        
        layoutComponents();
        
        serverField.setText(Config.getOption("general", "server"));
        portField.setText(Config.getOption("general", "port"));
        passwordField.setText(Config.getOption("general", "password"));
        portField.setInputVerifier(new PortVerifier());
        
        addCallbacks();
    }
    
    /**
     * Creates the new server dialog if one doesn't exist, and displays it.
     */
    public static synchronized void showNewServerDialog() {
        if (me == null) {
            me = new NewServerDialog();
        }
        
        me.serverField.requestFocus();
        
        if (ServerManager.getServerManager().numServers() == 0
                || MainFrame.getMainFrame().getActiveFrame() == null) {
            me.newServerWindowCheck.setSelected(true);
            me.newServerWindowCheck.setEnabled(false);
        } else {
            me.newServerWindowCheck.setEnabled(true);
        }
        
        me.populateProfiles();
        
        me.setLocationRelativeTo(MainFrame.getMainFrame());
        me.setVisible(true);
        me.requestFocus();
    }
    
    /**
     * Returns the current instance of the NewServerDialog.
     *
     * @return The current NewServerDialog instance
     */
    public static synchronized NewServerDialog getNewServerDialog() {
        if (me == null) {
            me = new NewServerDialog();
        }
        return me;
    }
    
    /**
     * Adds listeners for various objects in the dialog.
     */
    private void addCallbacks() {
        getCancelButton().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                NewServerDialog.this.setVisible(false);
            }
        });
        getOkButton().addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                final String host = serverField.getText();
                final String pass = passwordField.getText();
                int port;
                try {
                    port = Integer.parseInt(portField.getText());
                } catch (NumberFormatException ex) {
                    port = 6667;
                }
                
                NewServerDialog.this.setVisible(false);
                
                final ConfigSource profile =
                        (ConfigSource) identityField.getSelectedItem();
                
                // Open in a new window?
                if (newServerWindowCheck.isSelected()
                || ServerManager.getServerManager().numServers() == 0
                        || MainFrame.getMainFrame().getActiveFrame() == null) {
                    new Server(host, port, pass, sslCheck.isSelected(), profile);
                } else {
                    final JInternalFrame active =
                            MainFrame.getMainFrame().getActiveFrame();
                    final Server server = ServerManager.getServerManager().
                            getServerFromFrame(active);
                    if (server == null) {
                        Logger.error(ErrorLevel.ERROR, "Cannot determine "
                                + "active server window");
                    } else {
                        server.connect(host, port, pass, sslCheck.isSelected(),
                                profile);
                    }
                }
            }
        });
        editProfileButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent actionEvent) {
                new ProfileEditorDialog();
            }
        });
    }
    
    /**
     * Initialises the components in this dialog.
     */
    private void initComponents() {
        final JButton button1 = new JButton();
        final JButton button2 = new JButton();
        
        serverLabel = new JLabel();
        serverField = new JTextField();
        instructionLabel = new JLabel();
        portLabel = new JLabel();
        portField = new JTextField();
        passwordLabel = new JLabel();
        passwordField = new JPasswordField();
        newServerWindowCheck = new JCheckBox();
        rememberPasswordCheck = new JCheckBox();
        autoConnectCheck = new JCheckBox();
        sslCheck = new JCheckBox();
        identityLabel = new JLabel();
        identityField = new JComboBox();
        serverListLabel = new JLabel();
        serverListField = new JComboBox(new String[]{});
        editProfileButton = new JButton();
        
        setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        orderButtons(button2, button1);
        setTitle("Connect to a new server");
        
        serverLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        serverLabel.setText("Server:");
        
        serverField.setText("blueyonder.uk.quakenet.org");
        
        instructionLabel.setText("To connect to a new IRC server, enter "
                + "the server name below");
        
        portLabel.setHorizontalAlignment(SwingConstants.RIGHT);
        portLabel.setText("Port:");
        
        portField.setText("7000");
        
        passwordLabel.setText("Password:");
        
        identityLabel.setText("Profile: ");
        populateProfiles();
        
        editProfileButton.setText("Edit");
        
        serverListLabel.setText("Server: ");
        
        serverListField.setEnabled(false);
        
        newServerWindowCheck.setText("Open in a new server window");
        newServerWindowCheck.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0));
        newServerWindowCheck.setMargin(new Insets(0, 0, 0, 0));
        
        rememberPasswordCheck.setText("Remember server password");
        rememberPasswordCheck.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0));
        rememberPasswordCheck.setEnabled(false);
        rememberPasswordCheck.setMargin(new Insets(0, 0, 0, 0));
        
        autoConnectCheck.setText("Connect to this server automatically "
                + "in the future");
        autoConnectCheck.setBorder(
                BorderFactory.createEmptyBorder(0, 0, 0, 0));
        autoConnectCheck.setEnabled(false);
        autoConnectCheck.setMargin(new Insets(0, 0, 0, 0));
        
        sslCheck.setText("Use a secure (SSL) connection");
        sslCheck.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        sslCheck.setMargin(new Insets(0, 0, 0, 0));
    }
    
    /** Populates the profiles list. */
    public void populateProfiles() {
        final ConfigSource[] profiles =
                IdentityManager.getProfiles().toArray(new ConfigSource[0]);
        ((DefaultComboBoxModel) identityField.getModel()).removeAllElements();
        for (ConfigSource profile : profiles) {
            ((DefaultComboBoxModel)
            identityField.getModel()).addElement(profile);
        }
    }
    
    /**
     * Lays out the components in the dialog.
     */
    private void layoutComponents() {
        final GridBagConstraints constraints = new GridBagConstraints();
        getContentPane().setLayout(new GridBagLayout());
        
        constraints.gridx = 0;
        constraints.gridy = 0;
        constraints.gridwidth = 4;
        constraints.weightx = 0.0;
        constraints.weighty = 1.0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.anchor = GridBagConstraints.WEST;
        constraints.insets = new Insets(LARGE_BORDER, LARGE_BORDER,
                LARGE_BORDER, LARGE_BORDER);
        getContentPane().add(instructionLabel, constraints);
        
        constraints.gridy = 1;
        constraints.insets = new Insets(SMALL_BORDER, LARGE_BORDER,
                SMALL_BORDER, SMALL_BORDER);
        constraints.gridwidth = 1;
        getContentPane().add(serverLabel, constraints);
        constraints.insets = new Insets(SMALL_BORDER, 0,
                SMALL_BORDER, LARGE_BORDER);
        constraints.gridx = 1;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        constraints.weightx = 1.0;
        getContentPane().add(serverField, constraints);
        
        constraints.insets = new Insets(SMALL_BORDER, LARGE_BORDER,
                SMALL_BORDER, SMALL_BORDER);
        constraints.gridy = 2;
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        getContentPane().add(portLabel, constraints);
        constraints.insets = new Insets(SMALL_BORDER, 0,
                SMALL_BORDER, LARGE_BORDER);
        constraints.gridx = 1;
        constraints.weightx = 0.2;
        constraints.gridwidth = 3;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(portField, constraints);
        
        constraints.gridwidth = 1;
        constraints.gridy = 3;
        constraints.insets = new Insets(SMALL_BORDER, LARGE_BORDER,
                SMALL_BORDER, SMALL_BORDER);
        constraints.gridx = 0;
        constraints.weightx = 0.0;
        constraints.fill = GridBagConstraints.NONE;
        getContentPane().add(passwordLabel, constraints);
        constraints.insets = new Insets(SMALL_BORDER, 0,
                SMALL_BORDER, LARGE_BORDER);
        constraints.gridwidth = 3;
        constraints.gridx = 1;
        constraints.weightx = 0.8;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(passwordField, constraints);
        
        /*constraints.insets = new Insets(SMALL_BORDER, LARGE_BORDER,
                SMALL_BORDER, SMALL_BORDER);
        constraints.gridy = 4;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        getContentPane().add(serverListLabel, constraints);
        constraints.insets = new Insets(SMALL_BORDER, 0,
                SMALL_BORDER, LARGE_BORDER);
        constraints.gridx = 1;
        constraints.gridwidth = 3;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(serverListField, constraints);*/
        
        constraints.insets = new Insets(SMALL_BORDER, LARGE_BORDER,
                SMALL_BORDER, SMALL_BORDER);
        constraints.gridy = 5;
        constraints.gridx = 0;
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        getContentPane().add(identityLabel, constraints);
        constraints.insets = new Insets(SMALL_BORDER, 0,
                SMALL_BORDER, LARGE_BORDER);
        constraints.gridx = 1;
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        getContentPane().add(identityField, constraints);
        constraints.gridwidth = 1;
        constraints.gridx = 3;
        getContentPane().add(editProfileButton, constraints);
        
        constraints.weightx = 0.0;
        constraints.gridwidth = 4;
        constraints.gridy = 6;
        constraints.gridx = 0;
        constraints.fill = GridBagConstraints.NONE;
        constraints.insets = new Insets(SMALL_BORDER, LARGE_BORDER,
                SMALL_BORDER, SMALL_BORDER);
        getContentPane().add(sslCheck, constraints);
        
        constraints.gridy = 7;
        getContentPane().add(newServerWindowCheck, constraints);
        
        constraints.gridy = 8;
        getContentPane().add(rememberPasswordCheck, constraints);
        
        constraints.gridy = 9;
        getContentPane().add(autoConnectCheck, constraints);
        
        constraints.weighty = 0.0;
        constraints.weightx = 1.0;
        constraints.gridx = 0;
        constraints.gridy = 10;
        constraints.gridwidth = 2;
        constraints.fill = GridBagConstraints.BOTH;
        getContentPane().add(Box.createHorizontalGlue(), constraints);
        
        constraints.gridwidth = 1;
        constraints.weightx = 0.0;
        constraints.insets.set(LARGE_BORDER, LARGE_BORDER, LARGE_BORDER, 0);
        constraints.gridx = 2;
        constraints.anchor = GridBagConstraints.EAST;
        constraints.fill = GridBagConstraints.NONE;
        getContentPane().add(getLeftButton(), constraints);
        
        constraints.insets.set(LARGE_BORDER, LARGE_BORDER, LARGE_BORDER,
                LARGE_BORDER);
        constraints.gridx = 3;
        getContentPane().add(getRightButton(), constraints);
        
        pack();
    }
    
}

/**
 * Verifies that the port number is a valid port.
 */
class PortVerifier extends InputVerifier {
    
    /** The minimum port number. */
    private static final int MIN_PORT = 0;
    
    /** The maximum port number. */
    private static final int MAX_PORT = 65535;
    
    /**
     * Creates a new instance of PortVerifier.
     */
    public PortVerifier() {
        super();
    }
    
    /**
     * Verifies that the number specified in the textfield is a valid port.
     * @param jComponent The component to be tested
     * @return true iff the number is a valid port, false otherwise
     */
    public boolean verify(final JComponent jComponent) {
        final JTextField textField = (JTextField) jComponent;
        try {
            final int port = Integer.parseInt(textField.getText());
            return port > MIN_PORT && port <= MAX_PORT;
        } catch (NumberFormatException e) {
            return false;
        }
    }
    
}
