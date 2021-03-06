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

package com.dmdirc.ui;

import com.dmdirc.CustomWindow;
import com.dmdirc.FrameContainer;
import com.dmdirc.Precondition;
import com.dmdirc.logger.ErrorLevel;
import com.dmdirc.logger.Logger;
import com.dmdirc.ui.interfaces.FrameListener;
import com.dmdirc.util.ListenerList;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * The WindowManager maintains a list of all open windows, and their
 * parent/child relations.
 */
public class WindowManager {

    /** Singleton instance of the WindowManager. */
    private static final WindowManager INSTANCE = new WindowManager();

    /** A list of root windows. */
    private final List<FrameContainer> rootWindows
            = new CopyOnWriteArrayList<FrameContainer>();

    /** A list of frame listeners. */
    private final ListenerList listeners
            = new ListenerList();

    /**
     * Registers a FrameListener with the WindowManager.
     *
     * @param frameListener The frame listener to be registered
     */
    @Precondition("The specified FrameListener is not null")
    public void addListener(final FrameListener frameListener) {
        Logger.assertTrue(frameListener != null);

        listeners.add(FrameListener.class, frameListener);
    }

    /**
     * Registers a FrameListener with the WindowManager, and then calls the
     * relevant methods on it for all existing windows.
     *
     * @param frameListener The frame listener to be registered
     * @since 0.6.6
     */
    @Precondition("The specified FrameListener is not null")
    public void addListenerAndSync(final FrameListener frameListener) {
        addListener(frameListener);

        for (FrameContainer root : rootWindows) {
            frameListener.addWindow(root, true);

            for (FrameContainer child : root.getChildren()) {
                fireAddWindow(frameListener, root, child);
            }
        }
    }

    /**
     * Recursively fires the addWindow callback for the specified windows and
     * listener.
     *
     * @param listener The listener to be fired
     * @param parent The parent window
     * @param child The new child window that was added
     *
     */
    private static void fireAddWindow(final FrameListener listener,
            final FrameContainer parent, final FrameContainer child) {
        listener.addWindow(parent, child, true);

        for (FrameContainer grandchild : child.getChildren()) {
            fireAddWindow(listener, child, grandchild);
        }
    }

    /**
     * Unregisters a FrameListener with the WindowManager.
     *
     * @param frameListener The frame listener to be removed
     */
    public void removeListener(final FrameListener frameListener) {
        listeners.remove(FrameListener.class, frameListener);
    }

    /**
     * Adds a new root window to the Window Manager.
     *
     * @param window The window to be added
     * @since 0.6.4
     */
    @Precondition({
        "The specified Window is not null",
        "The specified Window has not already been added"
    })
    public void addWindow(final FrameContainer window) {
        addWindow(window, true);
    }

    /**
     * Adds a new root window to the Window Manager.
     *
     * @param window The window to be added
     * @param focus Should this window become focused
     * @since 0.6.4
     */
    @Precondition({
        "The specified Window is not null",
        "The specified Window has not already been added"
    })
    public void addWindow(final FrameContainer window, final boolean focus) {
        Logger.assertTrue(window != null);
        Logger.assertTrue(!rootWindows.contains(window));

        rootWindows.add(window);

        fireAddWindow(window, focus);
    }

    /**
     * Adds a new child window to the Window Manager.
     *
     * @param parent The parent window
     * @param child The child window to be added
     * @since 0.6.4
     */
    @Precondition("The specified Windows are not null")
    public void addWindow(final FrameContainer parent,
            final FrameContainer child) {
        addWindow(parent, child, true);
    }

    /**
     * Adds a new child window to the Window Manager.
     *
     * @param parent The parent window
     * @param child The child window to be added
     * @param focus Should this window become focused
     * @since 0.6.4
     */
    @Precondition({
        "The specified containers are not null",
        "The specified parent is in the window hierarchy already",
        "The specified child is NOT in the window hierarchy already"
    })
    public void addWindow(final FrameContainer parent,
            final FrameContainer child, final boolean focus) {
        Logger.assertTrue(parent != null);
        Logger.assertTrue(isInHierarchy(parent));
        Logger.assertTrue(child != null);
        Logger.assertTrue(!isInHierarchy(child));

        parent.addChild(child);

        fireAddWindow(parent, child, focus);
    }

    /**
     * Recursively determines if the specified target is in the known
     * hierarchy of containers. That is, whether or not the specified target
     * or any of its parents are root windows.
     *
     * @since 0.6.4
     * @param target The container to be tested
     * @return True if the target is in the hierarchy, false otherise
     */
    protected boolean isInHierarchy(final FrameContainer target) {
        return target != null && (rootWindows.contains(target)
                || isInHierarchy(target.getParent()));
    }

