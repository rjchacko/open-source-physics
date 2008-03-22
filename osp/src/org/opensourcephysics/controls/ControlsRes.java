/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.controls;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;


/**
 * ControlsRes provides access to internationalized string resources for OSPControls.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class ControlsRes {

  // static fields
  static final String BUNDLE_NAME = "org.opensourcephysics.resources.controls.controls_res";
  static ResourceBundle res = ResourceBundle.getBundle(BUNDLE_NAME, Locale.getDefault());
  // static constants for speed
  public static  String ANIMATION_NEW;
  public static  String ANIMATION_INIT;
  public static  String ANIMATION_STEP;
  public static  String ANIMATION_RESET;
  public static  String ANIMATION_START;
  public static  String ANIMATION_STOP;
  public static  String ANIMATION_RESET_TIP;
  public static  String ANIMATION_INIT_TIP;
  public static  String ANIMATION_START_TIP;
  public static  String ANIMATION_STOP_TIP;
  public static  String ANIMATION_NEW_TIP;
  public static  String ANIMATION_STEP_TIP;
  public static  String CALCULATION_CALC;
  public static  String CALCULATION_RESET;
  public static  String CALCULATION_CALC_TIP;
  public static  String CALCULATION_RESET_TIP;
  public static  String XML_NAME;
  public static  String XML_VALUE;
  private ControlsRes() {}

  private static String getString(final ResourceBundle bundle, final String key) {
    try {
      return bundle.getString(key);
    } catch(final MissingResourceException ex) {
      return '|'+key+'|';
    }
  }

  public static void setLocale(Locale locale){
     res = ResourceBundle.getBundle(BUNDLE_NAME, locale);
     setLocalStrings();
  }

  /**
   * Gets the localized value of a string. If no localized value is found, the
   * key is returned surrounded by exclamation points.
   *
   * @param key the string to localize
   * @return the localized string
   */
   static public String getString(String key) {
    try {
      return res.getString(key);
    } catch(MissingResourceException ex) {
      return "!"+key+"!"; //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

   /**
   * Gets the local strings.  Static strings are used for speed to avoid having to call the resource object.
   */
  private static void setLocalStrings(){
     ANIMATION_NEW = getString(res, "ANIMATION_NEW");
     ANIMATION_INIT = getString(res, "ANIMATION_INIT");
     ANIMATION_STEP = getString(res, "ANIMATION_STEP");
     ANIMATION_RESET = getString(res, "ANIMATION_RESET");
     ANIMATION_START = getString(res, "ANIMATION_START");
     ANIMATION_STOP = getString(res, "ANIMATION_STOP");
     ANIMATION_RESET_TIP = getString(res, "ANIMATION_RESET_TIP");
     ANIMATION_INIT_TIP = getString(res, "ANIMATION_INIT_TIP");
     ANIMATION_START_TIP = getString(res, "ANIMATION_START_TIP");
     ANIMATION_STOP_TIP = getString(res, "ANIMATION_STOP_TIP");
     ANIMATION_NEW_TIP = getString(res, "ANIMATION_NEW_TIP");
     ANIMATION_STEP_TIP = getString(res, "ANIMATION_STEP_TIP");
     CALCULATION_CALC = getString(res, "CALCULATION_CALC");
     CALCULATION_RESET = getString(res, "CALCULATION_RESET");
     CALCULATION_CALC_TIP = getString(res, "CALCULATION_CALC_TIP");
     CALCULATION_RESET_TIP = getString(res, "CALCULATION_RESET_TIP");
     XML_NAME = getString(res, "XML_NAME");
     XML_VALUE = getString(res, "XML_VALUE");
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
 *
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
