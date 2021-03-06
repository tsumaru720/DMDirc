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
package com.dmdirc.config.prefs;

import com.dmdirc.Main;
import com.dmdirc.actions.ActionManager;
import com.dmdirc.actions.CoreActionType;
import com.dmdirc.config.IdentityManager;
import com.dmdirc.interfaces.ActionListener;

import com.dmdirc.plugins.PluginManager;
import com.dmdirc.ui.interfaces.UIController;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PreferencesManagerTest {

    private static UIController controller;

    @BeforeClass
    public static void setUp() throws Exception {
        IdentityManager.load();
        Main.extractCorePlugins("ui_");
        PluginManager.getPluginManager();
        controller = mock(UIController.class);
    }

    @Test
    public void testDefaults() {
        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);
        assertNotNull(pm.getCategory("General"));
        assertNotNull(pm.getCategory("Connection"));
        assertNotNull(pm.getCategory("Messages"));
        assertNotNull(pm.getCategory("Advanced"));
        assertNotNull(pm.getCategory("GUI"));
        assertNotNull(pm.getCategory("Plugins"));
        assertNotNull(pm.getCategory("Updates"));
        assertNotNull(pm.getCategory("URL Handlers"));
    }

    @Test
    public void testDismiss() {
        final PreferencesCategory category = mock(PreferencesCategory.class);
        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);
        pm.addCategory(category);
        pm.dismiss();

        verify(category).dismiss();
    }

    @Test
    public void testSaveNoRestart() {
        final PreferencesCategory category = mock(PreferencesCategory.class);
        when(category.save()).thenReturn(false);

        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);
        pm.addCategory(category);
        assertFalse(pm.save());

        verify(category).save();
    }

    @Test
    public void testSaveRestart() {
        final PreferencesCategory category = mock(PreferencesCategory.class);
        when(category.save()).thenReturn(true);

        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);
        pm.addCategory(category);
        assertTrue(pm.save());

        verify(category).save();
    }

    @Test
    public void testGetCategory() {
        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);
        assertNull(pm.getCategory("unittest123"));
    }

    @Test
    public void testGetCategories() {
        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);
        assertNotNull(pm.getCategories());
        assertFalse(pm.getCategories().isEmpty());

        for (PreferencesCategory cat : pm.getCategories()) {
            assertNotNull(pm.getCategory(cat.getTitle()));
        }
    }

    @Test
    public void testSaveListener() {
        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);
        final PreferencesInterface tpi = mock(PreferencesInterface.class);

        pm.registerSaveListener(tpi);
        pm.fireSaveListeners();
        verify(tpi).save();
    }

    @Test
    public void testOpenAction() {
        final ActionListener tal = mock(ActionListener.class);

        ActionManager.getActionManager().initialise();
        ActionManager.getActionManager().registerListener(tal, CoreActionType.CLIENT_PREFS_OPENED);

        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);

        verify(tal).processEvent(eq(CoreActionType.CLIENT_PREFS_OPENED),
                (StringBuffer) same(null), same(pm));
    }

    @Test
    public void testCloseAction() {
        final ActionListener tal = mock(ActionListener.class);

        ActionManager.getActionManager().initialise();
        ActionManager.getActionManager().registerListener(tal, CoreActionType.CLIENT_PREFS_CLOSED);

        final PreferencesDialogModel pm = new PreferencesDialogModel(controller);
        pm.close();

        verify(tal).processEvent(eq(CoreActionType.CLIENT_PREFS_CLOSED),
                (StringBuffer) same(null));
    }

}