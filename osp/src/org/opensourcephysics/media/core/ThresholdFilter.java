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
import java.awt.event.*;
import java.awt.image.*;
import javax.swing.*;
import javax.swing.event.*;

import org.opensourcephysics.controls.*;

/**
 * This is a Filter that produces a b/w version of the source.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ThresholdFilter extends Filter {

  // instance fields
  private BufferedImage source, input, output;
  private int[] pixels;
  private int w, h;
  private Graphics2D gIn;
  private int threshold;
  private int defaultThreshold = 127;
  // inspector fields
  private Inspector inspector;
  private JLabel levelLabel;
  private IntegerField levelField;
  private JSlider levelSlider;


  /**
   * Constructs a default ThresholdFilter object.
   */
  public ThresholdFilter() {
    setThreshold(defaultThreshold);
  	hasInspector = true;
  }

  /**
   * Sets the threshold. Pixels brighter than the threshold become white,
   * those not brighter become black.
   *
   * @param threshold the threshold.
   */
  public void setThreshold(int threshold) {
    Integer prev = new Integer(this.threshold);
    this.threshold = threshold;
    support.firePropertyChange("threshold", prev, new Integer(threshold)); //$NON-NLS-1$
  }

  /**
   * Gets the threshold. Pixels brighter than the threshold become white,
   * those not brighter become black.
   *
   * @return the threshold.
   */
  public int getThreshold() {
    return threshold;
  }

  /**
   * Applies the filter to a source image and returns the result.
   *
   * @param sourceImage the source image
   * @return the filtered image
   */
  public BufferedImage getFilteredImage(BufferedImage sourceImage) {
    if (!isEnabled()) return sourceImage;
    if (sourceImage != source) initialize(sourceImage);
    if (sourceImage != input)
      gIn.drawImage(source, 0, 0, null);
    setOutputToThreshold(input);
    return output;
  }

  /**
   * Implements abstract Filter method.
   *
   * @return the inspector
   */
  public JDialog getInspector() {
  	if (inspector == null) inspector = new Inspector();
  	if (inspector.isModal() && vidPanel != null) {
  		Frame f = JOptionPane.getFrameForComponent(vidPanel);
    	if (frame != f) {
    		frame = f;
    		if (inspector != null) 
    			inspector.setVisible(false);
      	inspector = new Inspector();
    	}
    }
    inspector.initialize();
    return inspector;
  }

	/**
	 * Refreshes this filter's GUI
	 */
	public void refresh() {
		super.refresh();
    levelLabel.setText(MediaRes.getString("Filter.Threshold.Label.Threshold")); //$NON-NLS-1$
  	levelSlider.setToolTipText(MediaRes.getString("Filter.Threshold.ToolTip.Threshold")); //$NON-NLS-1$
		if (inspector != null) {
			inspector.setTitle(MediaRes.getString("Filter.Threshold.Title")); //$NON-NLS-1$
			inspector.pack();
		}
    boolean enabled = isEnabled();
    levelLabel.setEnabled(enabled);
    levelSlider.setEnabled(enabled);
    levelField.setEnabled(enabled);
	}

//_____________________________ private methods _______________________

