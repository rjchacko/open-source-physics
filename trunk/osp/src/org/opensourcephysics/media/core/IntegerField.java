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

import java.awt.*;
import javax.swing.*;

/**
 * This is a JTextField that accepts only integer numbers.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class IntegerField extends JTextField {

  // instance fields
  private NumberFormat format = NumberFormat.getNumberInstance();
  private int prevValue = 0;
  private Integer maxValue;
  private Integer minValue;

  /**
   * Constructs an IntegerField object.
   *
   * @param columns the number of columns available for text characters
   */
  public IntegerField(int columns) {
    super(columns);
    format.setParseIntegerOnly(true);
    setValue(prevValue);
  }

  /**
   * Gets the value from the text field.
   *
   * @return the value
   */
  public int getValue() {
    int retValue;
    try {
      retValue = format.parse(getText()).intValue();
      if (minValue != null && retValue < minValue.intValue()) {
        setValue(minValue.intValue());
        return minValue.intValue();
      }
      if (maxValue != null && retValue > maxValue.intValue()) {
        setValue(maxValue.intValue());
        return maxValue.intValue();
      }
    } catch (ParseException e) {
      Toolkit.getDefaultToolkit().beep();
      setValue(prevValue);
      return prevValue;
    }
    return retValue;
  }

  /**
   * Formats the specified value and enters it in the text field.
   *
   * @param value the value to be inserted
   */
  public void setValue(int value) {
    if (minValue != null)
      value = Math.max(value, minValue.intValue());
    if (maxValue != null)
      value = Math.min(value, maxValue.intValue());
    setText(format.format(value));
    prevValue = value;
  }

  /**
   * Sets a minimum value for this field.
   *
   * @param min the minimum allowed value
   */
  public void setMinValue(int min) {
    minValue = new Integer(min);
  }

  /**
   * Sets a maximum value for this field.
   *
   * @param max the maximum allowed value
   */
  public void setMaxValue(int max) {
    maxValue = new Integer(max);
  }

}
