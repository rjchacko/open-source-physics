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

import java.io.*;
import java.util.*;

import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.tools.*;

/**
 * This is a Video assembled from one or more still images.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ImageVideo extends VideoAdapter {

  // instance fields
  protected Component observer = new JPanel();    // image observer
  protected Image[] images = new Image[0];        // image array
  protected ArrayList paths = new ArrayList();    // relative image paths
  protected ArrayList newPaths = new ArrayList(); // newly loaded paths
  protected Dimension dim = new Dimension(0, 0); // max image dimension

  /**
   * Creates an ImageVideo and loads a named image or image sequence.
   *
   * @param imageName the name of the image file
   * @throws IOException
   */
  public ImageVideo(String imageName) throws IOException {
    append(imageName);
  }

  /**
   * Creates an ImageVideo and loads a named image or image sequence.
   *
   * @param imageName the name of the image file
   * @param sequence true to automatically load image sequence, if any
   * @throws IOException
   */
  public ImageVideo(String imageName, boolean sequence) throws IOException {
    append(imageName, sequence);
  }

  /**
   * Creates an ImageVideo from an image.
   *
   * @param image the image
   */
  public ImageVideo(Image image) {
    if (image != null) insert(new Image[] {image}, 0);
  }

  /**
   * Overrides VideoAdapter setFrameNumber method.
   *
   * @param n the desired frame number
   */
  public void setFrameNumber(int n) {
    super.setFrameNumber(n);
    int index = Math.min(getFrameNumber(), images.length-1);
    rawImage = images[index];
    isValidImage = false;
    isValidFilteredImage = false;
    firePropertyChange("framenumber", null, new Integer(getFrameNumber())); //$NON-NLS-1$
  }

  /**
   * Gets the image array.
   *
   * @return the image array
   */
  public Image[] getImages() {
    return images;
  }

  /**
   * Appends the named image or image sequence to the end of this video.
   * This method will ask user whether to load sequences, if any.
   *
   * @param imageName the image name
   * @throws IOException
   */
  public void append(String imageName) throws IOException {
  	insert(imageName, images.length);
  }

  /**
   * Appends the named image or image sequence to the end of this video.
   *
   * @param imageName the image name
   * @param sequence true to automatically load image sequence, if any
   * @throws IOException
   */
  public void append(String imageName, boolean sequence) throws IOException {
  	insert(imageName, images.length, sequence);
  }

  /**
   * Inserts the named image or image sequence at the specified index.
   * This method will ask user whether to load sequences, if any.
   *
   * @param imageName the image name
   * @param index the index
   * @throws IOException
   */
  public void insert(String imageName, int index) throws IOException {
    Image[] imageArray = loadImages(imageName,
                                    true,  // ask user for confirmation
                                    true); // allow sequences, if any
    if (imageArray.length > 0) {
      insert(imageArray, index);
    }
  }

  /**
   * Inserts the named image or image sequence at the specified index.
   *
   * @param imageName the image name
   * @param index the index
   * @param sequence true to automatically load image sequence, if any
   * @throws IOException
   */
  public void insert(String imageName, int index, boolean sequence) throws IOException {
    Image[] imageArray = loadImages(imageName,
                                    false, // don't ask user for confirmation
                                    sequence);
    if (imageArray.length > 0) {
      insert(imageArray, index);
    }
  }

  /**
   * Inserts an image at the specified index.
   *
   * @param image the image
   * @param index the index
   */
  public void insert(Image image, int index) {
  	if (image == null) return;
    insert(new Image[] {image}, index);
  }

  /**
   * Removes the image at the specified index.
   *
   * @param index the index
   * @return the path of the image, or null if none removed or path unknown
   */
  public String remove(int index) {
    int len = images.length;
    if (len == 1 || len <= index) return null; // don't remove the only image
    Image[] newArray = new Image[len-1];
    System.arraycopy(images, 0, newArray, 0, index);
    System.arraycopy(images, index+1, newArray, index, len-1-index);
    images = newArray;
    if (index < len-1) rawImage = images[index];
    else rawImage = images[index-1];
    frameCount = images.length;
    endFrameNumber = frameCount - 1;
    Dimension newDim = getDimension();
    if (newDim.height != dim.height || newDim.width != dim.width) {
    	this.firePropertyChange("size", dim, newDim); //$NON-NLS-1$
    	dim = newDim;
    }
    if (paths.size() <= index) return null;
    return (String)paths.remove(index);
  }

  public Dimension getDimension() {
  	int w = images[0].getWidth(observer);  	
  	int h = images[0].getHeight(observer);  	
  	for (int i = 1; i < images.length; i++) {
  		w = Math.max(w, images[i].getWidth(observer));
  		h = Math.max(h, images[i].getHeight(observer));
  	}
  	return new Dimension(w, h);
  }
  
  /**
   * Called by the garbage collector when this video is no longer in use.
   */
  protected void finalize() {
//    System.out.println("imageVideo garbage"); //$NON-NLS-1$
  }