/**
 * Creates the input and output images.
 *
 * @param image a new input image
 */
  private void initialize(BufferedImage image) {
    source = image;
    w = source.getWidth();
    h = source.getHeight();
    pixels = new int[w * h];
    output = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
    if (source.getType() == BufferedImage.TYPE_INT_RGB)
      input = source;
    else {
      input = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
      gIn = input.createGraphics();
    }
  }

  /**
   * Sets the output image pixels to black if the input pixel brightness
   * is below the threshold, or white if above.
   *
   * @param image the input image
   */
  private void setOutputToThreshold(BufferedImage image) {
    image.getRaster().getDataElements(0, 0, w, h, pixels);
    int pixel, r, g, b, v;
    int white = 0xffffff;
    for (int i = 0; i < pixels.length; i++) {
      pixel = pixels[i];
      r = (pixel >> 16) & 0xff;		// red
      g = (pixel >>  8) & 0xff;		// green
      b = (pixel      ) & 0xff;		// blue
      v = (r + g + b) / 3;				// brightness
      pixels[i] = v > threshold? white: 0;
    }
    output.getRaster().setDataElements(0, 0, w, h, pixels);
  }

  /**
   * Inner Inspector class to control filter parameters
   */
  private class Inspector extends JDialog {

    // instance fields
    int prev;

    /**
     * Constructs the Inspector.
     */
    public Inspector() {
      super(frame, !(frame instanceof org.opensourcephysics.display.OSPFrame));
      setTitle(MediaRes.getString("Filter.Threshold.Title")); //$NON-NLS-1$
      setResizable(false);
      createGUI();
      refresh();
      pack();
      // center on screen
      Rectangle rect = getBounds();
      Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
      int x = (dim.width - rect.width) / 2;
      int y = (dim.height - rect.height) / 2;
      setLocation(x, y);
    }

    /**
     * Creates the visible components.
     */
    void createGUI() {
      // create components
    	levelLabel = new JLabel();
      levelField = new IntegerField(3);
      levelField.setMaxValue(255);
      levelField.setMinValue(0);
      levelField.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          setThreshold(levelField.getValue());
          updateDisplay();
          levelField.selectAll();
        }
      });
      levelField.addFocusListener(new FocusListener() {
        public void focusGained(FocusEvent e) {
          levelField.selectAll();
        }

        public void focusLost(FocusEvent e) {
          setThreshold(levelField.getValue());
          updateDisplay();
        }
      });
      levelSlider = new JSlider(0, 0, 0);
      levelSlider.setMaximum(255);
      levelSlider.setMinimum(0);
      levelSlider.setBorder(BorderFactory.createEmptyBorder(0, 2, 0, 2));
      levelSlider.addChangeListener(new ChangeListener() {
        public void stateChanged(ChangeEvent e) {
          int i = levelSlider.getValue();
          if (i != getThreshold()) {
            setThreshold(i);
            updateDisplay();
          }
        }
      });
      // add components to content pane
      GridBagLayout gridbag = new GridBagLayout();
      JPanel panel = new JPanel(gridbag);
      setContentPane(panel);
      GridBagConstraints c = new GridBagConstraints();
      c.anchor = GridBagConstraints.EAST;
      c.fill = GridBagConstraints.NONE;
      c.weightx = 0.0;
      c.gridx = 0;
      c.insets = new Insets(5, 5, 0, 0);
      gridbag.setConstraints(levelLabel, c);
      panel.add(levelLabel);
      c.fill = GridBagConstraints.HORIZONTAL;
      c.gridx = 1;
      c.insets = new Insets(5, 0, 0, 0);
      gridbag.setConstraints(levelField, c);
      panel.add(levelField);
      c.gridx = 2;
      c.insets = new Insets(5, 0, 0, 0);
      c.weightx = 1.0;
      gridbag.setConstraints(levelSlider, c);
      panel.add(levelSlider);
      JPanel buttonbar = new JPanel(new FlowLayout());
      buttonbar.add(ableButton);
      buttonbar.add(closeButton);
      c.gridx = 2;
      c.gridy = 1;
      gridbag.setConstraints(buttonbar, c);
      panel.add(buttonbar);
    }

    /**
     * Initializes this inspector
     */
    void initialize() {
      prev = getThreshold();
      updateDisplay();
    }

    /**
     * Updates this inspector to reflect the current filter settings.
     */
    void updateDisplay() {
      levelField.setValue(getThreshold());
      levelSlider.setValue(getThreshold());
    }

    /**
     * Reverts to the previous filter settings.
     */
    void revert() {
      setThreshold(prev);
      updateDisplay();
    }

    /**
     * Resets parameters to their default settings.
     */
    void reset() {
      setThreshold(defaultThreshold);
      updateDisplay();
    }
  }

  /**
   * Returns an XML.ObjectLoader to save and load filter data.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new Loader();
  }

  /**
   * A class to save and load filter data.
   */
  static class Loader
      implements XML.ObjectLoader {

    /**
     * Saves data to an XMLControl.
     *
     * @param control the control to save to
     * @param obj the filter to save
     */
    public void saveObject(XMLControl control, Object obj) {
      ThresholdFilter filter = (ThresholdFilter) obj;
      control.setValue("threshold", filter.getThreshold()); //$NON-NLS-1$
      if (filter.frame != null && filter.inspector != null && 
      				filter.inspector.isVisible()) {
        int x = filter.inspector.getLocation().x - filter.frame.getLocation().x;
        int y = filter.inspector.getLocation().y - filter.frame.getLocation().y;
        control.setValue("inspector_x", x); //$NON-NLS-1$
        control.setValue("inspector_y", y); //$NON-NLS-1$
      }
    }

    /**
     * Creates a new filter.
     *
     * @param control the control
     * @return the new filter
     */
    public Object createObject(XMLControl control) {
      return new ThresholdFilter();
    }

    /**
     * Loads a filter with data from an XMLControl.
     *
     * @param control the control
     * @param obj the filter
     * @return the loaded object
     */
    public Object loadObject(XMLControl control, Object obj) {
      final ThresholdFilter filter = (ThresholdFilter) obj;
      if (control.getPropertyNames().contains("threshold")) { //$NON-NLS-1$
        filter.setThreshold(control.getInt("threshold")); //$NON-NLS-1$
      }
      filter.inspectorX = control.getInt("inspector_x"); //$NON-NLS-1$
      filter.inspectorY = control.getInt("inspector_y"); //$NON-NLS-1$
      return obj;
    }
  }
}
