/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import java.io.*;
import java.util.*;
import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import org.opensourcephysics.display.DisplayRes;

/**
 * Utility classes to work with Ejs at a high level
 *
 * @author Francisco Esquembre (http://fem.um.es)
 * @version 1.0
 */
public class EjsTool {
  static private final String INFO_FILE = ".Ejs.txt";
  static public final String GET_MODEL_METHOD = "_getEjsModel";
  static public final String GET_RESOURCES_METHOD = "_getEjsResources";

  // ---- Localization

  static private final String BUNDLE_NAME = "org.opensourcephysics.resources.tools.tools"; //$NON-NLS-1$
  static private ResourceBundle res = ResourceBundle.getBundle(BUNDLE_NAME);

  static public void setLocale(Locale locale){
    res = ResourceBundle.getBundle(BUNDLE_NAME, locale);
  }

  static public String getString(String key) {
    try { return res.getString(key); }
    catch (MissingResourceException e) { return '!' + key + '!'; }
  }

  // --- End of localization

  /**
   * Whether a class provides an Ejs model.
   * @param _ejsClass Class
   * @return boolean
   */
  static public boolean hasEjsModel (Class _ejsClass) {
    try {
      Class [] c = {};
      java.lang.reflect.Method getModelMethod = _ejsClass.getMethod(GET_MODEL_METHOD,c);
      return getModelMethod!=null;
    }
    catch (Exception _exc) { return false; }
  }

  /**
   * Runs the Ejs model corresponding to the given class.
   * The model and resources required will be extracted using
   * the ResourceLoader utility.
   * @param _ejsClass Class
   * @return boolean true if successful
   */
  static public boolean runEjs(Class _ejsClass) {
    try {
      Class [] c = {};
      java.lang.reflect.Method getModelMethod = _ejsClass.getMethod(GET_MODEL_METHOD,c);
      java.lang.reflect.Method getResourcesMethod = _ejsClass.getMethod(GET_RESOURCES_METHOD,c);
      Object[] o = {};
      String model = (String) getModelMethod.invoke(null,o);
      ArrayList list;
      if (getResourcesMethod!=null) list = (ArrayList) getResourcesMethod.invoke(null,o);
      else list = new ArrayList();
      return doRunEjs(model,list,_ejsClass);
    }
    catch (Exception _exc) {
      _exc.printStackTrace();
      String[] message=new String[]{res.getString("EjsTool.EjsNotRunning") ,res.getString("EjsTool.NoModel")+" "+_ejsClass.getName()};
      JOptionPane.showMessageDialog((JFrame)null,message,res.getString("EjsTool.Error"),JOptionPane.WARNING_MESSAGE);
      return false;
    }
  }

  /**
   * To be used by Ejs only.
   * It is here to make sure that the information saved matches the
   * information read by runEjs().
   * @param _release String
   */
  static public void saveInformation(String _home,String _release) {
    try {
      String filename = System.getProperty("user.home").replace('\\','/');
      if (!filename.endsWith("/")) filename = filename + "/";
      filename = filename + INFO_FILE;
      String dir = System.getProperty("user.dir");
      FileWriter fout = new FileWriter(filename);
      fout.write("directory = "+dir+"\n");
      fout.write("home = "+_home+"\n");
      fout.write("version = "+_release+"\n");
      fout.close();
    } catch (Exception exc) {}
  }

  //------------------------------------
  // Private methods and inner classes
  //------------------------------------

