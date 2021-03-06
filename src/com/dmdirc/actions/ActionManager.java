/*
 * Copyright (c) 2006-2011 Chris Smith, Shane Mc Cormack, Gregory Holmes
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

package com.dmdirc.actions;

import com.dmdirc.Main;
import com.dmdirc.Precondition;
import com.dmdirc.actions.interfaces.ActionComparison;
import com.dmdirc.actions.interfaces.ActionComponent;
import com.dmdirc.actions.interfaces.ActionType;
import com.dmdirc.actions.internal.WhoisNumericFormatter;
import com.dmdirc.actions.wrappers.AliasWrapper;
import com.dmdirc.actions.wrappers.PerformWrapper;
import com.dmdirc.config.IdentityManager;
import com.dmdirc.interfaces.ActionListener;
import com.dmdirc.interfaces.ConfigChangeListener;
import com.dmdirc.logger.ErrorLevel;
import com.dmdirc.logger.Logger;
import com.dmdirc.updater.components.ActionGroupComponent;
import com.dmdirc.util.MapList;
import com.dmdirc.util.resourcemanager.ZipResourceManager;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages all actions for the client.
 */
public final class ActionManager {

    /** A singleton instance of the ActionManager. */
    private static final ActionManager INSTANCE = new ActionManager();

    /** A logger for the action manager to use. */
    private static final java.util.logging.Logger LOGGER
            = java.util.logging.Logger.getLogger(ActionManager.class.getName());

    /** A list of registered action types. */
    private final List<ActionType> types
            = new ArrayList<ActionType>();

    /** A list of registered action components. */
    private final List<ActionComponent> components
            = new ArrayList<ActionComponent>();

    /** A list of registered action comparisons. */
    private final List<ActionComparison> comparisons
            = new ArrayList<ActionComparison>();

    /** A map linking types and a list of actions that're registered for them. */
    private final MapList<ActionType, Action> actions
            = new MapList<ActionType, Action>();

    /** A map linking groups and a list of actions that're in them. */
    private final Map<String, ActionGroup> groups
            = new HashMap<String, ActionGroup>();

    /** A map of objects to synchronise on for concurrency groups. */
    private final Map<String, Object> locks
            = new HashMap<String, Object>();

    /** A map of the action type groups to the action types within. */
    private final MapList<String, ActionType> typeGroups
            = new MapList<String, ActionType>();

    /** The listeners that we have registered. */
    private final MapList<ActionType, ActionListener> listeners
            = new MapList<ActionType, ActionListener>();

    /** Indicates whether or not user actions should be killed (not processed). */
    private boolean killSwitch
            = IdentityManager.getGlobalConfig().getOptionBool("actions", "killswitch");

    /** Creates a new instance of ActionManager. */
    private ActionManager() {
        // Shouldn't be instansiated
    }

    /**
     * Returns a singleton instance of the Action Manager.
     *
     * @return A singleton ActionManager instance
     */
    public static ActionManager getActionManager() {
        return INSTANCE;
    }

    /**
     * Initialises the action manager.
     */
    public void initialise() {
        registerTypes(CoreActionType.values());
        registerComparisons(CoreActionComparison.values());
        registerComponents(CoreActionComponent.values());

        addGroup(AliasWrapper.getAliasWrapper());
        addGroup(PerformWrapper.getPerformWrapper());

        new WhoisNumericFormatter(IdentityManager.getAddonIdentity()).register();

        // Register a listener for the closing event, so we can save actions
        registerListener(new ActionListener() {
            /** {@inheritDoc} */
            @Override
            public void processEvent(final ActionType type, final StringBuffer format,
                    final Object... arguments) {
                saveAllActions();
            }
        }, CoreActionType.CLIENT_CLOSED);

        // Make sure we listen for the killswitch
        IdentityManager.getGlobalConfig().addChangeListener("actions", "killswitch",
                new ConfigChangeListener() {
            /** {@inheritDoc} */
            @Override
            public void configChanged(final String domain, final String key) {
                killSwitch = IdentityManager.getGlobalConfig().getOptionBool(
                        "actions", "killswitch");
            }
        });
    }

