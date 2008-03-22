/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display;
import javax.swing.*;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.io.File;
import java.lang.reflect.Method;
import java.awt.Component;

import org.opensourcephysics.controls.XML;
import org.opensourcephysics.tools.Translator;
import java.net.URL;
import java.net.JarURLConnection;

/**
 * This defines static methods related to the runtime environment.
 * 
 * @author Douglas Brown
 * @author Wolfgang Chrstian
 * @version 1.0
 */
public class OSPRuntime {

   static String version = "1.3"; //$NON-NLS-1$
   static String releaseDate = "March 1, 2008"; //$NON-NLS-1$

   /** Disables drawing for faster start-up and to avoid screen flash in Drawing Panels. */
   volatile public static boolean disableAllDrawing= false;

   /** Shared Translator, if available. */
   public static Translator translator; // shared Translator

   /** Array of default OSP Locales. */
   public static Locale[] defaultLocales = new Locale[]{
  	 Locale.ENGLISH, new Locale("es"), new Locale("de"), //$NON-NLS-1$ //$NON-NLS-2$
     new Locale("da"), new Locale("sk"), Locale.TAIWAN}; //$NON-NLS-1$ //$NON-NLS-2$

   /** Set <I>true</I> if a program is being run within Launcher. */
   protected static boolean launcherMode = false;

   /** True if running as an applet. */
   public static boolean appletMode;

   /** Static reference to an applet for document/code base access. */
   public static JApplet applet;

   /** True if launched by WebStart. */
   public static boolean webStart;

   /** True if users allowed to author internal parameters such as Locale strings. */
   protected static boolean authorMode = true;

   /** Look and feel property for the graphical user interface. */
   public static boolean javaLookAndFeel = false;

   /** Path of the launch jar, if any. */
   static private String launchJarPath;

   /** Path of the launch jar, if any. */
   static private String launchJarName;

   /** The launch jar, if any. */
   static private JarFile launchJar = null;

   /** File Chooser starting directory. */
   public static String chooserDir;

   /** Location of OSP icon. */
   public static final String OSP_ICON_FILE = "/org/opensourcephysics/resources/controls/images/osp_icon.gif"; //$NON-NLS-1$

   /** True if always launching in single vm (applet mode, etc). */
   public static boolean launchingInSingleVM;

   /**
    * Sets default properties for OSP.
    */
   static {
      //java.util.Locale.setDefault(new java.util.Locale("es"));  // test of language resources
      JFrame.setDefaultLookAndFeelDecorated(OSPRuntime.javaLookAndFeel);
      JDialog.setDefaultLookAndFeelDecorated(OSPRuntime.javaLookAndFeel);
      // sets the default directory for the chooser
      try {                                                                                   // system properties may not be readable in some contexts
         OSPRuntime.chooserDir = System.getProperty("user.dir", null); //$NON-NLS-1$
      } catch(Exception ex) {
         OSPRuntime.chooserDir = null;
      }
      // creates the shared Translator
      try {
         Class translatorClass = Class.forName("org.opensourcephysics.tools.TranslatorTool"); //$NON-NLS-1$
         Method m = translatorClass.getMethod("getTool", (Class[]) null);                     //$NON-NLS-1$
         translator = (Translator) m.invoke(null, (Object[]) null);
      } catch(Exception ex) {
      /** empty block */
      }
   }

   /**
    * Private constructor to prevent instantiation.
    */
   private OSPRuntime() {
      /** empty block */
   }

   /**
    * Shows the about dialog.
    */
   public static void showAboutDialog(Component parent) {
      String aboutString = "OSP Library "+version+" released "+releaseDate+"\n"           //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                           +"Open Source Physics Project \n"+"www.opensourcephysics.org"; //$NON-NLS-1$ //$NON-NLS-2$
      JOptionPane.showMessageDialog(parent, aboutString, "About Open Source Physics", JOptionPane.INFORMATION_MESSAGE); //$NON-NLS-1$
   }

