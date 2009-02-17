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

package com.dmdirc.addons.ui_swing;

import com.dmdirc.Channel;
import com.dmdirc.ServerState;
import com.dmdirc.addons.ui_swing.components.InputTextFrame;
import com.dmdirc.addons.ui_swing.components.SnappingJSplitPane;
import com.dmdirc.addons.ui_swing.components.SwingInputHandler;
import com.dmdirc.addons.ui_swing.components.renderers.NicklistRenderer;
import com.dmdirc.addons.ui_swing.dialogs.channelsetting.ChannelSettingsDialog;
import com.dmdirc.addons.ui_swing.textpane.ClickType;
import com.dmdirc.commandparser.PopupType;
import com.dmdirc.commandparser.parsers.ChannelCommandParser;
import com.dmdirc.commandparser.parsers.CommandParser;
import com.dmdirc.parser.irc.ChannelClientInfo;
import com.dmdirc.ui.interfaces.ChannelWindow;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import net.miginfocom.swing.MigLayout;

/**
 * The channel frame is the GUI component that represents a channel to the user.
 */
public final class ChannelFrame extends InputTextFrame implements ActionListener,
        ChannelWindow {

    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 10;
    /** The nick list model used for this channel's nickname list. */
    private NicklistListModel nicklistModel;
    /** This channel's command parser. */
    private final ChannelCommandParser commandParser;
    /** Nick list. */
    private JList nickList;
    /** split pane. */
    private JSplitPane splitPane;
    /** popup menu item. */
    private JMenuItem settingsMI;
    /** The channel object that owns this frame. */
    private final Channel parent;

    /**
     * Creates a new instance of ChannelFrame. Sets up callbacks and handlers,
     * and default options for the form.
     * @param owner The Channel object that owns this frame
     */
    public ChannelFrame(final Channel owner) {
        super(owner);

        parent = owner;

        initComponents();

        nickList.setBackground(getConfigManager().getOptionColour("ui",
                "nicklistbackgroundcolour",
                getConfigManager().getOptionColour("ui", "backgroundcolour")));
        nickList.setForeground(getConfigManager().getOptionColour("ui",
                "nicklistforegroundcolour",
                getConfigManager().getOptionColour("ui", "foregroundcolour")));

        getConfigManager().addChangeListener("ui", "nicklistforegroundcolour",
                this);
        getConfigManager().addChangeListener("ui", "foregroundcolour", this);
        getConfigManager().addChangeListener("ui", "nicklistbackgroundcolour",
                this);
        getConfigManager().addChangeListener("ui", "backgroundcolour", this);
        getConfigManager().addChangeListener("ui", "nickListAltBackgroundColour",
                this);

        commandParser =
                new ChannelCommandParser(((Channel) getContainer()).getServer(),
                (Channel) getContainer());

        setInputHandler(new SwingInputHandler(getInputField(), commandParser,
                this));
    }

    /**
     * Retrieves the command Parser for this command window.
     * @return This window's command Parser
     */
    @Override
    public CommandParser getCommandParser() {
        return commandParser;
    }

    /** {@inheritDoc} */
    @Override
    public void updateNames(final List<ChannelClientInfo> clients) {
        SwingUtilities.invokeLater(new Runnable() {

            /** {@inheritDoc} */
            @Override
            public void run() {
                nicklistModel.replace(clients);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void updateNames() {
        SwingUtilities.invokeLater(new Runnable() {

            /** {@inheritDoc} */
            @Override
            public void run() {
                nicklistModel.sort();
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void addName(final ChannelClientInfo client) {
        SwingUtilities.invokeLater(new Runnable() {

            /** {@inheritDoc} */
            @Override
            public void run() {
                nicklistModel.add(client);
            }
        });
    }

    /** {@inheritDoc} */
    @Override
    public void removeName(final ChannelClientInfo client) {
        SwingUtilities.invokeLater(new Runnable() {

            /** {@inheritDoc} */
            @Override
            public void run() {
                nicklistModel.remove(client);
            }
        });
    }

    /**
     * Retrieves this channel frame's nicklist component.
     * @return This channel's nicklist
     */
    public JList getNickList() {
        return nickList;
    }

    /** {@inheritDoc} */
    @Override
    public Channel getChannel() {
        return parent;
    }

    /**
     * Initialises the compoents in this frame.
     */
    private void initComponents() {
        settingsMI = new JMenuItem("Settings");
        settingsMI.addActionListener(this);

        splitPane =
                new SnappingJSplitPane(SnappingJSplitPane.Orientation.HORIZONTAL,
                false);

        final JScrollPane nickScrollPane = new JScrollPane();
        nickList = new JList();
        nickList.setCellRenderer(new NicklistRenderer(parent.getConfigManager(),
                nickList));
        nickList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        nickList.addMouseListener(this);

        nicklistModel = new NicklistListModel();

        nickList.setModel(nicklistModel);
        nickScrollPane.setViewportView(nickList);

        nickScrollPane.setPreferredSize(new Dimension(150, 0));
        nickScrollPane.setMinimumSize(new Dimension(150, 0));

        getContentPane().setLayout(new MigLayout("fill, ins 0, hidemode 3, wrap 1"));

        getContentPane().add(splitPane, "grow");
        getContentPane().add(getSearchBar(), "growx, pushx");
        getContentPane().add(inputPanel, "growx, pushx");

        splitPane.setLeftComponent(getTextPane());
        splitPane.setRightComponent(nickScrollPane);
        splitPane.setResizeWeight(1);

        pack();
    }

    /**
     * {@inheritDoc}.
     * 
     * @param actionEvent Action event
     */
    @Override
    public void actionPerformed(final ActionEvent actionEvent) {
        if (actionEvent.getSource() == settingsMI) {
            ChannelSettingsDialog.showChannelSettingsDialog((Channel) getContainer());
        }
    }

    /**
     * Returns the splitpane.
     * @return nicklist JSplitPane
     */
    public JSplitPane getSplitPane() {
        return splitPane;
    }

    /**
     * Checks for url's, channels and nicknames. {@inheritDoc}
     */
    @Override
    public void mouseClicked(final MouseEvent mouseEvent) {
        processMouseEvent(mouseEvent);
        super.mouseClicked(mouseEvent);
    }

    /**
     * Not needed for this class. {@inheritDoc}
     */
    @Override
    public void mousePressed(final MouseEvent mouseEvent) {
        processMouseEvent(mouseEvent);
        super.mousePressed(mouseEvent);
    }

    /**
     * Not needed for this class. {@inheritDoc}
     */
    @Override
    public void mouseReleased(final MouseEvent mouseEvent) {
        processMouseEvent(mouseEvent);
        super.mouseReleased(mouseEvent);
    }

    /**
     * Processes every mouse button event to check for a popup trigger.
     *
     * @param e mouse event
     */
    @Override
    public void processMouseEvent(final MouseEvent e) {
        if (e.getSource() == nickList && nickList.getMousePosition() != null &&
                getMousePosition() != null) {
            boolean showMenu = checkShowNicklistMenu();
            if (!showMenu) {
                showMenu = selectNickUnderCursor();
            }
            if (showMenu) {
                if (e.isPopupTrigger()) {
                    showPopupMenu(ClickType.NICKNAME, getMousePosition(),
                            ((ChannelClientInfo) nickList.getSelectedValue()).getNickname());
                }
            } else {
                nickList.clearSelection();
            }
        }
        super.processMouseEvent(e);
    }

    /**
     *
     * Checks whether to show the nicklist menu.
     *
     * @return whether to show the nicklist menu
     */
    private boolean checkShowNicklistMenu() {
        boolean showMenu = false;
        final Point mousePos = nickList.getMousePosition();
        if (mousePos != null) {
            for (int i = 0; i < nickList.getModel().getSize(); i++) {
                if (nickList.getCellBounds(i, i) != null && nickList.getCellBounds(i, i).
                        contains(mousePos) && nickList.isSelectedIndex(i)) {
                    showMenu = true;
                    break;
                }
            }
        }
        return showMenu;
    }

    /**
     * Selects the nick underneath the mouse.
     *
     * @return true if an item was selected
     */
    private boolean selectNickUnderCursor() {
        boolean suceeded = false;
        final Point mousePos = nickList.getMousePosition();
        if (mousePos != null) {
            for (int i = 0; i < nickList.getModel().getSize(); i++) {
                if (nickList.getCellBounds(i, i) != null && nickList.getCellBounds(i, i).
                        contains(mousePos)) {
                    nickList.setSelectedIndex(i);
                    suceeded = true;
                    break;
                }
            }
        }
        return suceeded;
    }

    /** {@inheritDoc} */
    @Override
    public void configChanged(final String domain, final String key) {
        super.configChanged(domain, key);

        if ("nickListAltBackgroundColour".equals(key) ||
                "nicklistbackgroundcolour".equals(key) ||
                "backgroundcolour".equals(key) ||
                "nicklistforegroundcolour".equals(key) ||
                "foregroundcolour".equals(key)) {
            nickList.setBackground(getConfigManager().getOptionColour("ui",
                    "nicklistbackgroundcolour",
                    getConfigManager().getOptionColour("ui", "backgroundcolour")));
            nickList.setForeground(getConfigManager().getOptionColour("ui",
                    "nicklistforegroundcolour",
                    getConfigManager().getOptionColour("ui", "foregroundcolour")));
            nickList.repaint();
        }
    }

    /** {@inheritDoc} */
    @Override
    public PopupType getNicknamePopupType() {
        return PopupType.CHAN_NICK;
    }

    /** {@inheritDoc} */
    @Override
    public PopupType getChannelPopupType() {
        return PopupType.CHAN_NORMAL;
    }

    /** {@inheritDoc} */
    @Override
    public PopupType getHyperlinkPopupType() {
        return PopupType.CHAN_HYPERLINK;
    }

    /** {@inheritDoc} */
    @Override
    public PopupType getNormalPopupType() {
        return PopupType.CHAN_NORMAL;
    }

    /** {@inheritDoc} */
    @Override
    public void addCustomPopupItems(final JPopupMenu popupMenu) {
        if (getContainer().getServer().getState().equals(ServerState.CONNECTED)) {
            settingsMI.setEnabled(true);
        } else {
            settingsMI.setEnabled(false);
        }

        if (popupMenu.getComponentCount() > 0) {
            popupMenu.addSeparator();
        }

        popupMenu.add(settingsMI);
    }

    /** {@inheritDoc} */
    @Override
    public void redrawNicklist() {
        getNickList().repaint();
    }
}