    /**
     * Saves all actions.
     */
    public void saveAllActions() {
        for (ActionGroup group : groups.values()) {
            for (Action action : group) {
                action.save();
            }
        }
    }

    /**
     * Registers the specified default setting for actions.
     *
     * @param name The name of the setting to be registered
     * @param value The default value for the setting
     */
    public void registerSetting(final String name, final String value) {
        IdentityManager.getAddonIdentity().setOption("actions", name, value);
    }

    /**
     * Registers the specified group of actions with the manager.
     *
     * @param group The group of actions to be registered
     */
    public void addGroup(final ActionGroup group) {
        groups.put(group.getName(), group);
    }

    /**
     * Registers a set of action types with the manager.
     *
     * @param newTypes An array of ActionTypes to be registered
     */
    @Precondition("None of the specified ActionTypes are null")
    public void registerTypes(final ActionType[] newTypes) {
        for (ActionType type : newTypes) {
            Logger.assertTrue(type != null);

            if (!types.contains(type)) {
                LOGGER.log(Level.FINEST, "Registering action type: {0}", type);
                types.add(type);
                typeGroups.add(type.getType().getGroup(), type);
            }
        }
    }

    /**
     * Registers a set of action components with the manager.
     *
     * @param comps An array of ActionComponents to be registered
     */
    @Precondition("None of the specified ActionComponents are null")
    public void registerComponents(final ActionComponent[] comps) {
        for (ActionComponent comp : comps) {
            Logger.assertTrue(comp != null);

            LOGGER.log(Level.FINEST, "Registering action component: {0}", comp);
            components.add(comp);
        }
    }

    /**
     * Registers a set of action comparisons with the manager.
     *
     * @param comps An array of ActionComparisons to be registered
     */
    @Precondition("None of the specified ActionComparisons are null")
    public void registerComparisons(final ActionComparison[] comps) {
        for (ActionComparison comp : comps) {
            Logger.assertTrue(comp != null);

            LOGGER.log(Level.FINEST, "Registering action comparison: {0}", comp);
            comparisons.add(comp);
        }
    }

    /**
     * Returns a map of groups to action lists.
     *
     * @return a map of groups to action lists
     */
    public Map<String, ActionGroup> getGroupsMap() {
        return groups;
    }

    /**
     * Returns a map of type groups to types.
     *
     * @return A map of type groups to types
     */
    public MapList<String, ActionType> getGroupedTypes() {
        return typeGroups;
    }

