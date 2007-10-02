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

package com.dmdirc.ui.swing;

import com.dmdirc.Channel;
import com.dmdirc.Main;
import com.dmdirc.commandparser.commands.ChannelCommand;
import com.dmdirc.commandparser.parsers.ChannelCommandParser;
import com.dmdirc.commandparser.commands.Command;
import com.dmdirc.commandparser.parsers.CommandParser;
import com.dmdirc.commandparser.commands.ServerCommand;
import com.dmdirc.parser.ChannelClientInfo;
import com.dmdirc.ui.input.InputHandler;
import com.dmdirc.ui.interfaces.ChannelWindow;
import com.dmdirc.ui.swing.components.InputFrame;
import com.dmdirc.ui.swing.dialogs.channelsetting.ChannelSettingsDialog;
import static com.dmdirc.ui.swing.UIUtilities.SMALL_BORDER;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;

import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.plaf.basic.BasicSplitPaneDivider;
import javax.swing.plaf.basic.BasicSplitPaneUI;

/**
 * The channel frame is the GUI component that represents a channel to the user.
 */
public final class ChannelFrame extends InputFrame implements MouseListener,
        ActionListener, ChannelWindow {
    
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
        
        nickList.setBackground(getConfigManager().getOptionColour("ui", "nicklistbackgroundcolour",
                getConfigManager().getOptionColour("ui", "backgroundcolour", Color.WHITE)));
        nickList.setForeground(getConfigManager().getOptionColour("ui", "nicklistforegroundcolour",
                getConfigManager().getOptionColour("ui", "foregroundcolour", Color.BLACK)));
        
        getConfigManager().addChangeListener("ui", "nicklistforegroundcolour", this);
        getConfigManager().addChangeListener("ui", "nicklistbackgroundcolour", this);
        getConfigManager().addChangeListener("nicklist", "altBackgroundColour", this);
        
        commandParser = new ChannelCommandParser(((Channel) getContainer()).
                getServer(), (Channel) getContainer());
        
        setInputHandler(new InputHandler(getInputField(), commandParser, this));
    }
    
    /**
     * Retrieves the command Parser for this command window.
     * @return This window's command Parser
     */
    public CommandParser getCommandParser() {
        return commandParser;
    }
    
    /** {@inheritDoc} */
    public void updateNames(final List<ChannelClientInfo> clients) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nicklistModel.replace(clients);
            }
        });
    }
    
    /** {@inheritDoc} */
    public void updateNames() {
        nicklistModel.sort();
    }
    
    /** {@inheritDoc} */
    public void addName(final ChannelClientInfo client) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                nicklistModel.add(client);
            }
        });
    }
    
    /** {@inheritDoc} */
    public void removeName(final ChannelClientInfo client) {
        SwingUtilities.invokeLater(new Runnable() {
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
    
    /**
     * Initialises the compoents in this frame.
     */
    private void initComponents() {
        settingsMI = new JMenuItem("Settings");
        settingsMI.addActionListener(this);
        getPopup().addSeparator();
        getPopup().add(settingsMI);
        final JPanel panel = new JPanel(new BorderLayout());
        
        splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        
        final JScrollPane nickScrollPane = new JScrollPane();
        nickList = new JList();
        nickList.setCellRenderer(new NicklistRenderer(parent.getConfigManager()));
        nickList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        
        nickList.addMouseListener(this);
        
        splitPane.setBorder(null);
        final BasicSplitPaneDivider divider =
                ((BasicSplitPaneUI) splitPane.getUI()).getDivider();
        if (divider != null) {
            divider.setBorder(null);
        }
        
        nicklistModel = new NicklistListModel();
        
        nickList.setModel(nicklistModel);
        nickScrollPane.setViewportView(nickList);
        
        nickScrollPane.setMinimumSize(new Dimension(150, 10));
        getTextPane().setPreferredSize(new Dimension(((MainFrame) Main.getUI().
                getMainWindow()).getWidth(), 10));
        
        panel.add(getSearchBar(), BorderLayout.PAGE_START);
        panel.add(inputPanel, BorderLayout.PAGE_END);
        
        getContentPane().setLayout(new BorderLayout(SMALL_BORDER, SMALL_BORDER));
        
        getContentPane().add(splitPane, BorderLayout.CENTER);
        getContentPane().add(panel, BorderLayout.PAGE_END);
        
        splitPane.setLeftComponent(getTextPane());
        splitPane.setRightComponent(nickScrollPane);
        
        splitPane.setDividerLocation((double) 1);
        splitPane.setResizeWeight(1);
        splitPane.setDividerSize(SMALL_BORDER);
        splitPane.setContinuousLayout(true);
        
        pack();
    }
    
    /**
     * {@inheritDoc}.
     */
    public void actionPerformed(final ActionEvent actionEvent) {
        super.actionPerformed(actionEvent);
        if (commands.containsKey(actionEvent.getActionCommand())) {
            final Command command = commands.get(actionEvent.getActionCommand());
            for (Object nickname : nickList.getSelectedValues()) {
                if (command instanceof ChannelCommand) {
                    ((ChannelCommand) commands.get(actionEvent.getActionCommand())).
                            execute(this, getContainer().getServer(),
                            (Channel) this.getContainer(), false,
                            ((ChannelClientInfo) nickname).getNickname());
                } else if (command instanceof ServerCommand) {
                    ((ServerCommand) commands.get(actionEvent.getActionCommand())).
                            execute(this, getContainer().getServer(), false,
                            ((ChannelClientInfo) nickname).getNickname());
                }
            }
        }
        if (actionEvent.getSource() == settingsMI) {
            ChannelSettingsDialog.getChannelSettingDialog((Channel) getContainer()).setVisible(true);
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
    public void mouseClicked(final MouseEvent mouseEvent) {
        processMouseEvent(mouseEvent);
        super.mouseClicked(mouseEvent);
    }
    
    /**
     * Not needed for this class. {@inheritDoc}
     */
    public void mousePressed(final MouseEvent mouseEvent) {
        processMouseEvent(mouseEvent);
        super.mousePressed(mouseEvent);
    }
    
    /**
     * Not needed for this class. {@inheritDoc}
     */
    public void mouseReleased(final MouseEvent mouseEvent) {
        processMouseEvent(mouseEvent);
        super.mouseReleased(mouseEvent);
    }
    
    /**
     * Processes every mouse button event to check for a popup trigger.
     *
     * @param e mouse event
     */
    public void processMouseEvent(final MouseEvent e) {
        if (e.getSource() == nickList && nickList.getMousePosition() != null) {
            boolean showMenu = checkShowNicklistMenu();
            if (!showMenu) {
                showMenu = selectNickUnderCursor();
            }
            if (showMenu) {
                if (e.isPopupTrigger()) {
                    final Point point = getMousePosition();
                    popuplateNicklistPopup();
                    nicklistPopup.show(this, (int) point.getX(), (int) point.getY());
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
        for (int i = 0; i < nickList.getModel().getSize(); i++) {
            if (nickList.getCellBounds(i, i) != null
                    && nickList.getMousePosition() != null
                    && nickList.getCellBounds(i, i).contains(nickList.getMousePosition())
                    && nickList.isSelectedIndex(i)) {
                showMenu = true;
                break;
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
        if (nickList.getMousePosition() != null) {
            for (int i = 0; i < nickList.getModel().getSize(); i++) {
                if (nickList.getCellBounds(i, i) != null 
                        && nickList.getMousePosition() != null
                        && nickList.getCellBounds(i, i).contains(nickList.getMousePosition())) {
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
        
        if ("ui".equals(domain)) {
            if ("nicklistbackgroundcolour".equals(key) || "backgroundcolour".equals(key)) {
                nickList.setBackground(getConfigManager().getOptionColour("ui", "nicklistbackgroundcolour",
                        getConfigManager().getOptionColour("ui", "backgroundcolour", Color.WHITE)));
            } else if ("nicklistforegroundcolour".equals(key) || "foregroundcolour".equals(key)) {
                nickList.setForeground(getConfigManager().getOptionColour("ui", "nicklistforegroundcolour",
                        getConfigManager().getOptionColour("ui", "foregroundcolour", Color.BLACK)));
            }
        } else if ("nicklist".equals(domain) && "altBackgroundColour".equals(key)) {
            nickList.repaint();
        }
    }
}
