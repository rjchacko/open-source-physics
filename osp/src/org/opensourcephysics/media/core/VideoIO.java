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
import java.awt.image.*;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;
import org.opensourcephysics.media.gif.*;

/**
 * This provides static methods for managing video and text input/output.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class VideoIO {

  // static fields
  protected static JFileChooser chooser;
  protected static FileFilter videoFileFilter = new VideoFileFilter();
  protected static FileFilter qtFileFilter;
  protected static FileFilter imageFileFilter;
  protected static Collection videoTypes = new ArrayList();
  protected static String defaultXMLExt = "xml"; //$NON-NLS-1$

  static {
    qtFileFilter = new FileFilter() {
      public boolean accept(File f) {
        if (f == null) return false;
        if (f.isDirectory()) return true;
        if (f.getAbsolutePath().indexOf("QTJava.zip") != -1) return true; //$NON-NLS-1$
        return false;
      }
      public String getDescription() {return "QTJava.zip";} //$NON-NLS-1$
    };
    imageFileFilter = new FileFilter() {
      public boolean accept(File f) {
        if (f == null) return false;
        if (f.isDirectory()) return true;
        String extension = VideoIO.getExtension(f);
        if (extension != null &&
           (extension.equals("gif") || //$NON-NLS-1$
            extension.equals("jpg"))) return true; //$NON-NLS-1$
        return false;
      }
      public String getDescription() {return MediaRes.getString("VideoIO.ImageFileFilter.Description");} //$NON-NLS-1$
    };
  }

  /**
   * protected constructor to discourage instantiation
   */
  protected VideoIO() {/** empty block */}

  /**
   * Gets the extension of a file.
   *
   * @param file the file
   * @return the extension of the file
   */
  public static String getExtension(File file) {
    String ext = null;
    String s = file.getName();
    int i = s.lastIndexOf('.');
    if (i > 0 && i < s.length() - 1) {
      ext = s.substring(i + 1).toLowerCase();
    }
    return ext;
  }

  /**
   * Gets the video file chooser.
   *
   * @return the file chooser
   */
  public static JFileChooser getChooser() {
    if (chooser == null) {
      chooser = new JFileChooser(new File(OSPRuntime.chooserDir));
    }
    return chooser;
  }

  /**
   * Sets the default xml extension used when saving data.
   *
   * @param ext the default extension
   */
  public static void setDefaultXMLExtension(String ext) {
    defaultXMLExt = ext;
  }

  /**
   * Gets the path relative to the user directory.
   *
   * @param absolutePath the absolute path
   * @return the relative path
   */
  public static String getRelativePath(String absolutePath) {
    if (absolutePath.indexOf("/") == -1 && absolutePath.indexOf("\\") == -1) //$NON-NLS-1$ //$NON-NLS-2$
      return absolutePath;
    if (absolutePath.startsWith("http:")) return absolutePath; //$NON-NLS-1$
    String path = absolutePath;
    String relativePath = ""; //$NON-NLS-1$
    boolean validPath = false;
    // relative to user directory
    String base = System.getProperty("user.dir"); //$NON-NLS-1$
    if (base == null) return path;
    for (int j = 0; j < 3; j++) {
      if (j > 0) {
        // move up one level
        int k = base.lastIndexOf("\\"); //$NON-NLS-1$
        if (k == -1) k = base.lastIndexOf("/"); //$NON-NLS-1$
        if (k != -1) {
          base = base.substring(0, k);
          relativePath += "../"; //$NON-NLS-1$
        }
        else break; // no more levels!
      }
      if (path.startsWith(base)) {
        path = path.substring(base.length() + 1);
        // replace backslashes with forward slashes
        int i = path.indexOf("\\"); //$NON-NLS-1$
        while (i != -1) {
          path = path.substring(0, i) + "/" + path.substring(i + 1); //$NON-NLS-1$
          i = path.indexOf("\\"); //$NON-NLS-1$
        }
        relativePath += path;
        validPath = true;
        break;
      }
    }
    if (validPath) return relativePath;
    return path;
  }

  /**
   * Adds a video type to the list of available types
   *
   * @param type the video type
   */
  public static void addVideoType(VideoType type) {
    if (type != null) {
      boolean hasType = false;
      Iterator it = videoTypes.iterator();
      while (it.hasNext()) {
        if (it.next().getClass().equals(type.getClass()))
          hasType = true;
      }
      if (!hasType) {
        videoTypes.add(type);
      }
    }
  }

  /**
   * Gets an array of available video types
   *
   * @return the video types
   */
  public static VideoType[] getVideoTypes() {
    return (VideoType[])videoTypes.toArray(new VideoType[0]);
  }

  /**
   * Returns a video from a specified file. May return null.
   *
   * @param file the file
   * @return the video
   */
  public static Video getVideo(File file) {
    Video video = null;
    Iterator it = videoTypes.iterator();
    while (it.hasNext()) {
      VideoType vidType = (VideoType) it.next();
      video = vidType.getVideo(file.getAbsolutePath());
      if (video != null)
        break;
    }
    return video;
  }

  /**
   * Returns a clone of the specified video.
   *
   * @param video the video to clone
   * @return the clone
   */
  public static Video clone(Video video) {
    if (video == null) return null;
    XMLControl control = new XMLControlElement(video);
    return (Video)new XMLControlElement(control).loadObject(null);
  }

  /**
   * Loads the specified video panel from a file selected with a chooser
   * and sets the data file of the panel.
   *
   * @param vidPanel the video panel
   * @return an array containing the loaded object and file
   */
  public static File open(VideoPanel vidPanel) {
    return open((File)null, vidPanel);
  }

  /**
   * Displays a file chooser and returns the chosen file.
   *
   * @param type may be "open", "open video", "save", "qt", "insert image"
   * @return the file, or null if no file chosen
   */
  public static File getChooserFile(String type) {
    JFileChooser chooser = getChooser();
    int result = JFileChooser.CANCEL_OPTION;
    if (type.toLowerCase().equals("open")) { //$NON-NLS-1$
      chooser.removeChoosableFileFilter(qtFileFilter);
      chooser.removeChoosableFileFilter(imageFileFilter);
      chooser.addChoosableFileFilter(videoFileFilter);
      chooser.setFileFilter(chooser.getAcceptAllFileFilter());
      result = chooser.showOpenDialog(null);
    }
    else if (type.toLowerCase().equals("open video")) { //$NON-NLS-1$
      chooser.removeChoosableFileFilter(qtFileFilter);
      chooser.removeChoosableFileFilter(imageFileFilter);
      chooser.addChoosableFileFilter(videoFileFilter);
      chooser.setFileFilter(videoFileFilter);
      result = chooser.showOpenDialog(null);
    }
    else if (type.toLowerCase().equals("save")) { //$NON-NLS-1$
      chooser.removeChoosableFileFilter(videoFileFilter);
      chooser.removeChoosableFileFilter(qtFileFilter);
      chooser.removeChoosableFileFilter(imageFileFilter);
      chooser.setDialogTitle(MediaRes.getString("VideoIO.Dialog.SaveAs.Title")); //$NON-NLS-1$
      String filename = MediaRes.getString("VideoIO.FileName.Untitled"); //$NON-NLS-1$
      chooser.setSelectedFile(new File(filename + "." + defaultXMLExt)); //$NON-NLS-1$
      result = chooser.showSaveDialog(null);
    }
    else if (type.toLowerCase().equals("qt")) { //$NON-NLS-1$
      chooser.removeChoosableFileFilter(videoFileFilter);
      chooser.removeChoosableFileFilter(imageFileFilter);
      chooser.addChoosableFileFilter(qtFileFilter);
      chooser.setFileFilter(qtFileFilter);
      chooser.setDialogTitle(MediaRes.getString("VideoIO.Dialog.FindQT.Title")); //$NON-NLS-1$
      result = chooser.showDialog(null, MediaRes.getString("Dialog.Button.OK")); //$NON-NLS-1$
    }
    else if (type.toLowerCase().equals("insert image")) { //$NON-NLS-1$
      chooser.removeChoosableFileFilter(qtFileFilter);
      chooser.removeChoosableFileFilter(videoFileFilter);
      chooser.addChoosableFileFilter(imageFileFilter);
      chooser.setFileFilter(imageFileFilter);
      result = chooser.showOpenDialog(null);
    }
    if (result == JFileChooser.APPROVE_OPTION) {
      return chooser.getSelectedFile();
    }
    return null;
  }


  /**
   * Loads data or a video from a specified file into a VideoPanel.
   * If file is null, a file chooser is displayed.
   *
   * @param file the file to be loaded
   * @param vidPanel the video panel
   * @return the file opened
   */
  public static File open(File file, VideoPanel vidPanel) {
    JFileChooser chooser = getChooser();
    if (file == null) {
      chooser.setDialogTitle(MediaRes.getString("VideoIO.Dialog.Open.Title")); //$NON-NLS-1$
      file = getChooserFile("open"); //$NON-NLS-1$
    }
    if (file == null) return null;
    if (videoFileFilter.accept(file)) { // load video
      VideoType[] types = getVideoTypes();
      Video video = null;
      for (int i = 0; i < types.length; i++) {
        video = types[i].getVideo(file.getAbsolutePath());
        if (video != null) break;
      }
      if (video != null) {
        vidPanel.setVideo(video);
        vidPanel.repaint();
      }
      else {
        JOptionPane.showMessageDialog(null,
        				MediaRes.getString("VideoIO.Dialog.BadVideo.Message") + //$NON-NLS-1$
        				file.getAbsolutePath());
      }
    }
    else { // load data
      XMLControlElement control = new XMLControlElement();
      control.read(file.getAbsolutePath());
      Class type = control.getObjectClass();
      if (VideoPanel.class.isAssignableFrom(type)) {
        vidPanel.setDataFile(file);
        control.loadObject(vidPanel);
      }
      else if (!control.failedToRead()) {
        JOptionPane.showMessageDialog(
            null,
            "\"" + file.getName() + "\" " + //$NON-NLS-1$ //$NON-NLS-2$
            MediaRes.getString("VideoIO.Dialog.XMLMismatch.Message"), //$NON-NLS-1$
            MediaRes.getString("VideoIO.Dialog.XMLMismatch.Title"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
        return null;
      }
      else {
        JOptionPane.showMessageDialog(null,
        				MediaRes.getString("VideoIO.Dialog.BadFile.Message") + //$NON-NLS-1$
        				file.getAbsolutePath());
      }
      vidPanel.changed = false;
    }
    return file;
  }

  /**
   * Writes VideoPanel data to the specified file. If the file is null
   * it brings up a chooser.
   *
   * @param file the file to write to
   * @param vidPanel the video panel
   * @return the file written to, or null if not written
   */
  public static File save(File file, VideoPanel vidPanel) {
    if (file == null) {
      JFileChooser chooser = getChooser();
      chooser.removeChoosableFileFilter(videoFileFilter);
      chooser.removeChoosableFileFilter(imageFileFilter);
      chooser.removeChoosableFileFilter(qtFileFilter);
      chooser.setDialogTitle(MediaRes.getString("VideoIO.Dialog.SaveAs.Title")); //$NON-NLS-1$
      Video video = vidPanel.getVideo();
      String filename = MediaRes.getString("VideoIO.FileName.Untitled"); //$NON-NLS-1$
      if (vidPanel.getFilePath() != null) {
      	filename = XML.stripExtension(vidPanel.getFilePath());
      }
      else if (video != null && video.getProperty("name") != null) { //$NON-NLS-1$
        filename = (String)video.getProperty("name"); //$NON-NLS-1$
        int i = filename.lastIndexOf("."); //$NON-NLS-1$
        if (i > 0) {
          filename = filename.substring(0, i);
        }
      }
      file = new File(filename + "." + defaultXMLExt); //$NON-NLS-1$
      String parent = XML.getDirectoryPath(filename);
      if (!parent.equals("")) { //$NON-NLS-1$
      	XML.createFolders(parent);
      	chooser.setCurrentDirectory(new File(parent));
      }
      chooser.setSelectedFile(file);
      int result = chooser.showSaveDialog(null);
      if (result == JFileChooser.APPROVE_OPTION) {
        file = chooser.getSelectedFile();
        if (getExtension(file) == null) {
          file = new File(file.getPath() + "." + defaultXMLExt); //$NON-NLS-1$
        }
        if (file.exists()) {
          int selected = JOptionPane.showConfirmDialog(null,
              " \"" + file.getName() + "\" " //$NON-NLS-1$ //$NON-NLS-2$
              + MediaRes.getString("VideoIO.Dialog.FileExists.Message"), //$NON-NLS-1$
              MediaRes.getString("VideoIO.Dialog.FileExists.Title"),  //$NON-NLS-1$
              JOptionPane.YES_NO_CANCEL_OPTION);
          if (selected != JOptionPane.YES_OPTION) {
            return null;
          }
        }
        vidPanel.setDataFile(file);
      }
      else return null;
    }
    XMLControl xmlControl = new XMLControlElement(vidPanel);
    xmlControl.write(file.getAbsolutePath());
    vidPanel.changed = false;
    return file;
  }

  /**
   * Records a video of the current video clip and overlays.
   *
   * @param vidPanel the video panel to save
   * @param type the video type
   * @return the file saved, or null if not recorded/saved
   */
  public static File recordVideo(VideoPanel vidPanel, VideoType type) {
    VideoRecorder vr = type.getRecorder();
    if (vr == null) {
      JOptionPane.showMessageDialog(null, MediaRes.getString("VideoIO.Dialog.NoRecorder.Message")); //$NON-NLS-1$
      return null;
    }
    try {
      final VideoRecorder recorder = vr;
      recorder.createVideo();
      // create an image to use for rendering
      BufferedImage image = new BufferedImage(vidPanel.getWidth(),
                                              vidPanel.getHeight(),
                                              BufferedImage.TYPE_INT_RGB);
      // get the player and set the video clip to step 0
      final VideoPlayer player = vidPanel.getPlayer();
      player.stop();
      player.setStepNumber(0);
      final int stepCount = player.getVideoClip().getStepCount();
      int stepNumber;
      //Custom button text for JOptionPane
      Object[] options = {MediaRes.getString("VideoIO.Dialog.AddFrame.Button.Add"), //$NON-NLS-1$
          MediaRes.getString("VideoIO.Dialog.AddFrame.Button.Skip"), //$NON-NLS-1$
          MediaRes.getString("VideoIO.Dialog.AddFrame.Button.End")}; //$NON-NLS-1$
      // step thru frames in the clip and add those desired
      outer:
      for (stepNumber = 0; stepNumber < stepCount; stepNumber++) {
        // ask whether to add current frame
        int result = JOptionPane.showOptionDialog(null,
            MediaRes.getString("VideoIO.Dialog.AddFrame.Message"), //$NON-NLS-1$
            MediaRes.getString("VideoIO.Dialog.AddFrame.Title"), //$NON-NLS-1$
            JOptionPane.YES_NO_CANCEL_OPTION,
            JOptionPane.QUESTION_MESSAGE,
            null,  // icon
            options,
            options[0]);
        // perform requested actions
        switch (result) {
          case 0: // add frame
            if (stepNumber < stepCount-1) {
              double dt = player.getStepTime(stepNumber+1)
                        - player.getStepTime(stepNumber);
              recorder.setFrameDuration(dt);
            }
            recorder.addFrame(vidPanel.render(image));
          case 1: // step to the next frame
            if (stepNumber < stepCount-1) {
              player.step();
              continue;
            }
            recorder.saveVideo();
            break;
          case 2: // save video and exit
            recorder.saveVideo();
            break outer; // exit the for loop
        }
      }
      String fileName = recorder.getFileName();
      if (fileName != null)
      	return new File(fileName);
    } catch(IOException ex) {ex.printStackTrace();}
    return null;
  }

  static {
    // add image video type
    addVideoType(new GifVideoType());
    addVideoType(new ImageVideoType());
  }
}

