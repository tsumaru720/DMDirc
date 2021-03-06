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

import com.dmdirc.config.IdentityManager;
import com.dmdirc.interfaces.ConfigChangeListener;
import com.dmdirc.util.URLBuilder;

import java.awt.Image;
import java.awt.Toolkit;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * The icon manager provides a standard way to access icons for use in DMDirc.
 * It allows the user to override the default actions using config settings
 * under the icons domain.
 */
public final class IconManager implements ConfigChangeListener {

    /** Previously created IconManager instance. */
    private static final IconManager ME = new IconManager();

    /** A map of existing icons. */
    private final Map<String, Icon> icons;

    /** A map of existing images. */
    private final Map<String, Image> images;

    /** Creates a new instance of IconManager. */
    private IconManager() {
        icons = new HashMap<String, Icon>();
        images = new HashMap<String, Image>();

        IdentityManager.getGlobalConfig().addChangeListener("icon", this);
    }

    /**
     * Returns an instance of IconManager.
     *
     * @return Instance of IconManager
     */
    public static IconManager getIconManager() {
        return ME;
    }

    /**
     * Retrieves the icon with the specified type. Returns null if the icon
     * wasn't found.
     *
     * @param type The name of the icon type to retrieve
     *
     * @return The icon that should be used for the specified type
     */
    public Icon getIcon(final String type) {
        if (icons.containsKey(type)) {
            return icons.get(type);
        }
        if (!icons.containsKey(type)) {
            final URL iconURL = getIconURL(type);
            final Image iconImage = Toolkit.getDefaultToolkit().getImage(iconURL);
            final Image scaledIconImage = getScaledImage(iconImage, 16, 16);
            icons.put(type, new ImageIcon(scaledIconImage));
        }
        return icons.get(type);
    }

    /**
     * Retrieves the icon with the specified type. Returns null if the icon
     * wasn't found.
     *
     * @param type The name of the icon type to retrieve
     * @param width width of the image
     * @param height height of the image
     *
     * @return The icon that should be used for the specified type
     *
     * @since 0.6.3m1
     */
    public Icon getScaledIcon(final String type, final int width, final int height) {
        return new ImageIcon(getScaledImage(new ImageIcon(getIconURL(type)).
                getImage(), width, height));
    }

    /**
     * Retrieves the image with the specified type. Returns null if the icon
     * wasn't found.
     *
     * @param type The name of the icon type to retrieve
     *
     * @return The image that should be used for the specified type
     */
    public Image getImage(final String type) {
        if (!images.containsKey(type)) {
            images.put(type, Toolkit.getDefaultToolkit().createImage(getIconURL(type)));
        }
        return images.get(type);
    }

    /**
     * Returns a scaled image.
     *
     * @param image Image to scale
     * @param width Width of resulting image
     * @param height Height of resulting image
     *
     * @return Scaled Image
     */
    private Image getScaledImage(final Image image,
            final int width, final int height) {
        return image.getScaledInstance(width , height, Image.SCALE_SMOOTH);
    }

    /**
     * Retrieves the URL of a specified icon type.
     *
     * @param type The name of the icon type to retrieve
     *
     * @return The URL that should be used to retrieve the specified icon
     */
    private URL getIconURL(final String type) {
        final String iconType = getSpecialIcons(type);
        final ClassLoader cldr = Thread.currentThread().getContextClassLoader();
        final URL defaultURL = cldr.getResource("com/dmdirc/res/" + iconType + ".png");

        //Get the path for the url
        final String path = IdentityManager.getGlobalConfig().hasOptionString("icon", iconType)
                ? IdentityManager.getGlobalConfig().getOption("icon", iconType)
                : "dmdirc://com/dmdirc/res/" + iconType + ".png";

        //Get the url for the speficied path
        URL imageURL = URLBuilder.buildURL(path);

        if (imageURL == null && defaultURL != null) {
           imageURL = defaultURL;
        }

        if (imageURL == null && defaultURL == null) {
            imageURL = cldr.getResource("com/dmdirc/res/icon.png");
        }

        //Check URL points to a valid location
        try {
            imageURL.openConnection().connect();
        } catch (IOException ex) {
            imageURL = cldr.getResource("com/dmdirc/res/icon.png");
        }

        if (imageURL == null) {
            throw new IllegalArgumentException("Unable to load icon type '"
                    + iconType + "', and unable to load default");
        }

        return imageURL;
    }

    private String getSpecialIcons(final String type) {
        final Calendar cal = new GregorianCalendar();
        cal.setTime(new Date());
        if ((cal.get(Calendar.MONTH) == Calendar.DECEMBER
                && cal.get(Calendar.DAY_OF_MONTH) >= 12
                && cal.get(Calendar.DAY_OF_MONTH) <= 31)
                && ("icon".equals(type) || "logo".equals(type))) {
            return "logo-special";
        }
        return type;
    }

    /** {@inheritDoc} */
    @Override
    public void configChanged(final String domain, final String key) {
        if ("icon".equals(domain)) {
            if (images.containsKey(key)) {
                images.remove(key);
            }
            if (icons.containsKey(key)) {
                icons.remove(key);
            }
        }
    }

}