   /**
    * Determines if OS is Windows
    *
    * @return true if Windows
    */
   public static boolean isWindows() {
      try { // system properties may not be readable in some environments
         return(System.getProperty("os.name", "").toLowerCase().startsWith("windows")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      } catch(SecurityException ex) {
         return false;
      }
   }

   /**
    * Determines if OS is Mac
    *
    * @return true if Mac
    */
   public static boolean isMac() {
      try { // system properties may not be readable in some environments
         return(System.getProperty("os.name", "").toLowerCase().startsWith("mac")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      } catch(SecurityException ex) {
         return false;
      }
   }

   /**
    * Determines if OS is Linux
    *
    * @return true if Linux
    */
   public static boolean isLinux() {
      try { // system properties may not be readable in some environments
         return(System.getProperty("os.name", "").toLowerCase().startsWith("linux")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      } catch(SecurityException ex) {
         return false;
      }
   }

   /**
    * Determines if OS is Vista
    *
    * @return true if Vistsa
    */
   static public boolean isVista() {
      if(System.getProperty("os.name", "").toLowerCase().indexOf("vista")>-1) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
         return true;
      }
      return false;
   }

   /**
    * Determines if launched by WebStart
    *
    * @return true if launched by WebStart
    */
   public static boolean isWebStart() {
      if(!webStart) {
         try {
            webStart = Class.forName("javax.jnlp.BasicService")!=null; //$NON-NLS-1$
            // once true, remains true
         } catch(Exception ex) {
            /** empty block */
         }
      }
      return webStart;
   }

   /**
    * Determines if running as an applet
    *
    * @return true if running as an applet
    */
   public static boolean isAppletMode() {
      return appletMode;
   }

   /**
    * Determines if running in author mode
    *
    * @return true if running in author mode
    */
   public static boolean isAuthorMode() {
      return authorMode;
   }

   /**
    * Sets the authorMode property.
    * AuthorMode allows users to author internal parameters such as Locale strings.
    *
    * @param b boolean
    */
   public static void setAuthorMode(boolean b) {
      authorMode = b;
   }

   /**
    * Sets the launcherMode property.
    * LauncherMode disables access to propertes, such as Locale, that affect the VM.
    *
    * @param b boolean
    */
   public static void setLauncherMode(boolean b) {
      launcherMode = b;
   }

   /**
    * Gets the launcherMode property.
    * LauncherMode disables access to propertes, such as Locale, that affect the VM.
    *
    * @return boolean
    */
   public static boolean isLauncherMode() {
      return launcherMode;
   }

   /**
    * Sets the launch jar path.
    * param path the path
    */
   public static void setLaunchJarPath(String path) {
     if (path == null || launchJarPath !=null)return;
     // make sure the path ends with or contains a jar file
     if (!path.endsWith(".jar")) { //$NON-NLS-1$
       int n = path.indexOf(".jar!"); //$NON-NLS-1$
       if (n > -1) {
         path = path.substring(0, n + 4);
       }
       else return;
     }
     launchJarPath = path;
     launchJarName = path.substring(path.lastIndexOf("/") + 1); //$NON-NLS-1$
   }

   /**
    * Gets the launch jar nsme, if any.
    * @return launch jar path, or null if not launched from a jar
    */
   public static String getLaunchJarName() {
     return launchJarName;
   }


   /**
    * Gets the launch jar path, if any.
    * @return launch jar path, or null if not launched from a jar
    */
   public static String getLaunchJarPath() {
      return launchJarPath;
   }

   /**
    * Gets the launch jar directory, if any.
    * @return path to the directory containing the launch jar. May be null.
    */
   public static String getLaunchJarDirectory() {
     if(applet!=null) return null;
     return launchJarPath == null? null: XML.getDirectoryPath(launchJarPath);
   }

   /**
    * Gets the jar from which the progam was launched.
    * @return JarFile
    */
   public static JarFile getLaunchJar() {
      if(launchJar != null) return launchJar;
      if(launchJarPath == null) return null;
      boolean isWebFile = launchJarPath.startsWith("http:"); //$NON-NLS-1$
      try {
         if((OSPRuntime.applet==null)&&!isWebFile) {                   // application mode
            launchJar = new JarFile(launchJarPath);
         } else {                                                      // applet mode
            URL url;
            if(isWebFile) {
               // create a URL that refers to a jar file on the web
               url = new URL("jar:"+launchJarPath+"!/");               //$NON-NLS-1$ //$NON-NLS-2$
            } else {
               // create a URL that refers to a local jar file
               url = new URL("jar:file:/"+launchJarPath+"!/");         //$NON-NLS-1$ //$NON-NLS-2$
            }
            // get the jar
            JarURLConnection conn = (JarURLConnection) url.openConnection();
            launchJar = conn.getJarFile();
         }
      } catch(Exception ex) {
         ex.printStackTrace();
      }
      return launchJar;
   }

   /**
    * Gets Locales for languages that have properties files in the core library.
    * @return Locale[]
    */
   public static Locale[] getDefaultLocales() {
      return defaultLocales;
   }

   /**
    * Gets Locales for languages that have properties files in the core library.
    * @return Locale[]
    */
   public static Locale[] getInstalledLocales() {
      ArrayList list = new ArrayList();
      list.add(Locale.ENGLISH); // english is first in list
      if(getLaunchJarPath()!=null) {
         // find available locales
         JarFile jar = getLaunchJar();
         if(jar!=null) {
            for(Enumeration e = jar.entries(); e.hasMoreElements(); ) {
               JarEntry entry = (JarEntry) e.nextElement();
               String path = entry.toString();
               int n = path.indexOf(".properties"); //$NON-NLS-1$
               if(path.indexOf(".properties")>-1) { //$NON-NLS-1$
                  int m = path.indexOf("display_res_"); //$NON-NLS-1$
                  if(m>-1) {
                     String loc = path.substring(m+12, n);
                     if(loc.equals("zh_TW")) { //$NON-NLS-1$
                        list.add(Locale.TAIWAN);
                     } else {
                        Locale next = new Locale(loc);
                        if(!next.equals(Locale.ENGLISH)) {
                           list.add(next);
                        }
                     }
                  }
               }
            }
         }else{
            defaultLocales = new Locale[] { Locale.ENGLISH };
            return defaultLocales;
         }
      }
      return(Locale[]) list.toArray(new Locale[0]);
   }

   /**
    * Gets the translator, if any.
    * @return translator, or null if none available
    */
   public static Translator getTranslator() {
      return translator;
   }

   private static JFileChooser chooser;

   /**
    * Gets a file chooser.
    *
    * The choose is static and will therefore be the same for all OSPFrames.
    *
    * @return the chooser
    */
   public static JFileChooser getChooser() {
      if(chooser!=null) {
         return chooser;
      }
      try {
         chooser = (OSPRuntime.chooserDir==null)
                   ? new JFileChooser() : new JFileChooser(new File(OSPRuntime.chooserDir));
      } catch(Exception e) {
         System.err.println("Exception in OSPFrame getChooser="+e); //$NON-NLS-1$
         return null;
      }
      javax.swing.filechooser.FileFilter defaultFilter = chooser.getFileFilter();
      javax.swing.filechooser.FileFilter xmlFilter = new javax.swing.filechooser.FileFilter() {

         // accept all directories and *.xml files.
         public boolean accept(File f) {
            if(f==null) {
               return false;
            }
            if(f.isDirectory()) {
               return true;
            }
            String extension = null;
            String name = f.getName();
            int i = name.lastIndexOf('.');
            if((i>0)&&(i<name.length()-1)) {
               extension = name.substring(i+1).toLowerCase();
            }
            if((extension!=null)&&(extension.equals("xml"))) { //$NON-NLS-1$
               return true;
            }
            return false;
         }

         // the description of this filter
         public String getDescription() {
            return DisplayRes.getString("OSPRuntime.FileFilter.Description.XML"); //$NON-NLS-1$
         }
      };
      javax.swing.filechooser.FileFilter txtFilter = new javax.swing.filechooser.FileFilter() {

         // accept all directories and *.txt files.
         public boolean accept(File f) {
            if(f==null) {
               return false;
            }
            if(f.isDirectory()) {
               return true;
            }
            String extension = null;
            String name = f.getName();
            int i = name.lastIndexOf('.');
            if((i>0)&&(i<name.length()-1)) {
               extension = name.substring(i+1).toLowerCase();
            }
            if((extension!=null)&&extension.equals("txt")) { //$NON-NLS-1$
               return true;
            }
            return false;
         }

         // the description of this filter
         public String getDescription() {
            return DisplayRes.getString("OSPRuntime.FileFilter.Description.TXT"); //$NON-NLS-1$
         }
      };
      chooser.addChoosableFileFilter(xmlFilter);
      chooser.addChoosableFileFilter(txtFilter);
      chooser.setFileFilter(defaultFilter);
      return chooser;
   }

   /**
    * Uses a JFileChooser to ask for a name.
    * @param chooser JFileChooser
    * @return String The absolute pah of the filename. Null if cancelled
    */
   static public String chooseFilename(JFileChooser chooser) {
      return chooseFilename(chooser, null, true);
   }

   /**
    * Uses a JFileChooser to ask for a name.
    * @param chooser JFileChooser
    * @param parent Parent component for messages
    * @param toSave true if we will save to the chosen file, false if we will read from it
    * @return String The absolute pah of the filename. Null if cancelled
    */
   static public String chooseFilename(JFileChooser chooser, Component parent, boolean toSave) {
      String fileName = null;
      int result;
      if(toSave) {
         result = chooser.showSaveDialog(parent);
      } else {
         result = chooser.showOpenDialog(parent);
      }
      if(result==JFileChooser.APPROVE_OPTION) {
         OSPRuntime.chooserDir = chooser.getCurrentDirectory().toString();
         File file = chooser.getSelectedFile();
         // check to see if file exists
         if(toSave) {                                          // saving: check if the file will be overwritten
            if(file.exists()) {
               int selected = JOptionPane.showConfirmDialog(
                  parent, DisplayRes.getString("DrawingFrame.ReplaceExisting_message")+" "+file.getName() //$NON-NLS-1$ //$NON-NLS-2$
                  +DisplayRes.getString("DrawingFrame.QuestionMark"), DisplayRes.getString( //$NON-NLS-1$
                     "DrawingFrame.ReplaceFile_option_title"), //$NON-NLS-1$
                  JOptionPane.YES_NO_CANCEL_OPTION);
               if(selected!=JOptionPane.YES_OPTION) {
                  return null;
               }
            }
         } else {                                              // Reading: check if thefile actually exists
            if(!file.exists()) {
               JOptionPane.showMessageDialog(parent,
                                             DisplayRes.getString("GUIUtils.FileDoesntExist")+" "+file.getName(), //$NON-NLS-1$ //$NON-NLS-2$
                                             DisplayRes.getString("GUIUtils.FileChooserError"), //$NON-NLS-1$
                                             JOptionPane.ERROR_MESSAGE);
               return null;
            }
         }
         fileName = file.getAbsolutePath();
         if((fileName==null)||fileName.trim().equals("")) {    //$NON-NLS-1$
            return null;
         }
      }
      return fileName;
   }

   /**
    * Creates a JFileChooser with given desription and extensions
    * @param description String A description string
    * @param extensions String[] An array of allowed extensions
    * @return JFileChooser
    */
   static public javax.swing.JFileChooser createChooser(String description, String[] extensions) {
      javax.swing.JFileChooser chooser = new javax.swing.JFileChooser(new File(OSPRuntime.chooserDir));
      ExtensionFileFilter filter = new ExtensionFileFilter();
      for(int i = 0; i<extensions.length; i++) {
         filter.addExtension(extensions[i]);
      }
      filter.setDescription(description);
      chooser.setFileFilter(filter);
      return chooser;
   }

   /**
    * This file filter matches all files with a given set of
    * extensions.
    */
   static private class ExtensionFileFilter extends javax.swing.filechooser.FileFilter {

      private String description = ""; //$NON-NLS-1$
      private java.util.ArrayList extensions = new java.util.ArrayList();

      /**
       *  Adds an extension that this file filter recognizes.
       *  @param extension a file extension (such as ".txt" or "txt")
       */
      public void addExtension(String extension) {
         if(!extension.startsWith(".")) { //$NON-NLS-1$
            extension = "."+extension; //$NON-NLS-1$
         }
         extensions.add(extension.toLowerCase());
      }

      /**
       *  Sets a description for the file set that this file filter
       *  recognizes.
       *  @param aDescription a description for the file set
       */
      public void setDescription(String aDescription) {
         description = aDescription;
      }

      /**
       *  Returns a description for the file set that this file
       *  filter recognizes.
       *  @return a description for the file set
       */
      public String getDescription() {
         return description;
      }

      public boolean accept(File f) {
         if(f.isDirectory()) {
            return true;
         }
         String name = f.getName().toLowerCase();
         // check if the file name ends with any of the extensions
         for(int i = 0; i<extensions.size(); i++) {
            if(name.endsWith((String) extensions.get(i))) {
               return true;
            }
         }
         return false;
      }
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
