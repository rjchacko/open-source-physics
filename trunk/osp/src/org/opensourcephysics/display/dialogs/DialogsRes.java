/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display.dialogs;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * String constants for Dialogs.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class DialogsRes {
  public static  String AUTOSCALE_AUTOSCALE;
  public static  String AUTOSCALE_AUTO;
  public static  String AUTOSCALE_OK;
  public static  String AUTOSCALE_ZOOM_WARNING;
  public static  String SCALE_SCALE;
  public static  String SCALE_MIN;
  public static  String SCALE_MAX;
  public static  String SCALE_AUTO;
  public static  String SCALE_CANCEL;
  public static  String SCALE_OK;
  public static  String LOG_SCALE;
  public static  String LOG_X;
  public static  String LOG_Y;
  public static  String LOG_OK;
  public static  String LOG_WARNING;

  // static fields
  static final String BUNDLE_NAME = "org.opensourcephysics.resources.display.dialogs_res";  //$NON-NLS-1$
  static Locale locale = Locale.getDefault();
  static ResourceBundle res = ResourceBundle.getBundle(BUNDLE_NAME, locale);


  private DialogsRes() {}

  /**
   * Gets a localized resource string.
   *
   * @param bundle ResourceBundle
   * @param key String
   * @return String
   */
  private static String getString(final ResourceBundle bundle, final String key) {
    try {
      return bundle.getString(key);
    } catch(final MissingResourceException ex) {
      // assert(false) : ex.getMessage();
      return '|'+key+'|';
    }
  }

  /**
   * Sets the locale.
   *
   * @param loc the locale
   */
  public static void setLocale(Locale loc){
     if (locale==loc){
        return;
     }
     locale = loc;
     // get the new resource bundle
     res = ResourceBundle.getBundle(BUNDLE_NAME, locale);
     setLocalStrings();
  }

  /**
  * Gets the local strings.
  *
  * Static strings are used for speed to avoid having to call the resource object.  This may no longer be necessary on fast machines.
  */
 private static void setLocalStrings(){
    AUTOSCALE_AUTOSCALE = getString(res, "AUTOSCALE_AUTOSCALE");
    AUTOSCALE_AUTO = getString(res, "AUTOSCALE_AUTO");
    AUTOSCALE_OK = getString(res, "AUTOSCALE_OK");
    AUTOSCALE_ZOOM_WARNING = getString(res, "AUTOSCALE_ZOOM_WARNING");
    SCALE_SCALE = getString(res, "SCALE_SCALE");
    SCALE_MIN = getString(res, "SCALE_MIN");
    SCALE_MAX = getString(res, "SCALE_MAX");
    SCALE_AUTO = getString(res, "SCALE_AUTO");
    SCALE_CANCEL = getString(res, "SCALE_CANCEL");
    SCALE_OK = getString(res, "SCALE_OK");
    LOG_SCALE = getString(res, "LOG_SCALE");
    LOG_X = getString(res, "LOG_X");
    LOG_Y = getString(res, "LOG_Y");
    LOG_OK = getString(res, "LOG_OK");
    LOG_WARNING = getString(res, "LOG_WARNING");
  }


  static {
    setLocalStrings();
  }
}

/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

 * Code that uses any portion of the code in the org.opensourcephysics package
 * or any subpackage (subdirectory) of this package must must also be be released
 * under the GNU GPL license.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston MA 02111-1307 USA
 * or view the license online at http://www.gnu.org/copyleft/gpl.html
 *
 * Copyright (c) 2007  The Open Source Physics project
 *                     http://www.opensourcephysics.org
 */
