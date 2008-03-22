/*
 * The org.opensourcephysics.media.core package defines the Open Source Physics
 * media framework for working with video and other media.
 *
 * Copyright (c) 2004  Douglas Brown and Wolfgang Christian.
 *
 * This is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
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
 * For additional information and documentation on Open Source Physics,
 * please see <http://www.opensourcephysics.org/>.
 */
package org.opensourcephysics.media.core;

import java.text.*;

/**
 * This is a NumberField that displays numbers in decimal format with a fixed
 * number of decimal places.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class DecimalField extends NumberField {

  /**
   * Constructs a DecimalField.
   *
   * @param columns the number of character columns
   * @param places the number of decimal places to display
   */
  public DecimalField(int columns, int places) {
    super(columns);
    places = Math.min(places, 5);
    places = Math.max(places, 1);
    if (format instanceof DecimalFormat) {
      String pattern = "0."; //$NON-NLS-1$
      for (int i = 0; i < places; i++) {
        pattern += "0"; //$NON-NLS-1$
      }
      ((DecimalFormat)format).applyPattern(pattern);
    }
  }

  /**
   * Sets the resolution for this field.
   *
   * @param delta the change in value that must be resolvable
   */
  public void setResolution(double delta) {
    if (format instanceof DecimalFormat) {
      if (delta < .001)
        ( (DecimalFormat) format).applyPattern("0.0000"); //$NON-NLS-1$
      else if (delta < .01)
        ( (DecimalFormat) format).applyPattern("0.000"); //$NON-NLS-1$
      else if (delta < .1)
        ( (DecimalFormat) format).applyPattern("0.00"); //$NON-NLS-1$
      else if (delta < 1)
        ( (DecimalFormat) format).applyPattern("0.0"); //$NON-NLS-1$
      else
         ( (DecimalFormat) format).applyPattern("0"); //$NON-NLS-1$
    }
  }

}
