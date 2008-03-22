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
import java.text.*;

import java.awt.*;
import javax.swing.*;

import org.opensourcephysics.display.*;
import org.opensourcephysics.controls.*;

/**
 * This VideoRecorder records to a scratch file which is then copied as needed.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public abstract class ScratchVideoRecorder implements VideoRecorder {

  // static fields
  protected static JFileChooser chooser;

  // instance fields
  protected VideoType videoType; // type of video being recorded
  protected Dimension dim; // dimension of new video
  protected Image frameImage; // most recently added frame
  protected double frameDuration = 100; // milliseconds
  protected int frameCount; // number of frames recorded
  protected String scratchName; // base name of scratch files
  protected int scratchNumber = 0; // appended to base name for uniqueness
  protected File scratchFile; // active scratch file
  protected boolean canRecord; // scratch file ready to accept added frames
  protected boolean hasContent; // scratch file has added frames
  protected boolean isSaved; // scratch file has been saved to saveFile
  protected File saveFile = null; // file to which scratch is saved
  protected boolean saveChanges = false; // true to ask to save changes

  /**
   * Constructs a ScratchVideoRecorder for the specified video type.
   *
   * @param vidType the video type
   */
  public ScratchVideoRecorder(VideoType vidType) {
    videoType = vidType;
    Date now = new Date();
    SimpleDateFormat formatter = new SimpleDateFormat("_yyMMdd_HHmmss_"); //$NON-NLS-1$
    String s = formatter.format(now);
    scratchName = "scratch" + s; //$NON-NLS-1$
    ShutdownHook shutdownHook = new ShutdownHook();
    Runtime.getRuntime().addShutdownHook(shutdownHook);
    try {
      createScratch();
    }
    catch (IOException ex) {ex.printStackTrace();}
  }

  /**
   * Creates a new video (scratch file) and sets fileName to null.
   *
   * @throws IOException
   */
  public void createVideo() throws IOException {
    // create scratch file if none
    if (scratchFile == null) {
      createScratch();
      if (scratchFile == null) {
        OSPLog.severe("No scratch file"); //$NON-NLS-1$
        return;
      }
    }
    // replace scratch file with new one if needed
    if (scratchFile != null && hasContent) {
      // save movie if not saved and saveChanges flag is true
      if (saveChanges && !isSaved) {
        String query =
            MediaRes.getString("ScratchVideoRecorder.Dialog.SaveVideo.Message"); //$NON-NLS-1$
        int n = JOptionPane.showConfirmDialog(
            null, query, MediaRes.getString("ScratchVideoRecorder.Dialog.SaveVideo.Title"), JOptionPane.YES_NO_OPTION); //$NON-NLS-1$
        if (n == JOptionPane.YES_OPTION)
          saveVideo();
      }
      createScratch();
      saveFile = null;
      dim = null; // new video will be size of first image
    }
  }

  /**
   * Creates a new video and sets the destination file name.
   *
   * @param fileName name of the file to which the video will be written
   * @throws IOException
   */
  public void createVideo(String fileName) throws IOException {
    createVideo();
    File file = new File(fileName);
    if (file.exists() && !file.canWrite()) {
      saveFile = null;
      throw new IOException("Read-only file"); //$NON-NLS-1$
    }
    saveFile = file;
  }

  /**
   * Creates the file to which a newly created video will be written.
   *
   * @param fileName name of the file to which the video will be written
   * @param width width of the new video
   * @param height height of the new video
   * @throws IOException
   */
  public void createVideo(String fileName, int width, int height) throws IOException {
    createVideo(fileName);
    if (width > 0 && height > 0) dim = new Dimension(width, height);
  }

  /**
   * Sets the time duration per frame.
   *
   * @param millis the duration per frame in milliseconds
   */
  public void setFrameDuration(double millis) {
    frameDuration = millis;
  }

  /**
   * Adds a video frame with the specified image.
   *
   * @param image the image to be drawn on the video frame.
   * @throws IOException
   */
  public void addFrame(Image image) throws IOException {
    if (image == null) return;
    frameImage = image;
    if (scratchFile == null ||
        (hasContent && isSaved)) {
      createVideo();
    }
    if (scratchFile == null) return;
    if (!canRecord) {
      canRecord = startRecording();
      isSaved = false;
      hasContent = false;
    }
    if (canRecord && append(image)) {
      hasContent = true;
      frameCount++;
    }
  }

  /**
   * Gets the current scratch video.
   *
   * @return the video
   * @throws IOException
   */
  public Video getVideo() throws IOException {
    saveScratch();
    return videoType.getVideo(scratchFile.getAbsolutePath());
  }

  /**
   * Saves the scratch video to the current file or chooser file.
   *
   * @return the full path of the saved file
   * @throws IOException
   */
  public String saveVideo() throws IOException {
    if (saveFile != null) {
      return saveVideo(saveFile.getAbsolutePath());
    }
    return saveVideoAs();
  }

  /**
   * Saves the current scratch video to the specified file name.
   *
   * @param fileName the file name
   * @return the full path of the saved file
   * @throws IOException
   */
  public String saveVideo(String fileName) throws IOException {
    if (scratchFile == null) return null;
    if (fileName == null) return saveVideoAs();
    setFileName(fileName);
    if (saveFile == null)  return null;
    saveScratch();
    // copy scratch to fileName
    int buffer = 2048;
    byte[] data = new byte[buffer];
    int count = 0;
    int total = 0;
    FileInputStream fin = new FileInputStream(scratchFile);
    InputStream in = new BufferedInputStream(fin);
    FileOutputStream fout = new FileOutputStream(saveFile);
    OutputStream out = new BufferedOutputStream(fout);
    while ((count = in.read(data, 0, buffer)) != -1) {
      out.write(data, 0, count);
      total += count;
    }
    out.flush();
    out.close();
    in.close();
    isSaved = true;
    OSPLog.fine("copied " + total + " bytes from " + //$NON-NLS-1$ //$NON-NLS-2$
                scratchFile.getName() + " to " + saveFile.getAbsolutePath()); //$NON-NLS-1$
    scratchFile.delete();
    return saveFile.getAbsolutePath();
  }

  /**
   * Saves the scratch video to a file picked from a chooser.
   *
   * @return the full path of the saved file
   * @throws IOException
   */
  public String saveVideoAs() throws IOException {
    File file = selectFile();
    if (file != null) {
      return saveVideo(file.getAbsolutePath());
    }
    return null;
  }

  /**
   * Gets the file name of the destination video.
   * @return the file name
   */
  public String getFileName() {
    return saveFile == null? null: saveFile.getAbsolutePath();
  }

  /**
   * Sets the file name. May be null.
   * @param path the file name
   */
  public void setFileName(String path) {
    if (saveFile != null && saveFile.getAbsolutePath().equals(path)) return;
    File file = new File(path);
    if (file.exists() && !file.canWrite()) {
      saveFile = null;
    }
    else saveFile = file;
  }

