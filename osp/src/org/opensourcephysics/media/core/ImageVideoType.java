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

/**
 * This implements the VideoType interface with a buffered image type.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class ImageVideoType implements VideoType {

  /**
   * Opens a video file as an ImageVideo.
   *
   * @param file the video file
   * @return a new image video
   */
  public Video getVideo(File file) {
    try {
      return new ImageVideo(file.getAbsolutePath());
    }
    catch (IOException ex) {
      return null;
    }
  }

  /**
   * Opens a named image as an ImageVideo.
   *
   * @param name the name of the image
   * @return a new image video
   */
  public Video getVideo(String name) {
    try {
      return new ImageVideo(name);
    }
    catch (IOException ex) {
      return null;
    }
  }

  /**
   * Gets a video recorder.
   *
   * @return the video recorder
   */
  public VideoRecorder getRecorder() {
    return null;
  }

  /**
   * Reports whether this type can record videos
   *
   * @return true if this can record videos
   */
  public boolean canRecord() {
    return false;
  }

  /**
   * Gets the name and/or description of this type.
   *
   * @return a description
   */
  public String getDescription() {
    return "JPEG image"; //$NON-NLS-1$
  }

  /**
   * Gets the name and/or description of this type.
   *
   * @return a description
   */
  public String getDefaultExtension() {
    return "jpg"; //$NON-NLS-1$
  }

  /**
   * Gets the file filter for this type.
   *
   * @return a file filter
   */
  public javax.swing.filechooser.FileFilter getFileFilter() {
    return null;
  }

  /**
   * Return true if the specified video is this type.
   *
   * @param video the video
   * @return true if the video is this type
   */
  public boolean isType(Video video) {
    return video.getClass().equals(ImageVideo.class);
  }

}


