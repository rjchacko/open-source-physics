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
 * This NumberField displays large and small numbers in scientific format.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ScientificField extends NumberField {

  private String[] patterns = new String[]
      {"0.0E0", "00.0E0", "000.0E0", "0000.0E0"}; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$ //$NON-NLS-4$
  private double[] ranges = new double[4];
  private boolean fixedPattern = true;
  private int currentPatternIndex = 0;
  private double zeroLimit = .0000000001;

  /**
   * Constructs a ScientificField with default resolution.
   *
   * @param columns the number of character columns
   */
  public ScientificField(int columns) {
    this(columns, 0.1);
  }

  /**
   * Constructs a ScientificField with specified resolution.
   *
   * @param columns the number of character columns
   * @param delta the change in value that must be resolvable
   */
  public ScientificField(int columns, double delta) {
    super(columns);
    setResolution(delta);
  }

  /**
   * Overrides NumberField setValue method.
   *
   * @param value the value to be entered
   */
  public void setValue(double value) {
    if (Math.abs(value) < zeroLimit) value = 0;
    super.setValue(value);
  }

  /**
   * Sets the resolution for this field.
   *
   * @param delta the change in value that must be resolvable
   */
  public void setResolution(double delta) {
    if (format instanceof DecimalFormat) {
//      if (delta >= .0001 && delta < 10) {
        fixedPattern = true;
//        if (delta < .001) ( (DecimalFormat) format).applyPattern("0.0000");
//        else if (delta < .01) ( (DecimalFormat) format).applyPattern("0.000");
//        else if (delta < .1) ( (DecimalFormat) format).applyPattern("0.00");
//        else if (delta < 1) ( (DecimalFormat) format).applyPattern("0.0");
//        else ( (DecimalFormat) format).applyPattern("0");
        ( (DecimalFormat) format).applyPattern("0.000E0"); //$NON-NLS-1$
//      }
//      else {
//        fixedPattern = false;
//        int power = Math.round((float)(Math.log(delta)/Math.log(10)));
//        for (int i = 0; i < ranges.length; i++) {
//          ranges[i] = Math.pow(10, power + i + 1);
//        }
//      }
    }
  }

  /**
   * Sets the format for a specified value.
   *
   * @param value the value to be displayed
   */
  public void setFormatFor(double value) {
    if (fixedPattern) return;
    value = Math.abs(value);
    for (int i = 0; i < ranges.length; i++) {
      if (value < ranges[i]) { // found the right range
        if (i != currentPatternIndex) {
          currentPatternIndex = i;
          if (format instanceof DecimalFormat) {
            ( (DecimalFormat) format).applyPattern(getPattern());
          }
        }
        return;
      }
    }
  }

  /**
   * Gets the pattern for this field.
   *
   * @return the current pattern
   */
  protected String getPattern() {
    return patterns[currentPatternIndex];
  }

}