//________________________________ private methods _________________________________

  /**
   * Creates the scratch file.
   *
   * @throws IOException
   */
  protected void createScratch() throws IOException {
    if (hasContent || scratchFile == null) {
      if (scratchFile != null) {
        saveScratch();
        scratchFile.delete();
      }
      scratchFile = new File(scratchName + scratchNumber +
                             "." + videoType.getDefaultExtension()); //$NON-NLS-1$
      scratchNumber++;
      hasContent = false;
      canRecord = false;
      OSPLog.finest(scratchFile.getAbsolutePath());
    }
  }

  /**
   * Shows a save dialog used to set the output movie file.
   * @return the movie file
   */
  protected File selectFile() {
    if (chooser == null)
      chooser = new JFileChooser(new File(OSPRuntime.chooserDir));
    File file = null;
    chooser.setDialogTitle("VideoIO.Dialog.SaveAs.Title"); //$NON-NLS-1$
    String filename = MediaRes.getString("VideoIO.FileName.Untitled"); //$NON-NLS-1$
    filename += "." + videoType.getDefaultExtension(); //$NON-NLS-1$
    chooser.setSelectedFile(new File(filename));
    int result = chooser.showDialog(null, MediaRes.getString("Dialog.Button.Save")); //$NON-NLS-1$
    if (result == JFileChooser.APPROVE_OPTION) {
      file = chooser.getSelectedFile();
      if (file.exists()) {
        int selected = JOptionPane.showConfirmDialog(null,
                " \"" + file.getName() + "\" " //$NON-NLS-1$ //$NON-NLS-2$
                + MediaRes.getString("VideoIO.Dialog.FileExists.Message"), //$NON-NLS-1$
                MediaRes.getString("VideoIO.Dialog.FileExists.Title"),  //$NON-NLS-1$
                JOptionPane.YES_NO_CANCEL_OPTION);
        if (selected == JOptionPane.YES_OPTION) {
          return file;
        }
      }
    }
    return file;
  }

  /**
   * Saves the current video to the scratch file.
   *
   * @throws IOException
   */
  abstract protected void saveScratch() throws IOException;

  /**
   * Starts the video recording process using current dimension dim.
   *
   * @return true if video recording successfully started
   */
  abstract protected boolean startRecording();

  /**
   * Appends a frame to the current video.
   *
   * @param image the image to append
   * @return true if image successfully appended
   */
  abstract protected boolean append(Image image);

  /**
   * A class to delete the scratch file on shutdown.
   */
  class ShutdownHook extends Thread {
    public void run() {
      if (scratchFile != null) {
        try {
          saveScratch();
        }
        catch (Exception ex) {/** empty block */}
        scratchFile.deleteOnExit();
      }
    }
  }
}
