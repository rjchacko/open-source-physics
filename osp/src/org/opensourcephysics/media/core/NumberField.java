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

import java.awt.*;
import java.text.*;
import javax.swing.*;

/**
 * This is a JTextField that accepts only numbers.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class NumberField extends JTextField {

    // instance fields
    protected NumberFormat format = NumberFormat.getInstance();
    protected double prevValue;
    protected Double maxValue;
    protected Double minValue;

    /**
     * Constructs a NumberField.
     *
     * @param columns the number of character columns
     */
    public NumberField(int columns) {
      super(columns);
    }

    /**
     * Gets the value from the text field.
     *
     * @return the value
     */
    public double getValue() {
      if (getText().equals(format.format(prevValue))) return prevValue;
      double retValue;
      try {
        retValue = format.parse(getText()).doubleValue();
        if (minValue != null && retValue < minValue.doubleValue()) {
          setValue(minValue.doubleValue());
          return minValue.doubleValue();
        }
        if (maxValue != null && retValue > maxValue.doubleValue()) {
          setValue(maxValue.doubleValue());
          return maxValue.doubleValue();
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
     * @param value the value to be entered
     */
    public void setValue(double value) {
      if (!isVisible()) return;
      if (minValue != null)
        value = Math.max(value, minValue.doubleValue());
      if (maxValue != null)
        value = Math.min(value, maxValue.doubleValue());
      setFormatFor(value);
      setText(format.format(value));
      prevValue = value;
    }

    /**
     * Sets the resolution for this number field.
     *
     * @param delta the change in value that must be resolvable
     */
    public void setResolution(double delta) {/** implemented in subclasses */}

    /**
     * Sets a minimum value for this field.
     *
     * @param min the minimum allowed value
     */
    public void setMinValue(double min) {
      minValue = new Double(min);
    }

    /**
     * Sets a maximum value for this field.
     *
     * @param max the maximum allowed value
     */
    public void setMaxValue(double max) {
      maxValue = new Double(max);
    }

    /**
     * Gets the format for this field.
     *
     * @return the format
     */
    public NumberFormat getFormat() {
      return format;
    }

    /**
     * Sets the format for a specified value. Subclasses may override this to
     * modify the format before displaying the value.
     *
     * @param value the value to be displayed
     */
    public void setFormatFor(double value) {/** implemented in subclasses */}

}
