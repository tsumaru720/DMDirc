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

package com.dmdirc.addons.nowplaying;

import com.dmdirc.config.IdentityManager;
import com.dmdirc.config.prefs.PreferencesInterface;
import com.dmdirc.ui.swing.components.reorderablelist.ReorderableJList;
import static com.dmdirc.ui.swing.UIUtilities.SMALL_BORDER;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import net.miginfocom.swing.MigLayout;

/**
 * Now playing plugin config panel.
 */
public class ConfigPanel extends JPanel implements PreferencesInterface, KeyListener {

    /**
     * A version number for this class. It should be changed whenever the class
     * structure is changed (or anything else that would prevent serialized
     * objects being unserialized with the new class).
     */
    private static final long serialVersionUID = 1;

    /** Media source order list. */
    private ReorderableJList list;

    /** Media sources. */
    private final List<String> sources;

    /** The plugin that owns this panel. */
    private final NowPlayingPlugin plugin;
    
    /** Text field for our setting. */
    private JTextField textfield;
    
    /** Label for previews. */
    private JLabel preview;

    /** Creates a new instance of ConfigPanel.
     *
     * @param plugin The plugin that owns this panel
     * @param sources A list of sources to be used in the panel
     */
    public ConfigPanel(final NowPlayingPlugin plugin, final List<String> sources) {
        super();

        this.sources = new LinkedList<String>(sources);
        this.plugin = plugin;

        initComponents();
    }

    /** Initialises the components. */
    private void initComponents() {
        list = new ReorderableJList();

        for (String source : sources) {
            list.getModel().addElement(source);
        }
        
        textfield = new JTextField(IdentityManager.getGlobalConfig()
                .getOption(NowPlayingPlugin.DOMAIN, "format", "is playing $artist - $title"));
        textfield.addKeyListener(this);
        preview = new JLabel("Preview: * nick ");

        setLayout(new MigLayout("fillx, ins 0"));

        add(new JSeparator(), "span, split 3, width 10");
        add(new JLabel("Source order"));
        add(new JSeparator(), "growx, wrap");

        add(new JScrollPane(list), "gaptop 5, span, growx, wrap");

        add(new JSeparator(), "span, split 3, width 10");
        add(new JLabel("Output format"));
        add(new JSeparator(), "growx, wrap");
        
        add(textfield, "span, growx, gaptop 5, wrap");
        add(preview, "span, growx, wrap");

        add(new JSeparator(), "span, split 3, width 10");
        add(new JLabel("Substitutions"));
        add(new JSeparator(), "growx, wrap");        
        
        add(new JLabel("$app"), "width 25%!");
        add(new JLabel("$title"), "width 25%!");        
        add(new JLabel("$artist"), "width 25%!");
        add(new JLabel("$album"), "wrap");

        add(new JLabel("$bitrate"));
        add(new JLabel("$format"));        
        add(new JLabel("$length"));
        add(new JLabel("$time"));
        
        updatePreview();
    }
    
    private void updatePreview() {
        MediaSource source = plugin.getBestSource();
        
        if (source == null) {
            source = new DummyMediaSource();
        }
        
        preview.setText("Preview: * nick " + plugin.doSubstitution(textfield.getText(),
                source));
    }

    public List<String> getSources() {
        final List<String> newSources = new LinkedList<String>();

        final Enumeration<?> values = list.getModel().elements();

        while (values.hasMoreElements()) {
            newSources.add((String) values.nextElement());
        }

        return newSources;
    }

    /** {@inheritDoc} */
    @Override
    public void save() {
        plugin.saveSettings(getSources());
        IdentityManager.getConfigIdentity().setOption(NowPlayingPlugin.DOMAIN,
                "format", textfield.getText());
    }
    
    public void keyTyped(KeyEvent e) {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override    
    public void keyPressed(KeyEvent e) {
        // Do nothing
    }

    /** {@inheritDoc} */
    @Override    
    public void keyReleased(KeyEvent e) {
        updatePreview();
    }    
    
    /**
     * A dummy media source for use in previews.
     */
    private class DummyMediaSource implements MediaSource {

        /** {@inheritDoc} */
        @Override
        public boolean isRunning() {
            return true;
        }

        /** {@inheritDoc} */
        @Override        
        public boolean isPlaying() {
            return true;
        }

        /** {@inheritDoc} */
        @Override        
        public String getAppName() {
            return "MyProgram";
        }

        /** {@inheritDoc} */
        @Override        
        public String getArtist() {
            return "The Artist";
        }

        /** {@inheritDoc} */
        @Override        
        public String getTitle() {
            return "Song about nothing";
        }

        /** {@inheritDoc} */
        @Override        
        public String getAlbum() {
            return "Album 45";
        }

        /** {@inheritDoc} */
        @Override        
        public String getLength() {
            return "3:45";
        }

        /** {@inheritDoc} */
        @Override        
        public String getTime() {
            return "1:20";
        }

        /** {@inheritDoc} */
        @Override        
        public String getFormat() {
            return "flac";
        }

        /** {@inheritDoc} */
        @Override        
        public String getBitrate() {
            return "128";
        }
        
    }

}
