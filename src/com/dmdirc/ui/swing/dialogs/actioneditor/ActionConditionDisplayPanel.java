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

package com.dmdirc.ui.swing.dialogs.actioneditor;

import com.dmdirc.actions.ActionCondition;
import com.dmdirc.actions.interfaces.ActionType;
import com.dmdirc.ui.IconManager;
import com.dmdirc.ui.swing.components.ImageButton;
import com.dmdirc.ui.swing.components.ImageToggleButton;
import com.dmdirc.ui.swing.components.TextLabel;
import com.dmdirc.util.ListenerList;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

import net.miginfocom.swing.MigLayout;

/**
 * Action condition display panel.
 */
public class ActionConditionDisplayPanel extends JPanel implements ActionListener {

    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 1;
    /** Info label. */
    private TextLabel label;
    /** Edit button. */
    private JToggleButton editButton;
    /** Delete button. */
    private ImageButton deleteButton;
    /** Edit panel. */
    private ActionConditionEditorPanel editPanel;
    /** Listeners. */
    private ListenerList listeners;
    /** Action condition. */
    private ActionCondition condition;
    /** Action trigger. */
    private ActionType trigger;

    /** 
     * Instantiates the panel.
     * 
     * @param condition Action condition
     * @param trigger Action trigger
     */
    public ActionConditionDisplayPanel(final ActionCondition condition,
            final ActionType trigger) {
        super();

        initComponents();
        setCondition(trigger, condition);
        addListeners();
        layoutComponents();
    }

    /** Initialises the components. */
    private void initComponents() {
        label = new TextLabel();
        editButton = new ImageToggleButton("edit", IconManager.getIconManager().
                getIcon("edit-inactive"),
                IconManager.getIconManager().getIcon("edit-active"));
        deleteButton = new ImageButton("delete", IconManager.getIconManager().
                getIcon("close-inactive"),
                IconManager.getIconManager().getIcon("close-active"));

        editPanel = new ActionConditionEditorPanel();
        listeners = new ListenerList();
    }

    /** Adds the listeners. */
    private void addListeners() {
    }

    /** Lays out the components. */
    private void layoutComponents() {
        setLayout(new MigLayout("fill, pack, hidemode 3"));
        add(label, "growx");
        add(editButton, "right");
        add(deleteButton, "right, wrap");
        add(editPanel, "grow");
    }

    /** 
     * {@inheritDoc}
     * 
     * @param e Action event
     */
    @Override
    public void actionPerformed(final ActionEvent e) {
        if (e.getSource().equals(deleteButton)) {
            fireConditionRemoved(this);
        } else if (e.getSource().equals(editButton)) {
            editPanel.setVisible(editButton.getModel().isSelected());
        }
    }

    /**
     * Adds an ActionConditionRemovalListener to the listener list.
     *
     * @param listener Listener to add
     */
    public void addConditionListener(final ActionConditionRemovalListener listener) {
        if (listener == null) {
            return;
        }

        listeners.add(ActionConditionRemovalListener.class, listener);
    }

    /**
     * Removes an ActionConditionRemovalListener from the listener list.
     *
     * @param listener Listener to remove
     */
    public void removeConditionListener(final ActionConditionRemovalListener listener) {
        listeners.remove(ActionConditionRemovalListener.class, listener);
    }

    /**
     * Fired when the an action condition is removed.
     *
     * @param condition Removed condition
     */
    protected void fireConditionRemoved(final ActionConditionDisplayPanel condition) {
        for (ActionConditionRemovalListener listener : listeners.get(ActionConditionRemovalListener.class)) {
            listener.conditionRemoved(condition);
        }
    }

    /** {@inheritDoc} */
    @Override
    public void setEnabled(final boolean enabled) {
        editPanel.setEnabled(enabled);
        editButton.setEnabled(enabled);
        deleteButton.setEnabled(enabled);
    }

    /**
     * Sets the new action condition.
     * 
     * @param trigger Action trigger
     * @param condition Action condition
     */
    public void setCondition(final ActionType trigger,
            final ActionCondition condition) {
        this.trigger = trigger;
        this.condition = condition;

        label.setText("The " +
                trigger.getType().getArgNames()[condition.getArg()] + "'s " + condition.getComponent().
                getName() + " " + condition.getComparison().getName() + " '" +
                condition.getTarget() + "'");
    }
    
    /**
     * Returns the action condition represented by this panel.
     * 
     * @return Action condition
     */
    public ActionCondition getCondition() {
        return condition;
    }
}