//_______________________ private/protected methods ____________________________

  /**
   * Loads an image or image sequence specified by name.
   *
   * @param imagePath the image path
   * @param alwaysAsk true to always ask for sequence confirmation
   * @param sequence true to automatically load sequences (if not alwaysAsk)
   * @return an array of loaded images
   * @throws IOException
   */
  private Image[] loadImages(String imagePath,
                             boolean alwaysAsk,
                             boolean sequence) throws IOException {
  	newPaths.clear();
    Resource res = ResourceLoader.getResource(imagePath);
    if (res == null)
      throw new IOException("Image " + imagePath + " not found"); //$NON-NLS-1$ //$NON-NLS-2$
    Image image = res.getImage();
    if (image == null)
      throw new IOException("\"" + imagePath + "\" is not an image"); //$NON-NLS-1$ //$NON-NLS-2$
    if (getProperty("name") == null) { //$NON-NLS-1$
      setProperty("name", XML.getRelativePath(imagePath)); //$NON-NLS-1$
      setProperty("path", imagePath); //$NON-NLS-1$
      setProperty("absolutepath", res.getAbsolutePath()); //$NON-NLS-1$
    }
    newPaths.add(XML.getRelativePath(imagePath));
    if (!alwaysAsk && !sequence) {
      return new Image[] {image};
    }
    // look for image sequence (numbered image names)
    String extension = ""; //$NON-NLS-1$
    int i = imagePath.lastIndexOf('.');
    if (i > 0 && i < imagePath.length() - 1) {
      extension = imagePath.substring(i).toLowerCase();
      imagePath = imagePath.substring(0, i); // now free of extension
    }
    int len = imagePath.length();
    int n = 0;
    // first find the number of digits in name end
    int digits = 1;
    for (; digits < len; digits++) {
      try {
        n = Integer.parseInt(imagePath.substring(len-digits));
      }
      catch (NumberFormatException ex) {
         break;
      }
    }
    digits--; // failed at digits, so go back one
    if (digits == 0) { // no number found
       return new Image[] {image};
     }
    // image name ends with number, so look for sequence
    ArrayList imageList = new ArrayList();
    imageList.add(image);
    int limit = 10;
    digits = Math.min(digits, 4);
    switch(digits) {
      case 1: limit = 10; break;
      case 2: limit = 100; break;
      case 3: limit = 1000; break;
      case 4: limit = 10000;
    }
    String root = imagePath.substring(0, len-digits);
    try {
      boolean asked = false;
      while (n < limit-1) {
        n++;
        // fill with leading zeros if nec
        String num = String.valueOf(n);
        int zeros = digits - num.length();
        for (int k = 0; k < zeros; k++) {
          num = "0" + num; //$NON-NLS-1$
        }
        imagePath = root + num + extension;
        image = ResourceLoader.getImage(imagePath);
        if (image == null) break;
        if (!asked && alwaysAsk) {
          asked = true;
          // strip path from image name
          String name = (String)getProperty("name"); //$NON-NLS-1$
          name = XML.getName(imagePath);
          int response = JOptionPane.showOptionDialog(
              null,
              name + " " + MediaRes.getString("ImageVideo.Dialog.LoadSequence.Message") + XML.NEW_LINE + //$NON-NLS-1$ //$NON-NLS-2$
              MediaRes.getString("ImageVideo.Dialog.LoadSequence.Query"), //$NON-NLS-1$
              MediaRes.getString("ImageVideo.Dialog.LoadSequence.Title"), //$NON-NLS-1$
              JOptionPane.YES_NO_OPTION,
              JOptionPane.QUESTION_MESSAGE,
              null,
              new String[] {MediaRes.getString("ImageVideo.Dialog.LoadSequence.Button.SingleImage"), MediaRes.getString("ImageVideo.Dialog.LoadSequence.Button.AllImages")}, //$NON-NLS-1$ //$NON-NLS-2$
              MediaRes.getString("ImageVideo.Dialog.LoadSequence.Button.AllImages")); //$NON-NLS-1$
          if (response == JOptionPane.YES_OPTION) break;
        }
        newPaths.add(XML.getRelativePath(imagePath));
        imageList.add(image);
      }
    } catch (NumberFormatException ex) {ex.printStackTrace();}
    return (Image[])imageList.toArray(new Image[0]);
  }

  /**
   * Inserts images starting at the specified index.
   *
   * @param newImages an array of images
   */
  protected void insert(Image[] newImages, int index) {
    int len = images.length;
    index = Math.min(index, len); // in case some images not successfully loaded
    paths.addAll(Math.min(paths.size(), index), newPaths);
    newPaths.clear();
    int n = newImages.length;
    Image[] newArray = new Image[len + n];
    System.arraycopy(images, 0, newArray, 0, index);
    System.arraycopy(newImages, 0, newArray, index, n);
    System.arraycopy(images, index, newArray, index+n, len-index);
    images = newArray;
    rawImage = images[index];
    frameCount = images.length;
    endFrameNumber = frameCount - 1;
    if (coords == null) {
      size = new Dimension(rawImage.getWidth(observer),
                           rawImage.getHeight(observer));
      bufferedImage = new BufferedImage(size.width, size.height,
                                        BufferedImage.TYPE_INT_RGB);
      // create coordinate system and relativeAspects
      coords = new ImageCoordSystem(frameCount);
      coords.addPropertyChangeListener(this);
      aspects = new DoubleArray(frameCount, 1);
    }
    else {
      coords.setLength(frameCount);
      aspects.setLength(frameCount);
    }
    Dimension newDim = getDimension();
    if (newDim.height != dim.height || newDim.width != dim.width) {
    	this.firePropertyChange("size", dim, newDim); //$NON-NLS-1$
    	dim = newDim;
    }
  }
  