    /**
     * Removes a window from the Window Manager. If the specified window
     * has child windows, they are recursively removed before the target window.
     * If the window hasn't previously been added, the request to remove it is
     * ignored.
     *
     * @param window The window to be removed
     * @since 0.6.4
     */
    @Precondition({
        "The specified window is not null",
        "The specified window is in the window hierarchy"
    })
    public void removeWindow(final FrameContainer window) {
        Logger.assertTrue(isInHierarchy(window));
        Logger.assertTrue(window != null);

        for (FrameContainer child : window.getChildren()) {
            child.close();
        }

        if (rootWindows.contains(window)) {
            fireDeleteWindow(window);
            rootWindows.remove(window);
        } else {
            final FrameContainer parent = window.getParent();
            fireDeleteWindow(parent, window);

            if (parent == null) {
                Logger.appError(ErrorLevel.MEDIUM, "Invalid window removed",
                        new IllegalArgumentException("Tried to remove a"
                        + " non-root window that has no known parent.\nWindow:"
                        + " " + window.getName()));
                return;
            } else {
                parent.removeChild(window);
            }
        }

        window.windowClosed();
    }

    /**
     * Removes a window from the Window Manager. If the specified window
     * has child windows, they are recursively removed before the target window.
     * If the window hasn't previously been added, the reques to remove it is
     * ignored.
     *
     * @param window The window to be removed
     * @param canWait Whether or not this method can wait for child windows
     * to be closed. If canWait is false, a new thread is created.
     * @deprecated The canWait parameter no longer has any effect. Call
     * {@link #removeWindow(com.dmdirc.FrameContainer)} instead.
     * @since 0.6.3
     */
    @Precondition({
        "The specified window is not null",
        "The specified window is in the window hierarchy"
    })
    @Deprecated
    public void removeWindow(final FrameContainer window, final boolean canWait) {
        removeWindow(window);
    }

    /**
     * Finds and returns a global custom window with the specified name.
     * If a custom window with the specified name isn't found, null is returned.
     *
     * @param name The name of the custom window to search for
     * @return The specified custom window, or null
     */
    @Precondition("The specified window name is not null")
    public FrameContainer findCustomWindow(final String name) {
        Logger.assertTrue(name != null);

        return findCustomWindow(rootWindows, name);
    }

    /**
     * Finds and returns a non-global custom window with the specified name.
     * If a custom window with the specified name isn't found, null is returned.
     *
     * @param parent The parent whose children should be searched
     * @param name The name of the custom window to search for
     * @return The specified custom window, or null
     */
    @Precondition({
        "The specified window name is not null",
        "The specified parent window is not null",
        "The specified parent window has been added to the Window Manager"
    })
    public FrameContainer findCustomWindow(final FrameContainer parent,
            final String name) {
        Logger.assertTrue(parent != null);
        Logger.assertTrue(name != null);

        return findCustomWindow(parent.getChildren(), name);
    }

    /**
     * Finds a custom window with the specified name among the specified list
     * of windows. If the custom window is not found, returns null.
     *
     * @param windows The list of windows to search
     * @param name The name of the custom window to search for
     * @return The custom window if found, or null otherwise
     */
    private FrameContainer findCustomWindow(final Collection<FrameContainer> windows,
            final String name) {
        for (FrameContainer window : windows) {
            if (window instanceof CustomWindow
                    && ((CustomWindow) window).getName().equals(name)) {
                return window;
            }
        }

        return null;
    }

    /**
     * Retrieves all known root (parent-less) windows.
     *
     * @since 0.6.4
     * @return A collection of all known root windows.
     */
    public Collection<FrameContainer> getRootWindows() {
        return Collections.unmodifiableCollection(rootWindows);
    }

    /**
     * Fires the addWindow(Window) callback.
     *
     * @param window The window that was added
     * @param focus Should this window become focused
     */
    private void fireAddWindow(final FrameContainer window, final boolean focus) {
        for (FrameListener listener : listeners.get(FrameListener.class)) {
            listener.addWindow(window, focus);
        }
    }

    /**
     * Fires the addWindow(Window, Window) callback.
     *
     * @param parent The parent window
     * @param child The new child window that was added
     * @param focus Should this window become focused
     *
     */
    private void fireAddWindow(final FrameContainer parent,
            final FrameContainer child, final boolean focus) {
        for (FrameListener listener : listeners.get(FrameListener.class)) {
            listener.addWindow(parent, child, focus);
        }
    }

    /**
     * Fires the delWindow(Window) callback.
     *
     * @param window The window that was removed
     */
    private void fireDeleteWindow(final FrameContainer window) {
        for (FrameListener listener : listeners.get(FrameListener.class)) {
            listener.delWindow(window);
        }
    }

    /**
     * Fires the delWindow(Window, Window) callback.
     *
     * @param parent The parent window
     * @param child The child window that was removed
     */
    private void fireDeleteWindow(final FrameContainer parent,
            final FrameContainer child) {
        for (FrameListener listener : listeners.get(FrameListener.class)) {
            listener.delWindow(parent, child);
        }
    }

    /**
     * Returns a singleton instance of the WindowManager.
     *
     * @return A singleton WindowManager instance
     */
    public static WindowManager getWindowManager() {
        return INSTANCE;
    }
}