    /**
     * Loads actions from the user's directory.
     */
    public void loadUserActions() {
        actions.clear();

        for (ActionGroup group : groups.values()) {
            group.clear();
        }

        final File dir = new File(getDirectory());

        if (!dir.exists()) {
            try {
                dir.mkdirs();
                dir.createNewFile();
            } catch (IOException ex) {
                Logger.userError(ErrorLevel.HIGH, "I/O error when creating actions directory: "
                        + ex.getMessage());
            }
        }

        if (dir.listFiles() == null) {
            Logger.userError(ErrorLevel.MEDIUM, "Unable to load user action files");
        } else {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    loadActions(file);
                }
            }
        }

        registerComponents();
    }

    /**
     * Creates new ActionGroupComponents for each action group.
     */
    private void registerComponents() {
        for (ActionGroup group : groups.values()) {
            new ActionGroupComponent(group);
        }
    }

    /**
     * Loads action files from a specified group directory.
     *
     * @param dir The directory to scan.
     */
    @Precondition("The specified File is not null and represents a directory")
    private void loadActions(final File dir) {
        Logger.assertTrue(dir != null);
        Logger.assertTrue(dir.isDirectory());

        LOGGER.log(Level.FINER, "Loading actions from directory: {0}", dir.getAbsolutePath());

        if (!groups.containsKey(dir.getName())) {
            groups.put(dir.getName(), new ActionGroup(dir.getName()));
        }

        for (File file : dir.listFiles()) {
            new Action(dir.getName(), file.getName());
        }
    }

    /**
     * Registers an action with the manager.
     *
     * @param action The action to be registered
     */
    @Precondition("The specified action is not null")
    public void addAction(final Action action) {
        Logger.assertTrue(action != null);

        LOGGER.log(Level.FINE, "Registering action: {0}/{1} (status: {2})",
                new Object[] { action.getGroup(), action.getName(), action.getStatus() });

        if (action.getStatus() != ActionStatus.FAILED) {
            for (ActionType trigger : action.getTriggers()) {
                LOGGER.log(Level.FINER, "Action has trigger {0}", trigger);
                actions.add(trigger, action);
            }
        }

        getOrCreateGroup(action.getGroup()).add(action);
    }

    /**
     * Retrieves the action group with the specified name. A new group is
     * created if it doesn't already exist.
     *
     * @param name The name of the group to retrieve
     * @return The corresponding ActionGroup
     */
    public ActionGroup getOrCreateGroup(final String name) {
        if (!groups.containsKey(name)) {
            groups.put(name, new ActionGroup(name));
        }

        return groups.get(name);
    }

    /**
     * Unregisters an action with the manager.
     *
     * @param action The action to be unregistered
     */
    @Precondition("The specified action is not null")
    public void removeAction(final Action action) {
        Logger.assertTrue(action != null);

        actions.removeFromAll(action);
        getOrCreateGroup(action.getGroup()).remove(action);
    }

    /**
     * Reregisters the specified action. Should be used when the action's
     * triggers change.
     *
     * @param action The action to be reregistered
     */
    public void reregisterAction(final Action action) {
        removeAction(action);
        addAction(action);
    }

    /**
     * Processes an event of the specified type.
     *
     * @param type The type of the event to process
     * @param format The format of the message that's going to be displayed for
     * the event. Actions may change this format.
     * @param arguments The arguments for the event
     * @return True if the event should be processed, or false if an action
     * listener has requested the event be skipped.
     */
    @Precondition({
        "The specified ActionType is not null",
        "The specified ActionType has a valid ActionMetaType",
        "The length of the arguments array equals the arity of the ActionType's ActionMetaType"
    })
    public boolean triggerEvent(final ActionType type,
            final StringBuffer format, final Object ... arguments) {
        Logger.assertTrue(type != null);
        Logger.assertTrue(type.getType() != null);
        Logger.assertTrue(type.getType().getArity() == arguments.length);

        boolean res = false;

        if (listeners.containsKey(type)) {
            for (ActionListener listener
                    : new ArrayList<ActionListener>(listeners.get(type))) {
                try {
                    listener.processEvent(type, format, arguments);
                } catch (Exception e) {
                    Logger.appError(ErrorLevel.MEDIUM, "Error processing action: "
                            + e.getMessage(), e);
                }
            }
        }

        if (!killSwitch) {
            res |= triggerActions(type, format, arguments);
        }

        return !res;
    }

    /**
     * Triggers actions that respond to the specified type.
     *
     * @param type The type of the event to process
     * @param format The format of the message that's going to be displayed for
     * the event. Actions may change this format.
     * @param arguments The arguments for the event
     * @return True if the event should be skipped, or false if it can continue
     */
    @Precondition("The specified ActionType is not null")
    private boolean triggerActions(final ActionType type,
            final StringBuffer format, final Object ... arguments) {
        Logger.assertTrue(type != null);

        boolean res = false;

        if (actions.containsKey(type)) {
            for (Action action : new ArrayList<Action>(actions.get(type))) {
                try {
                    if (action.getConcurrencyGroup() == null) {
                        res |= action.trigger(format, arguments);
                    } else {
                        synchronized (locks) {
                            if (!locks.containsKey(action.getConcurrencyGroup())) {
                                locks.put(action.getConcurrencyGroup(), new Object());
                            }
                        }

                        synchronized (locks.get(action.getConcurrencyGroup())) {
                            res |= action.trigger(format, arguments);
                        }
                    }
                } catch (LinkageError e) {
                    Logger.appError(ErrorLevel.MEDIUM, "Error processing action: "
                            + e.getMessage(), e);
                } catch (Exception e) {
                    Logger.appError(ErrorLevel.MEDIUM, "Error processing action: "
                            + e.getMessage(), e);
                }
            }
        }

        return res;
    }

    /**
     * Returns the directory that should be used to store actions.
     *
     * @return The directory that should be used to store actions
     */
    public static String getDirectory() {
        return Main.getConfigDir() + "actions" + System.getProperty("file.separator");
    }

    /**
     * Creates a new group with the specified name.
     *
     * @param group The group to be created
     *
     * @return The newly created group
     */
    @Precondition({
        "The specified group is non-null and not empty",
        "The specified group is not an existing group"
    })
    public ActionGroup createGroup(final String group) {
        Logger.assertTrue(group != null);
        Logger.assertTrue(!group.isEmpty());
        Logger.assertTrue(!groups.containsKey(group));

        final File file = new File(getDirectory() + group);
        if (file.isDirectory() || file.mkdir()) {
            final ActionGroup actionGroup = new ActionGroup(group);
            groups.put(group, actionGroup);
            return actionGroup;
        } else {
            throw new IllegalArgumentException("Unable to create action group directory"
                    + "\n\nDir: " + getDirectory() + group);
        }
    }

    /**
     * Removes the group with the specified name.
     *
     * @param group The group to be removed
     */
    @Precondition({
        "The specified group is non-null and not empty",
        "The specified group is an existing group"
    })
    public void deleteGroup(final String group) {
        Logger.assertTrue(group != null);
        Logger.assertTrue(!group.isEmpty());
        Logger.assertTrue(groups.containsKey(group));

        for (Action action : groups.get(group).getActions()) {
            removeAction(action);
        }

        final File dir = new File(getDirectory() + group);

        if (dir.isDirectory()) {
            for (File file : dir.listFiles()) {
                if (!file.delete()) {
                    Logger.userError(ErrorLevel.MEDIUM, "Unable to remove file: "
                            + file.getAbsolutePath());
                    return;
                }
            }
        }

        if (!dir.delete()) {
            Logger.userError(ErrorLevel.MEDIUM, "Unable to remove directory: "
                    + dir.getAbsolutePath());
            return;
        }

        groups.remove(group);
    }

    /**
     * Renames the specified group.
     *
     * @param oldName The old name of the group
     * @param newName The new name of the group
     */
    @Precondition({
        "The old name is non-null and not empty",
        "The old name is an existing group",
        "The new name is non-null and not empty",
        "The new name is not an existing group",
        "The old name does not equal the new name"
    })
    public void changeGroupName(final String oldName, final String newName) {
        Logger.assertTrue(oldName != null);
        Logger.assertTrue(!oldName.isEmpty());
        Logger.assertTrue(newName != null);
        Logger.assertTrue(!newName.isEmpty());
        Logger.assertTrue(groups.containsKey(oldName));
        Logger.assertTrue(!groups.containsKey(newName));
        Logger.assertTrue(!newName.equals(oldName));

        createGroup(newName);

        for (Action action : groups.get(oldName).getActions()) {
            action.setGroup(newName);
            getOrCreateGroup(oldName).remove(action);
            getOrCreateGroup(newName).add(action);
        }

        deleteGroup(oldName);
    }

    /**
     * Returns the action comparison specified by the given string, or null if it
     * doesn't match a valid registered action comparison.
     *
     * @param type The name of the action comparison to try and find
     * @return The type with the specified name, or null on failure
     */
    public ActionType getType(final String type) {
        if (type == null || type.isEmpty()) {
            return null;
        }

        for (ActionType target : types) {
            if (target.name().equals(type)) {
                return target;
            }
        }

        return null;
    }

    /**
     * Returns a list of action types that are compatible with the one
     * specified.
     *
     * @param type The type to be checked against
     * @return A list of compatible action types
     */
    @Precondition("The specified type is not null")
    public List<ActionType> findCompatibleTypes(final ActionType type) {
        Logger.assertTrue(type != null);

        final List<ActionType> res = new ArrayList<ActionType>();
        for (ActionType target : types) {
            if (!target.equals(type) && target.getType().equals(type.getType())) {
                res.add(target);
            }
        }

        return res;
    }

    /**
     * Returns a list of action components that are compatible with the
     * specified class.
     *
     * @param target The class to be tested
     * @return A list of compatible action components
     */
    @Precondition("The specified target is not null")
    public List<ActionComponent> findCompatibleComponents(final Class<?> target) {
        Logger.assertTrue(target != null);

        final List<ActionComponent> res = new ArrayList<ActionComponent>();
        for (ActionComponent subject : components) {
            if (subject.appliesTo().equals(target)) {
                res.add(subject);
            }
        }

        return res;
    }

    /**
     * Returns a list of action comparisons that are compatible with the
     * specified class.
     *
     * @param target The class to be tested
     * @return A list of compatible action comparisons
     */
    @Precondition("The specified target is not null")
    public List<ActionComparison> findCompatibleComparisons(final Class<?> target) {
        Logger.assertTrue(target != null);

        final List<ActionComparison> res = new ArrayList<ActionComparison>();
        for (ActionComparison subject : comparisons) {
            if (subject.appliesTo().equals(target)) {
                res.add(subject);
            }
        }

        return res;
    }

    /**
     * Returns a list of all the action types registered by this manager.
     *
     * @return A list of registered action types
     */
    public List<ActionType> getAllTypes() {
        return types;
    }

    /**
     * Returns a list of all the action types registered by this manager.
     *
     * @return A list of registered action comparisons
     */
    public List<ActionComparison> getAllComparisons() {
        return comparisons;
    }

    /**
     * Returns the action component specified by the given string, or null if it
     * doesn't match a valid registered action component.
     *
     * @param type The name of the action component to try and find
     * @return The actioncomponent with the specified name, or null on failure
     */
    @Precondition("The specified type is non-null and not empty")
    public ActionComponent getComponent(final String type) {
        Logger.assertTrue(type != null);
        Logger.assertTrue(!type.isEmpty());

        for (ActionComponent target : components) {
            if (target.name().equals(type)) {
                return target;
            }
        }

        return null;
    }

    /**
     * Returns the action type specified by the given string, or null if it
     * doesn't match a valid registered action type.
     *
     * @param type The name of the action type to try and find
     * @return The actiontype with the specified name, or null on failure
     */
    @Precondition("The specified type is non-null and not empty")
    public ActionComparison getComparison(final String type) {
        Logger.assertTrue(type != null);
        Logger.assertTrue(!type.isEmpty());

        for (ActionComparison target : comparisons) {
            if (target.name().equals(type)) {
                return target;
            }
        }

        return null;
    }

    /**
     * Installs an action pack located at the specified path.
     *
     * @param path The full path of the action pack .zip.
     * @throws IOException If the zip cannot be extracted
     */
    public static void installActionPack(final String path) throws IOException {
        final ZipResourceManager ziprm = ZipResourceManager.getInstance(path);

        ziprm.extractResources("", getDirectory());

        getActionManager().loadUserActions();

        new File(path).delete();
    }

    /**
     * Adds a new listener for the specified action type.
     *
     * @param types The action types that are to be listened for
     * @param listener The listener to be added
     */
    public void registerListener(final ActionListener listener, final ActionType ... types) {
        for (ActionType type : types) {
            listeners.add(type, listener);
        }
    }

    /**
     * Removes a listener for the specified action type.
     *
     * @param types The action types that were being listened for
     * @param listener The listener to be removed
     */
    public void unregisterListener(final ActionListener listener, final ActionType ... types) {
        for (ActionType type : types) {
            listeners.remove(type, listener);
        }
    }

    /**
     * Removes a listener for all action types.
     *
     * @param listener The listener to be removed
     */
    public void unregisterListener(final ActionListener listener) {
        listeners.removeFromAll(listener);
    }
}