  /**
   * Extracts an Ejs model (and its resource files) from the given
   * source and then runs Ejs with that model. The example is extracted
   * in the Simulations directory of the users' Ejs. The user will be warned
   * before overwritting any file.
   * @return boolean
   */
  static private boolean doRunEjs(String _model, ArrayList _resources, Class _ejsClass) {
    File rootDir=null;
    String home="Simulations";
    try {
      String filename = System.getProperty("user.home").replace('\\','/');
      if (!filename.endsWith("/")) filename = filename + "/";
      filename = filename + INFO_FILE;
      String dir = null; //, version=null;
      Reader reader=new FileReader(filename);
      if (reader==null) return false;
      LineNumberReader l = new LineNumberReader(reader);
      String sl = l.readLine();
      while (sl != null) {
        if      (sl.startsWith("directory = ")) dir     = sl.substring("directory = ".length()).trim();
        //else if (sl.startsWith("version = "))   version = sl.substring("version = ".length()).trim();
        else if (sl.startsWith("home = "))      home    = sl.substring("home = ".length()).trim();
        sl = l.readLine();
      }
      reader.close();
      rootDir = new File(dir);
      if (!new File (rootDir,"EjsConsole.jar").exists()) rootDir = null;
    } catch (Exception exc) { rootDir = null; }
    while (rootDir==null) { // Ask the user for it
//      String[] message=new String[]{res.getString("EjsTool.EjsNotFound") ,res.getString("EjsTool.IndicateRootDir")};
//      JOptionPane.showMessageDialog((JFrame)null,message,res.getString("EjsTool.Message"),JOptionPane.WARNING_MESSAGE);
      JFileChooser chooser = new JFileChooser();
      chooser.setDialogTitle(res.getString("EjsTool.EjsNotFound"));
      chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
      chooser.setMultiSelectionEnabled(false);
      // The message
      JTextArea textArea   = new JTextArea (res.getString("EjsTool.IndicateRootDir"));
      textArea.setWrapStyleWord(true);
      textArea.setLineWrap(true);
      textArea.setEditable(false);
      textArea.setFont(textArea.getFont().deriveFont(java.awt.Font.BOLD));
      textArea.setPreferredSize(new java.awt.Dimension(150,60));
      textArea.setBackground(chooser.getBackground());
      textArea.setBorder(new javax.swing.border.EmptyBorder(5,10,0,0));
      chooser.setAccessory(textArea);
      if (chooser.showOpenDialog((java.awt.Component)null) != JFileChooser.APPROVE_OPTION) return false; // The user cancelled
      rootDir = chooser.getSelectedFile();
      if (rootDir==null) return false; // The user cancelled
      if (!new File (rootDir,"EjsConsole.jar").exists()) rootDir = null;
      home = "Simulations";
    }
    File simulationsDir = new File (rootDir,home);
    if (!simulationsDir.exists()) simulationsDir.mkdirs();
    if (!_resources.contains(_model)) _resources.add(0,_model);
    AbstractList finalList = ejsConfirmList((java.awt.Component)null,new java.awt.Dimension(400,400),
      res.getString("EjsTool.ExtractingFiles"),res.getString("EjsTool.Message"),_resources);
    if (finalList==null) return false; // The user cancelled
    // Add the "files" directory to the ResourceLoader
    String filesDir=null;
    if (ResourceLoader.getResource(_model)==null) { // Seacrh in the class "files" directory
      filesDir = _ejsClass.getName().replace('.', '/') + "/files";
      ResourceLoader.addSearchPath(filesDir);
    }
    if (!JarTool.extract((Object)null,finalList,simulationsDir)) {  // _source
      if (filesDir!=null) ResourceLoader.removeSearchPath(filesDir);
      return false;
    }
    if (filesDir!=null) ResourceLoader.removeSearchPath(filesDir);
    // Now run the Ejs console
    Runner runner = new Runner(rootDir,simulationsDir.getAbsolutePath()+"/"+_model);
    java.lang.Thread thread = new Thread(runner);
    thread.setPriority(Thread.NORM_PRIORITY);
    thread.start();
    return true;
  }