//______________________________ static XML.Loader_________________________  

  /**
   * Returns an XML.ObjectLoader to save and load ImageVideo data.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new Loader();
  }

  /**
   * A class to save and load ImageVideo data.
   */
  static class Loader implements XML.ObjectLoader {

    /**
     * Saves ImageVideo data to an XMLControl.
     *
     * @param control the control to save to
     * @param obj the ImageVideo object to save
     */
    public void saveObject(XMLControl control, Object obj) {
      ImageVideo video = (ImageVideo)obj;
      String[] paths = (String[])video.paths.toArray(new String[0]);
      if (paths.length > 0) control.setValue("paths", paths); //$NON-NLS-1$
      if (!video.getFilterStack().isEmpty()) {
        control.setValue("filters", video.getFilterStack().getFilters()); //$NON-NLS-1$
      }
    }

    /**
     * Creates a new ImageVideo.
     *
     * @param control the control
     * @return the new ImageVideo
     */
    public Object createObject(XMLControl control) {
      String[] paths = (String[])control.getObject("paths"); //$NON-NLS-1$
      if (paths == null) {
      	try { // legacy code that opens single image or sequence
          String path = control.getString("path"); //$NON-NLS-1$
          boolean seq = control.getBoolean("sequence"); //$NON-NLS-1$
          if (path != null) {
            ImageVideo vid = new ImageVideo(path, seq);
            return vid;
          }
        }
        catch (IOException ex) {
        	ex.printStackTrace();
        	return null;
        }
      }
      boolean[] sequences = (boolean[])control.getObject("sequences"); //$NON-NLS-1$
      if (sequences != null) {
  	    try { // pre-2007 code
  	      ImageVideo vid = new ImageVideo(paths[0], sequences[0]);
  	      for (int i = 1; i < paths.length; i++) {
  	        vid.append(paths[i], sequences[i]);
  	      }
  	      return vid;
  	    }
  	    catch (Exception ex) {
        	ex.printStackTrace();
        	return null;
  	    }
      }
      try { // 2007 code
      	if (paths.length == 0) return null;
        ImageVideo vid = new ImageVideo(paths[0], false);
        for (int i = 1; i < paths.length; i++) {
          vid.append(paths[i], false);
        }
        vid.rawImage = vid.images[0];
        Collection filters = (Collection)control.getObject("filters"); //$NON-NLS-1$
        if (filters != null) {
          vid.getFilterStack().clear();
          Iterator it = filters.iterator();
          while (it.hasNext()) {
            Filter filter = (Filter)it.next();
            vid.getFilterStack().addFilter(filter);
          }
        }
        return vid;
      }
      catch (Exception ex) {
      	ex.printStackTrace();
      	return null;
      }
    }

    /**
     * This does nothing, but is required by the XML.ObjectLoader interface.
     *
     * @param control the control
     * @param obj the ImageVideo object
     * @return the loaded object
     */
    public Object loadObject(XMLControl control, Object obj) {
      return obj;
    }
  }
}
