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

package uk.org.ownage.dmdirc.ui.dialogs.actionseditor;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;
import uk.org.ownage.dmdirc.actions.ActionComparison;
import uk.org.ownage.dmdirc.actions.ActionComponent;
import uk.org.ownage.dmdirc.actions.ActionCondition;
import uk.org.ownage.dmdirc.actions.ActionManager;
import uk.org.ownage.dmdirc.actions.ActionType;

import uk.org.ownage.dmdirc.ui.MainFrame;
import uk.org.ownage.dmdirc.ui.components.StandardDialog;

import static uk.org.ownage.dmdirc.ui.UIUtilities.SMALL_BORDER;
import static uk.org.ownage.dmdirc.ui.UIUtilities.layoutGrid;

/**
 * Action conditions editing dialog, used in the actions editor dialog.
 */
public final class ConditionEditorDialog extends StandardDialog implements
        ActionListener {
    
    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 1;
    
    /** Parent dialog, informed of changes on close. */
    private ConditionsTabPanel parent;
    /** Parent action type trigger. */
    private ActionType trigger;
    /** conditions to be edited, or null if new. */
    private ActionCondition condition;
    /** Condition argument. */
    private int argument;
    /** Condition component. */
    private ActionComponent component;
    /** Condition comparison. */
    private ActionComparison comparison;
    /** Condition target. */
    private String target;
    
    /** Buttons panel. */
    private JPanel buttonsPanel;
    /** Parent conditions panel. */
    private JPanel conditionsPanel;
    /** Argument combobox. */
    private JComboBox arguments;
    /** Component combobox. */
    private JComboBox components;
    /** Comparison combobox. */
    private JComboBox comparisons;
    /** Target textfield. */
    private JTextField targetText;
    
    /**
     * Creates a new instance of ConditionEditorDialog.
     *
     * @param parent parent conditions panel.
     * @param action parent action
     * @param condition condition to be edited (or null)
     */
    public ConditionEditorDialog(final ConditionsTabPanel parent,
            final ActionType trigger, final ActionCondition condition) {
        super(MainFrame.getMainFrame(), false);
        
        this.trigger = trigger;
        this.parent = parent;
        this.condition = condition;
        if (condition == null) {
            this.argument = -1;
            this.component = null;
            this.comparison = null;
            this.target = null;
        } else {
            this.argument = condition.getArg();
            this.component = condition.getComponent();
            this.comparison = condition.getComparison();
            this.target = condition.getTarget();
        }
        
        this.setTitle("Condition Editor");
        
        this.setResizable(false);
        
        initComponents();
        addListeners();
        layoutComponents();
        
        this.setLocationRelativeTo(MainFrame.getMainFrame());
        
        this.setVisible(true);
    }
    
    /** Initialises the components. */
    private void initComponents() {
        initButtonsPanel();
        conditionsPanel = new JPanel();
        arguments = new JComboBox(new DefaultComboBoxModel());
        components = new JComboBox(new DefaultComboBoxModel());
        comparisons = new JComboBox(new DefaultComboBoxModel());
        targetText = new JTextField();
        
        arguments.setRenderer(new ActionCellRenderer());
        components.setRenderer(new ActionCellRenderer());
        comparisons.setRenderer(new ActionCellRenderer());
        
        arguments.setPreferredSize(new Dimension(300, arguments.getFont().getSize()));
        components.setPreferredSize(new Dimension(300, components.getFont().getSize()));
        comparisons.setPreferredSize(new Dimension(300, comparisons.getFont().getSize()));
        targetText.setPreferredSize(new Dimension(300, targetText.getFont().getSize()));
        
        components.setEnabled(false);
        comparisons.setEnabled(false);
        targetText.setEnabled(false);
        
        populateArguments();
    }
    
    private void populateArguments() {
        conditionsPanel.setVisible(false);
        
        ((DefaultComboBoxModel) arguments.getModel()).removeAllElements();
        
        for (String argument : trigger.getType().getArgNames()) {
            ((DefaultComboBoxModel) arguments.getModel()).addElement(argument);
        }
        
        if (argument == -1) {
            arguments.setSelectedIndex(-1);
            components.setEnabled(false);
            component = null;
            comparison = null;
            target = null;
            getOkButton().setEnabled(false);
        } else {
            arguments.setSelectedIndex(argument);
            components.setEnabled(true);
            components.setSelectedIndex(-1);
        }
        
        populateComponents();
    }
    
    private void populateComponents() {
        ((DefaultComboBoxModel) components.getModel()).removeAllElements();
        
        if (arguments.getSelectedItem() != null) {
            for (ActionComponent component : ActionManager.getCompatibleComponents(
                    trigger.getType().getArgTypes()[arguments.getSelectedIndex()]
                    )) {
                ((DefaultComboBoxModel) components.getModel()).addElement(component);
            }
        }
        
        if (component == null) {
            components.setSelectedIndex(-1);
            comparisons.setEnabled(false);
            comparison = null;
            target = null;
            getOkButton().setEnabled(false);
        } else {
            components.setSelectedItem(component);
            comparisons.setEnabled(true);
        }
        
        populateComparisons();
    }
    
    private void populateComparisons() {
        ((DefaultComboBoxModel) comparisons.getModel()).removeAllElements();
        
        if (arguments.getSelectedItem() != null) {
            for (ActionComparison comparison : ActionManager.getCompatibleComparisons(
                    arguments.getSelectedItem().getClass())) {
                ((DefaultComboBoxModel) comparisons.getModel()).addElement(comparison);
            }
        }
        
        if (comparison == null) {
            comparisons.setSelectedIndex(-1);
            targetText.setEnabled(false);
            target = null;
            getOkButton().setEnabled(false);
        } else {
            comparisons.setSelectedItem(comparison);
            targetText.setEnabled(true);
            getOkButton().setEnabled(true);
        }
        
        populateTarget();
    }
    
    private void populateTarget() {
        targetText.setText(target);
        
        conditionsPanel.setVisible(true);
    }
    
    /** Initialises the button panel. */
    private void initButtonsPanel() {
        orderButtons(new JButton(), new JButton());
        
        getOkButton().setEnabled(false);
        
        buttonsPanel = new JPanel();
        
        buttonsPanel.setBorder(BorderFactory.createEmptyBorder(0, SMALL_BORDER,
                SMALL_BORDER, SMALL_BORDER));
        
        buttonsPanel.setLayout(new BoxLayout(buttonsPanel, BoxLayout.LINE_AXIS));
        
        buttonsPanel.add(Box.createHorizontalGlue());
        buttonsPanel.add(getLeftButton());
        buttonsPanel.add(Box.createHorizontalStrut(SMALL_BORDER));
        buttonsPanel.add(getRightButton());
    }
    
    /** Adds listeners to the components. */
    private void addListeners() {
        getOkButton().addActionListener(this);
        getCancelButton().addActionListener(this);
        
        arguments.addActionListener(this);
        components.addActionListener(this);
        comparisons.addActionListener(this);
    }
    
    /** Lays out the components in the dialog. */
    private void layoutComponents() {
        layoutConditionsPanel();
        layoutButtonPanel();
        
        pack();
    }
    
    /** Lays out the conditions panel. */
    private void layoutConditionsPanel() {
        conditionsPanel.setLayout(new SpringLayout());
        
        conditionsPanel.add(new JLabel("Argument: "));
        conditionsPanel.add(arguments);
        conditionsPanel.add(new JLabel("Component: "));
        conditionsPanel.add(components);
        conditionsPanel.add(new JLabel("Comparison: "));
        conditionsPanel.add(comparisons);
        conditionsPanel.add(new JLabel("Target: "));
        conditionsPanel.add(targetText);
        
        layoutGrid(conditionsPanel, 4, 2, SMALL_BORDER, SMALL_BORDER,
                SMALL_BORDER, SMALL_BORDER);
    }
    
    /** Lays out the button panel. */
    private void layoutButtonPanel() {
        this.setLayout(new BorderLayout());
        
        this.add(conditionsPanel, BorderLayout.CENTER);
        this.add(buttonsPanel, BorderLayout.PAGE_END);
    }
    
    /** {@inheritDoc}. */
    public void actionPerformed(final ActionEvent event) {
        if (event.getSource() == arguments) {
            if (arguments.getSelectedItem() != null) {
                argument = arguments.getSelectedIndex();
            } else {
                argument = -1;
            }
            populateArguments();
        } else if (event.getSource() == components) {
            if (components.getSelectedItem() != null) {
                component = (ActionComponent) components.getSelectedItem();
            } else {
                component = null;
            }
            populateComponents();
        } else if (event.getSource() == comparisons) {
            if (comparisons.getSelectedItem() != null) {
                comparison = (ActionComparison) comparisons.getSelectedItem();
            } else {
                comparison = null;
            }
            populateComparisons();
        }
        if (event.getSource() == getOkButton()) {
            if (condition == null) {
                parent.addCondition(new ActionCondition(argument, component,
                        comparison, targetText.getText()));
            } else {
                condition.setArg(argument);
                condition.setComponent(component);
                condition.setComparison(comparison);
                condition.setTarget(targetText.getText());
                parent.doConditions();
            }
            this.dispose();
        } else if (event.getSource() == getCancelButton()) {
            this.dispose();
        }
    }
    
}