  /**
   * This method receives a list of objects which it exposes to the user.
   * The user can remove objects from the list and click ok.
   * The class then returns the modified list.
   * If the user clicks cancel it will return null
   * @param _target Component The dialog will be shown relative to this component
   * @param _size Dimension The size of the display dialog
   * @param _message String The message to display
   * @param _title String The title for the display dialog
   * @param _list AbstractList The initial list of objects
   * @return AbstractList
   */
  public static AbstractList ejsConfirmList (Component _target, Dimension _size, String _message, String _title, AbstractList _list) {
    class ReturnValue { boolean value = false; }
    final ReturnValue returnValue=new ReturnValue();

    final DefaultListModel listModel = new DefaultListModel();
    for (int i=0,n=_list.size(); i<n; i++) listModel.addElement(_list.get(i));
    final JList list = new JList(listModel);
    list.setEnabled(true);
    list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.setSelectionInterval(0,listModel.getSize()-1);
    JScrollPane scrollPane = new JScrollPane(list);
    scrollPane.setPreferredSize(_size);

    final JDialog dialog = new JDialog();

    java.awt.event.MouseAdapter mouseListener =  new java.awt.event.MouseAdapter () {
      public void mousePressed (java.awt.event.MouseEvent evt) {
        AbstractButton button = (AbstractButton) (evt.getSource());
        String aCmd = button.getActionCommand();
        if      (aCmd.equals("ok"))      { returnValue.value = true;  dialog.setVisible (false); }
        else if (aCmd.equals("cancel"))  { returnValue.value = false; dialog.setVisible (false); }
        else if (aCmd.equals("selectall")) { list.setSelectionInterval(0,listModel.getSize()-1); }
        else if (aCmd.equals("selectnone")) { list.removeSelectionInterval(0,listModel.getSize()-1); }
      }
    };

    JButton okButton = new JButton (DisplayRes.getString("GUIUtils.Ok"));
    okButton.setActionCommand ("ok");
    okButton.addMouseListener (mouseListener);

    JButton cancelButton = new JButton (DisplayRes.getString("GUIUtils.Cancel"));
    cancelButton.setActionCommand ("cancel");
    cancelButton.addMouseListener (mouseListener);

    JButton excludeButton = new JButton (DisplayRes.getString("GUIUtils.SelectAll"));
    excludeButton.setActionCommand ("selectall");
    excludeButton.addMouseListener (mouseListener);

    JButton includeButton = new JButton (DisplayRes.getString("GUIUtils.SelectNone"));
    includeButton.setActionCommand ("selectnone");
    includeButton.addMouseListener (mouseListener);

    JPanel buttonPanel = new JPanel (new FlowLayout(FlowLayout.CENTER));
    buttonPanel.add (okButton);
    buttonPanel.add (excludeButton);
    buttonPanel.add (includeButton);
    buttonPanel.add (cancelButton);

    JPanel topPanel = new JPanel (new BorderLayout ());

    JTextArea textArea   = new JTextArea (_message);
    textArea.setWrapStyleWord(true);
    textArea.setLineWrap(true);
    textArea.setEditable(false);
    textArea.setFont(textArea.getFont().deriveFont(Font.BOLD));
    textArea.setBackground(topPanel.getBackground());
    textArea.setBorder(new javax.swing.border.EmptyBorder(5,5,10,5));

    topPanel.setBorder(new javax.swing.border.EmptyBorder(5,10,5,10));
    topPanel.add (textArea,BorderLayout.NORTH);
    topPanel.add (scrollPane,BorderLayout.CENTER);

    JSeparator sep1 = new JSeparator (SwingConstants.HORIZONTAL);

    JPanel southPanel = new JPanel (new java.awt.BorderLayout());
    southPanel.add (sep1,java.awt.BorderLayout.NORTH);
    southPanel.add (buttonPanel,java.awt.BorderLayout.SOUTH);

    dialog.getContentPane().setLayout (new java.awt.BorderLayout(5,0));
    dialog.getContentPane().add (topPanel,java.awt.BorderLayout.CENTER);
    dialog.getContentPane().add (southPanel,java.awt.BorderLayout.SOUTH);

    dialog.addWindowListener (
      new java.awt.event.WindowAdapter() {
        public void windowClosing(java.awt.event.WindowEvent event) { returnValue.value = false; }
      }
    );

//    dialog.setSize (_size);
    dialog.validate();
    dialog.pack();
    dialog.setTitle (_title);
    dialog.setLocationRelativeTo (_target);
    dialog.setModal(true);

    dialog.setVisible (true);
    if (!returnValue.value) return null;
    Object[] selection = list.getSelectedValues();
    AbstractList newList = new ArrayList();
    for (int i=0,n=selection.length; i<n; i++) newList.add(selection[i]);
    return newList;
  }


  /**
   * Inner class to run Ejs
   */
  static private class Runner implements Runnable {
    File rootDir;
    String model;

    Runner(File _dir, String _model) { rootDir = _dir; model = _model; }

    public void run () {
      try {
        final Vector cmd = new Vector();
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) cmd.add(javaHome + java.io.File.separator + "bin" + java.io.File.separator + "java");
        else cmd.add("java");
        cmd.add("-jar");
        cmd.add("EjsConsole.jar");
        if (model!=null) cmd.add(model);
        String[] cmdarray = (String[]) cmd.toArray(new String[0]);
//        for (int i=0; i<cmdarray.length; i++) System.out.println ("Trying to run ["+i+"] = "+cmdarray[i]);
        Process proc = Runtime.getRuntime().exec(cmdarray, null, rootDir);
        proc.waitFor();
      }
      catch (Exception exc) { exc.printStackTrace(); }
    }

  } // End of inner class

} // End of class

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
