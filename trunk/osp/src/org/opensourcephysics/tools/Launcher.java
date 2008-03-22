/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */
package org.opensourcephysics.tools;
import java.io.*;
import java.lang.reflect.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;
import java.awt.*;
import java.awt.event.*;
import java.beans.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.undo.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.*;
import javax.swing.border.Border;
import org.opensourcephysics.controls.XMLControl;
import org.opensourcephysics.controls.XML;
import org.opensourcephysics.controls.OSPLog;
import org.opensourcephysics.controls.XMLControlElement;
import org.opensourcephysics.controls.XMLTableInspector;
import org.opensourcephysics.controls.XMLTreePanel;
import org.opensourcephysics.controls.XMLTable;
import org.opensourcephysics.controls.Password;
import org.opensourcephysics.controls.MessageFrame;
import org.opensourcephysics.display.OSPRuntime;
import org.opensourcephysics.display.DisplayRes;
import org.opensourcephysics.display.PrintUtils;
import org.opensourcephysics.display.AppFrame;
import org.opensourcephysics.tools.FontSizer;
import org.opensourcephysics.tools.ResourceLoader;
import org.opensourcephysics.tools.Resource;
import org.opensourcephysics.tools.EjsTool;
import org.opensourcephysics.tools.Diagnostics;
import org.opensourcephysics.tools.EncryptionTool;
import org.opensourcephysics.tools.LaunchNode.HTML;

/**
 * This provides a GUI for launching osp applications and xml files.
 *
 * @author Douglas Brown
 */
public class Launcher {

  // static fields
  protected static String defaultFileName = "launcher_default";                      //$NON-NLS-1$
  protected static String resourcesPath = "/org/opensourcephysics/resources/tools/"; //$NON-NLS-1$
  protected static String classPath;                                                 // list of jar names for classes, resources
  protected static String tabSetBasePath = "";                                       // absolute base path of tabset      //$NON-NLS-1$
  protected static String version = "1.4";                                           //$NON-NLS-1$
  protected static String releaseDate = "July 2007";                                 //$NON-NLS-1$
  protected static JFileChooser chooser;
  protected static FileFilter xmlFileFilter;
  protected static FileFilter xsetFileFilter;
  protected static FileFilter launcherFileFilter;
  protected static int wInit = 480, hInit = 400;
  protected static JDialog splashDialog;
  protected static JLabel creditsLabel;
  protected static JLabel splashTitleLabel;
  protected static JLabel splashPathLabel;
  protected static javax.swing.Timer splashTimer;
  protected static float baseMenuFontSize;
  // some icons are created in LaunchBuilder
  protected static Icon launchIcon, launchedIcon, singletonIcon, whiteFolderIcon;
  protected static Icon redFileIcon, greenFileIcon, magentaFileIcon, yellowFileIcon;
  protected static Icon whiteFileIcon, noFileIcon, ghostFileIcon;
  protected static Icon redFolderIcon, greenFolderIcon, yellowFolderIcon;
  protected static Icon linkIcon, htmlIcon, launchEmptyIcon, ejsIcon;
  public static boolean singleAppMode = false;
  public static boolean singletonMode = false;
  private static boolean newVMAllowed = false;
  protected static javax.swing.Timer frameFinder;
  protected static ArrayList existingFrames = new ArrayList();
  public static LaunchNode activeNode;
  // instance fields
  protected JDialog xmlInspector;
  protected JDialog tableInspector;
  protected int divider = 160;
  public JFrame frame;
  protected JPanel contentPane;
  protected JTabbedPane tabbedPane;
  protected boolean postEdits = true;
  protected boolean navigationVisible = true;
  protected JToolBar navbar;
  protected Icon navOpenIcon;
  protected Icon navClosedIcon;
  protected JButton navButton;
  protected JButton backButton;
  protected JButton forwardButton;
  protected JMenuItem singleAppItem;
  protected LaunchNode selectedNode;                                                 // currently selected node
  protected LaunchNode previousNode;                                                 // previously selected node
  protected String tabSetName;                                                       // name relative to tabSetBasePath
  protected JTextPane textPane;                                                      // for button view
  protected JScrollPane textScroller;                                                // for button view
  protected boolean showText = true;                                                 // for button view
  protected ArrayList htmlTabList = new ArrayList();                                 // HTML textPanes and scrollers
  protected JMenu fileMenu;
  protected JMenu displayMenu;
  protected JMenu helpMenu;
  protected JMenuItem openItem;
  protected JMenu openFromJarMenu;
  protected JMenuItem closeTabItem;
  protected JMenuItem closeAllItem;
  protected JMenuItem editItem;
  protected JMenuItem exitItem;
  protected JMenuItem inspectItem;
  protected JMenuItem hideItem;
  protected JMenuItem backItem;
  protected JMenu languageMenu;
  protected JMenuItem sizeUpItem;
  protected JMenuItem sizeDownItem;
  protected JMenuItem lookFeelItem;
  protected JMenuItem logItem;
  protected JMenuItem aboutItem;
  protected JMenuItem authorInfoItem;
  protected JMenu diagnosticMenu;
  protected JMenuItem[] languageItems;
  protected LaunchClassChooser classChooser;
  protected JPopupMenu popup = new JPopupMenu();
  protected Set openPaths = new HashSet();  // relative paths
  protected Launcher spawner;
  protected boolean previewing = false;
  protected boolean editorEnabled = true;  // enables editing current set
  protected Set changedFiles = new HashSet();  // relative paths
  protected MouseListener tabListener;
  protected boolean newNodeSelected = false;
  protected boolean selfContained = false;
  protected String jarBasePath = null; // non-null only if current xset is internal to jar
  protected String title;
  protected ArrayList tabs = new ArrayList();
  protected LauncherUndo undoManager;
  protected UndoableEditSupport undoSupport;
  protected String password;
  protected boolean pwRequiredToLoad;
  protected HyperlinkListener linkListener;
  protected boolean saveState = true;
  static {
    OSPRuntime.setAuthorMode(false);
    OSPRuntime.setLauncherMode(true);
    OSPRuntime.javaLookAndFeel = true; // default is Java L&F
    JFrame.setDefaultLookAndFeelDecorated(true);
    JDialog.setDefaultLookAndFeelDecorated(true);
  }

  /**
   * Constructs a bare Launcher with a splash screen.
   */
  public Launcher() {
    this(true);
  }

  /**
   * Constructs a bare Launcher with or without a splash screen.
   *
   * @param splash true to show the splash screen
   */
  public Launcher(boolean splash) {
    createGUI(splash);
    XML.setLoader(LaunchSet.class, new LaunchSet());
    // if statement added by W. Christian
    if(OSPRuntime.applet==null) {                             // never allow new VM if Launcher was instantiated by an applet
      // determine whether launching in new VM is possible; may not be possible in Java Web Start
      // create a test launch thread
      Runnable launchRunner = new Runnable() {
        public void run() {
          // prevent single VM in Vista and Linux
          if(System.getProperty("os.name", "")                //$NON-NLS-1$ //$NON-NLS-2$
              .toLowerCase().indexOf("vista")>-1) {   //$NON-NLS-1$
            return;
          }
          if(System.getProperty("os.name", "")                //$NON-NLS-1$ //$NON-NLS-2$
              .toLowerCase().indexOf("linux")>-1) {   //$NON-NLS-1$
            return;
          }
          try {
            Process proc = Runtime.getRuntime().exec("java"); //$NON-NLS-1$
            // if it made it this far, new VM is allowed (?)
            newVMAllowed = true;
            proc.destroy();
          } catch(Exception ex) {
            /** empty block */
          }
        }
      };
      try {
        Thread thread = new Thread(launchRunner);
        thread.start();
        thread.join(5000); // wait for join but don't wait over 5 seconds
      } catch(InterruptedException ex) {
        /** empty block */
      }
    }
    if(FontSizer.getLevel()!=0) {
      setFontLevel(FontSizer.getLevel());
    } else {
      refreshStringResources();
      refreshGUI();
    }
    // center frame on the screen
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (dim.width-frame.getBounds().width)/2;
    int y = (dim.height-frame.getBounds().height)/2;
    frame.setLocation(x, y);
  }

  /**
   * Constructs a Launcher and opens the specified xml file.
   *
   * @param fileName the name of the xml file
   */
  public Launcher(String fileName) {
    this(fileName, (fileName==null)||!fileName.startsWith("<?xml")); //$NON-NLS-1$
  }

  /**
   * Constructs a Launcher and opens the specified xml file.
   *
   * @param fileName the name of the xml file
   * @param splash true to show the splash screen
   */
  public Launcher(String fileName, boolean splash) {
    createGUI(splash);
    XML.setLoader(LaunchSet.class, new LaunchSet());
    String path = null;
    if(fileName==null) {
      // look for default file with launchJarName or defaultFileName
      if(OSPRuntime.getLaunchJarName()!=null) {
        fileName = XML.stripExtension(OSPRuntime.getLaunchJarName())+".xset"; //$NON-NLS-1$
        path = open(fileName);
      }
      if(path==null) {
        fileName = defaultFileName+".xset";                                  //$NON-NLS-1$
        path = open(fileName);
      }
      if(path==null) {
        fileName = defaultFileName+".xml";                                   //$NON-NLS-1$
        path = open(fileName);
      }
    } else {
      path = open(fileName);
    }
    if(FontSizer.getLevel()!=0) {
      setFontLevel(FontSizer.getLevel());
    } else {
      refreshStringResources();
      refreshGUI();
    }
    // center frame on the screen
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (dim.width-frame.getBounds().width)/2;
    int y = (dim.height-frame.getBounds().height)/2;
    frame.setLocation(x, y);
  }

  /**
   * Gets the size.
   *
   * @return the size Dimension object
   */
  public Dimension getSize() {
    return contentPane.getSize();
  }

  /**
   * Sets the size.
   *
   * @param dim the size Dimension
   */
  public void setSize(Dimension dim) {
    contentPane.setPreferredSize(dim);
    frame.pack();
  }

  /**
   * Sets the size.
   *
   * @param width the width
   * @param height the height
   */
  public void setSize(int width, int height) {
    contentPane.setPreferredSize(new Dimension(width, height));
    frame.pack();
  }

  /**
   * Gets the divider location.
   *
   * @return the divider location
   */
  public int getDivider() {
    return divider;
  }

  /**
   * Sets the divider location.
   *
   * @param loc the divider location
   */
  public void setDivider(int loc) {
    divider = loc;
    refreshGUI();
  }

  /**
   * Reports visibility of the Launcher frame.
   *
   * @return true if visible
   */
  public boolean isVisible() {
    return frame.isVisible();
  }

  /**
   * Sets the visibility of the Launcher frame.
   *
   * @param visible true to show the frame
   */
  public void setVisible(boolean visible) {
    frame.setVisible(visible);
  }

  /**
   * Sets navigation button visibility
   *
   * @param vis true to show navigation buttons
   */
  public void setNavigationVisible(boolean vis) {
    navigationVisible = vis;
    if(getSelectedNode().isButtonView()) {
      showButtonView(getSelectedNode());
    } else {
      showTabbedPaneView();
    }
  }

  /**
   * Clears the navigation history.
   */
  public void clearHistory() {
    undoManager.discardAllEdits();
    backButton.setEnabled(false);
    forwardButton.setEnabled(false);
  }

  /**
   * Sets the editorEnabled property.
   *
   * @param enabled true to enable editing from within Launcher
   */
  public void setEditorEnabled(boolean enabled) {
    editorEnabled = enabled;
  }

  /**
   * Sets the hyperlinksEnabled property for all nodes.
   *
   * @param enabled true to enable hyperlinks in html pages
   */
  public void setHyperlinksEnabled(boolean enabled) {
    for(int i = 0; i<getTabCount(); i++) {
      LaunchPanel tab = getTab(i);
      Enumeration e = tab.getRootNode().breadthFirstEnumeration();
      while(e.hasMoreElements()) {
        LaunchNode next = (LaunchNode) e.nextElement();
        setHyperlinksEnabled(next, enabled);
      }
    }
  }

  /**
   * Sets the hyperlinksEnabled property for a specified node.
   *
   * @param enabled true to enable hyperlinks in node's html pages
   */
  public void setHyperlinksEnabled(LaunchNode node, boolean enabled) {
    for(int i = 0; i<node.getHTMLCount(); i++) {
      LaunchNode.HTML html = node.getHTML(i);
      html.hyperlinksEnabled = enabled;
    }
  }

  /**
   * Gets the LaunchPanel in the selected tab. May return null.
   *
   * @return the LaunchPanel
   */
  public LaunchPanel getSelectedTab() {
    return(LaunchPanel) tabbedPane.getSelectedComponent();
  }

  /**
   * Sets the selected tab by name and returns its LaunchPanel,
   * or null if tab not found.
   *
   * @param path a path that starts with the name of the tab's root node
   * @return the LaunchPanel containing the root
   */
  public LaunchPanel setSelectedTab(String path) {
    if(path==null) {
      return null;
    }
    for(int i = 0; i<tabbedPane.getTabCount(); i++) {
      LaunchPanel tab = getTab(i);
      if(path.startsWith(tab.getRootNode().name)) {
        tabbedPane.setSelectedComponent(tab);
        return tab;
      }
    }
    return null;
  }

  /**
   * Sets the selected tab and returns its LaunchPanel,
   * or null if tab not found.
   *
   * @param tab the tab to select
   * @return the LaunchPanel containing the root
   */
  public LaunchPanel setSelectedTab(LaunchPanel tab) {
    tabbedPane.setSelectedComponent(tab);
    return (tab==tabbedPane.getSelectedComponent())
           ? tab
           : null;
  }

  /**
   * Gets the selected launch node. May return null.
   *
   * @return the selected launch node
   */
  public LaunchNode getSelectedNode() {
    if(getSelectedTab()==null) {
      selectedNode = null;
    } else {
      selectedNode = getSelectedTab().getSelectedNode();
    }
    return selectedNode;
  }

  /**
   * Sets the selected node by path and returns the node,
   * or null if node not found. Path is node names separated by / or \
   *
   * @param path the path of the node
   * @return the LaunchNode
   */
  public LaunchNode setSelectedNode(String path) {
    return setSelectedNode(path, 0, null);
  }

  /**
   * Sets the selected node by path and returns the node,
   * or null if node not found. Path is node names separated by / or \
   *
   * @param path the path of the node
   * @param pageNumber the html page number
   * @return the LaunchNode
   */
  public LaunchNode setSelectedNode(String path, int pageNumber) {
    return setSelectedNode(path, pageNumber, null);
  }

  /**
   * Sets the selected node by path and returns the node,
   * or null if node not found. Path is node names separated by / or \
   *
   * @param path the path of the node
   * @param pageNumber the html page number
   * @return the LaunchNode
   */
  public LaunchNode setSelectedNode(String path, int pageNumber, URL url) {
    if(path==null) {
      return null;
    }
    path = XML.forwardSlash(path);
    setSelectedTab(path);
    LaunchPanel tab = getSelectedTab();
    if(tab==null) {
      return null;
    }
    Enumeration e = tab.getRootNode().breadthFirstEnumeration();
    while(e.hasMoreElements()) {
      LaunchNode node = (LaunchNode) e.nextElement();
      if(path.equals(node.getPathString())) { // found node
        tab.setSelectedNode(node, pageNumber, url);
        return node;
      }
    }
    return null;
  }

  /**
   * Sets the selected node by path and returns the node,
   * or null if node not found. Path is node names separated by / or \
   *
   * @param keywords the keywords of the node
   * @param pageNumber the html page to dosplay
   * @return the LaunchNode
   */
  public LaunchNode setSelectedNodeByKey(String keywords, int pageNumber) {
    if((keywords==null)||keywords.equals("")) { //$NON-NLS-1$
      return null;
    }
    for(int i = 0; i<getTabCount(); i++) {
      LaunchPanel tab = getTab(i);
      Enumeration e = tab.getRootNode().breadthFirstEnumeration();
      while(e.hasMoreElements()) {
        LaunchNode node = (LaunchNode) e.nextElement();
        if(keywords.equals(node.keywords)) { // found node
          setSelectedTab(tab);
          tab.setSelectedNode(node, pageNumber);
          return node;
        }
      }
    }
    return null;
  }

  /**
   * Gets the root node of the selected launch tree. May return null.
   *
   * @return the root node
   */
  public LaunchNode getRootNode() {
    if(getSelectedTab()==null) {
      return null;
    }
    return getSelectedTab().getRootNode();
  }

  /**
   * Gets the current number of tabs (LaunchPanels)
   *
   * @return the tab count
   */
  public int getTabCount() {
    return tabbedPane.getTabCount();
  }

  /**
   * Gets the launch panel at the specified tab index. May return null.
   *
   * @param i the tab index
   * @return the launch panel
   */
  public LaunchPanel getTab(int i) {
    if(i>=tabbedPane.getTabCount()) {
      return null;
    }
    return(LaunchPanel) tabbedPane.getComponentAt(i);
  }

  /**
   * Gets the html tab at the specified index.
   *
   * @param i the tab index
   * @return the html tab
   */
  public HTMLPane getHTMLTab(int i) {
    while(i>=htmlTabList.size()) {
      htmlTabList.add(new HTMLPane());
    }
    return(HTMLPane) htmlTabList.get(i);
  }

  /**
   * Gets the content pane.
   *
   * @return the content pane
   */
  public Container getContentPane() {
    return contentPane;
  }

  /**
   * Opens an xml document and selects a tab and/or node specified by name.
   * args[0] may be a relative path, absolute path, or self-contained xml string.
   * args[1] may be a tab name and/or node path.
   *
   * @param args the arguments
   * @return the absolute path, or null if failed or currently open
   */
  public String open(String[] args) {
    if((args==null)||(args.length==0)) {
      return null;
    }
    // arg[0] is file name or xml string to open
    String path = open(args[0]);
    // arg[1] is tab name or tab/node names
    if((args.length>1)&&(args[1]!=null)) {
      setSelectedNode(args[1]);
    }
    return path;
  }

  /**
   * Opens an xml document specified by name and displays it in a new tab
   * (or selects the tab if already open).
   * Name may be a relative path, absolute path, or self-contained xml string.
   *
   * @param name the name
   * @return the absolute path, or null if failed or currently open
   */
  public String open(String name) {
    if((name==null)||name.equals("")) { //$NON-NLS-1$
      return null;
    }
    // add search paths to resource loader in last-to-first order
    ResourceLoader.addSearchPath(resourcesPath);
    ResourceLoader.addSearchPath(tabSetBasePath);
    String path = name;
    String absolutePath = ""; //$NON-NLS-1$
    XMLControlElement control = new XMLControlElement();
    // look for self-contained xml string (commonly used for previews)
    if(name.startsWith("<?xml")) { //$NON-NLS-1$
      control.readXML(name);
      if(control.failedToRead()) {
        return null;
      }
    }
    if(control.getObjectClassName().equals(Object.class.getName())) { // did not read xml string
      // read the named file and get its absolute path
    	// look first in launch jar directory (added by D Brown 2007-11-05)
    	String jarBase = OSPRuntime.getLaunchJarDirectory();
      absolutePath = control.read(XML.getResolvedPath(path, jarBase));
      if(control.failedToRead()) {
      	// look using relative path
      	absolutePath = control.read(path);
      }
    }
    if(control.failedToRead()) {
      String jar = XML.stripExtension(OSPRuntime.getLaunchJarName());
      if(!name.startsWith(defaultFileName)&&(jar!=null)&&!name.startsWith(jar)) {
        OSPLog.info(LaunchRes.getString("Log.Message.InvalidXML")+" "+name);                                        //$NON-NLS-1$ //$NON-NLS-2$
        JOptionPane.showMessageDialog(null, LaunchRes.getString("Dialog.InvalidXML.Message")+" \""+name+"\"",       //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
                                      LaunchRes.getString("Dialog.InvalidXML.Title"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
      }
      return null;
    }
    OSPLog.fine(name);
    // absolute and return paths are empty if name is xml string
    String returnPath = XML.forwardSlash(absolutePath);
    // set non-null jar base path if opening from a jar or zip file
    String jarName = LaunchRes.getString("Splash.Label.Internal");              //$NON-NLS-1$
    int zip = Math.max(returnPath.indexOf("jar!"), returnPath.indexOf("zip!")); //$NON-NLS-1$ //$NON-NLS-2$
    if(zip>-1) {
      jarName = XML.getName(returnPath.substring(0, zip+3));
      String s = XML.getDirectoryPath(returnPath.substring(0, zip+3));
      jarBasePath = s.equals("")   //$NON-NLS-1$
                    ? XML.forwardSlash(System.getProperty("user.dir", "")) //$NON-NLS-1$ //$NON-NLS-2$
                    : s;
      returnPath = returnPath.substring(zip+5);
    }
    else jarBasePath = null;
    // check for password
    String prevPassword = password;
    if(control.getPassword()!=null) {
      boolean pwRequired = control.getBoolean("pw_required_by_launcher"); //$NON-NLS-1$
      if((Launcher.this instanceof LaunchBuilder)||pwRequired) {
        if(Password.verify(control.getPassword(), returnPath)) {
          password = control.getPassword();
        }
      }
    }
    Class type = control.getObjectClass();
    if(LaunchSet.class.equals(type)) {
      // show name and icon in splash label if splashing
      if(!returnPath.equals("")&&(splashDialog!=null)&&splashDialog.isVisible()) {               //$NON-NLS-1$
        Resource res = ResourceLoader.getResource(path);
        String loading = LaunchRes.getString("Log.Message.Loading")+": ";                        //$NON-NLS-1$ //$NON-NLS-2$
        if(res.getFile()!=null) {                                                                // external file
          OSPLog.info(loading+res.getAbsolutePath());
          // pale blue background and magenta icon if external file
          splashDialog.getContentPane().setBackground(new Color(242, 242, 255));
          splashPathLabel.setIcon(magentaFileIcon);
          splashPathLabel.setText(loading+name);
        } else {
          loading = LaunchRes.getString("Log.Message.LoadingFrom")+" "+                          //$NON-NLS-1$ //$NON-NLS-2$
            jarName+": "+name;                                                                   //$NON-NLS-1$
          boolean internal = jarName.equals(LaunchRes.getString("Splash.Label.Internal"))||      //$NON-NLS-1$
                      jarName.equals(OSPRuntime.getLaunchJarName());
          if(internal) {
            // pale yellow background and green icon if internal resource
            splashDialog.getContentPane().setBackground(new Color(255, 255, 228));
            splashPathLabel.setIcon(greenFileIcon);
          } else {
            // pale blue background and magenta icon if external resource
            splashDialog.getContentPane().setBackground(new Color(242, 242, 255));
            splashPathLabel.setIcon(magentaFileIcon);
          }
          splashPathLabel.setText(loading);
          loading = LaunchRes.getString("Log.Message.Loading")+": ";                             //$NON-NLS-1$ //$NON-NLS-2$
          OSPLog.info(loading+res.getAbsolutePath());
        }
      }
      // close all open tabs
      if(!removeAllTabs()) {
        password = prevPassword;
        return null;
      }
      // set paths
      tabSetName = XML.getName(returnPath);
      // set static tabset base path unless reading xml string (return path "")
      if(!returnPath.equals("")) {                                                               //$NON-NLS-1$
        if(jarBasePath!=null) { // xset loaded from internal jar entry
          tabSetBasePath = "";                                                                   //$NON-NLS-1$
        } else {
          tabSetBasePath = XML.getDirectoryPath(returnPath);
        }
      }
      // load the xset data
      OSPLog.finest(LaunchRes.getString("Log.Message.Loading")+": "+                             //$NON-NLS-1$ //$NON-NLS-2$
        returnPath);
      // don't post edits during loading
      boolean post = postEdits;
      postEdits = false;
      LaunchSet tabset = new LaunchSet(this, tabSetName);
      control.loadObject(tabset);
      if(tabset.failedToLoad) {
        if(tabSetName.equals(XML.getName(returnPath))) {
          tabSetName = null;
        }
        returnPath = null;
      } else if((splashDialog!=null)&&splashDialog.isVisible()) {
        LaunchNode root = getRootNode();
        if((root!=null)&&!root.getAuthor().trim().equals("")) {                                  //$NON-NLS-1$
          String by = LaunchRes.getString("Label.Author")+": ";                                  //$NON-NLS-1$ //$NON-NLS-2$
          creditsLabel.setText(by+root.getAuthor());
        }
      }
      changedFiles.clear();
      OSPLog.fine("returning "+returnPath);                                                      //$NON-NLS-1$
      postEdits = post;
      return returnPath;
    } else if(LaunchNode.class.equals(type)) {
      // load the xml file data
      OSPLog.finest(LaunchRes.getString("Log.Message.Loading")+": "+path);                       //$NON-NLS-1$ //$NON-NLS-2$
      LaunchNode node = new LaunchNode(LaunchRes.getString("NewNode.Name"));                     //$NON-NLS-1$
      // assign file name BEFORE loading node
      node.setFileName(XML.getPathRelativeTo(returnPath, tabSetBasePath));
      control.loadObject(node);
      String tabName = getDisplayName(returnPath);
      // if node is already open, select the tab and return null
      for(int i = 0; i<tabbedPane.getComponentCount(); i++) {
        if(tabbedPane.getTitleAt(i).equals(tabName)) {
          LaunchNode root = ((LaunchPanel) tabbedPane.getComponent(i)).getRootNode();
          if(root.matches(node)) {
            tabbedPane.setSelectedIndex(i);
            return null;
          }
        }
      }
      // don't post edits during loading
      postEdits = false;
      // if no tabset is open, then create new one
      if(tabSetName==null) {
        tabSetName = LaunchRes.getString("Tabset.Name.New");                                     //$NON-NLS-1$
        title = null;
        tabSetBasePath = XML.getDirectoryPath(returnPath);
        editorEnabled = true;
      }
      addTab(node);
      Enumeration e = node.breadthFirstEnumeration();
      while(e.hasMoreElements()) {
        LaunchNode next = (LaunchNode) e.nextElement();
        next.setLaunchClass(next.launchClassName);
      }
      postEdits = true;
      return returnPath;                                                                         // successfully opened and added to tab
    } else {
      OSPLog.info(LaunchRes.getString("Log.Message.NotLauncherFile"));                           //$NON-NLS-1$
      if(name.length()>20) {
        name = name.substring(0, 20)+"...";                                                      //$NON-NLS-1$
      }
      JOptionPane.showMessageDialog(null, LaunchRes.getString("Dialog.NotLauncherFile.Message")+ //$NON-NLS-1$
        " \""+name+"\"",                                                                         //$NON-NLS-1$ //$NON-NLS-2$
        LaunchRes.getString("Dialog.NotLauncherFile.Title"),                                     //$NON-NLS-1$
        JOptionPane.WARNING_MESSAGE);
    }
    return null;
  }

  // ______________________________ protected methods _____________________________

  /**
   * Creates a LaunchPanel with the specified root and adds it to a new tab.
   *
   * @param root the root node
   * @return true if tab was added
   */
  protected boolean addTab(LaunchNode root) {
    final LaunchPanel tab = new LaunchPanel(root, this);
    tabs.add(tab);
    if(root.isHiddenInLauncher()&&!(this instanceof LaunchBuilder)) {
      return false;
    }
    tab.tree.setCellRenderer(new LaunchRenderer());
    tab.tree.addTreeSelectionListener(new TreeSelectionListener() {

      public void valueChanged(TreeSelectionEvent e) {
        newNodeSelected = true;
        refreshGUI();
      }
    });
    tab.tree.addMouseListener(new MouseAdapter() {

      public void mousePressed(MouseEvent e) {
        if(splashDialog!=null) {
          splashDialog.dispose();
        }
        tab.tree.removeMouseListener(this);
      }
    });
    tab.tree.addMouseListener(new MouseAdapter() {

      public void mousePressed(MouseEvent e) {
        popup.removeAll();
        handleMousePressed(e, tab);
      }
    });
    tabbedPane.addTab(root.toString(), tab);
    tabbedPane.setSelectedComponent(tab);
    tab.setSelectedNode(root);
    if(!root.tooltip.equals("")) { //$NON-NLS-1$
      tabbedPane.setToolTipTextAt(tabbedPane.getSelectedIndex(), root.tooltip);
    }
    tab.dataPanel.addComponentListener(new ComponentAdapter() {

      public void componentResized(ComponentEvent e) {
        divider = tab.splitPane.getDividerLocation();
      }
    });
    return true;
  }

  /**
   * Displays a button view of the specified node.
   *
   * @param node the node
   */
  protected void showButtonView(final LaunchNode node) {
    LaunchNode.HTML html = node.getHTML(0);
    if((html!=null)&&(html.url!=null)) {
      setLinksEnabled(textPane, html.hyperlinksEnabled);
      try {
        if(html.url.getContent()!=null) {
          final java.net.URL url = html.url;
          Runnable runner = new Runnable() {

            public void run() {
              try {
                textPane.setPage(url);
              } catch(IOException ex) {
                OSPLog.fine(LaunchRes.getString("Log.Message.BadURL")+" "+url); //$NON-NLS-1$ //$NON-NLS-2$
              }
            }
          };
          SwingUtilities.invokeLater(runner);
        }
      } catch(IOException ex) {
        OSPLog.finest(LaunchRes.getString("Log.Message.BadURL")+" "+html.url);  //$NON-NLS-1$ //$NON-NLS-2$
        if(showText) {
          textPane.setContentType(LaunchPanel.TEXT_TYPE);
          textPane.setText(node.description);
        }
      }
    } else if(showText) {
      textPane.setContentType(LaunchPanel.TEXT_TYPE);
      textPane.setText(node.description);
    }
    contentPane.removeAll();
    contentPane.add(textScroller, BorderLayout.CENTER);
    // add buttons (child nodes) to SOUTH
    JPanel box = new JPanel();
    contentPane.add(box, BorderLayout.SOUTH);
    ActionListener openAction = new AbstractAction() {

      public void actionPerformed(ActionEvent e) {
        // open linked file and node
        int i = Integer.parseInt(e.getActionCommand());
        LaunchNode child = (LaunchNode) node.getChildAt(i);
        if(child.getLaunchClass()==null) {
          String[] prevArgs = undoManager.getLauncherState();
          if(open(child.args)!=null) {
            if(prevArgs!=null) {
              UndoableEdit edit = undoManager.new LoadEdit(child.args, prevArgs);
              undoSupport.postEdit(edit);
            }
            refreshGUI();
          }
        } else {
          child.launch();
        }
      }
    };
    for(int i = 0; i<node.getChildCount(); i++) {
      LaunchNode child = (LaunchNode) node.getChildAt(i);
      JButton button = new JButton(child.name);
      button.addActionListener(openAction);
      button.setActionCommand(String.valueOf(i));
      button.setToolTipText(child.tooltip);
      box.add(button);
    }
    frame.validate();
    refreshGUI();
    frame.repaint();
  }

  /**
   * Displays the standard tabbed pane view.
   */
  protected void showTabbedPaneView() {
    contentPane.removeAll();
    if(navigationVisible&&!(this instanceof LaunchBuilder)) {
      contentPane.add(navbar, BorderLayout.NORTH);
    }
    contentPane.add(tabbedPane, BorderLayout.CENTER);
    frame.validate();
    refreshGUI();
    frame.repaint();
  }

  /**
   * Opens an xml file selected with a chooser.
   *
   * @return the name of the opened file
   */
  protected String open() {
    getXMLChooser().setFileFilter(launcherFileFilter);
    int result = getXMLChooser().showOpenDialog(null);
    if(result==JFileChooser.APPROVE_OPTION) {
      File file = getXMLChooser().getSelectedFile();
      String fileName = XML.forwardSlash(file.getAbsolutePath());
      OSPRuntime.chooserDir = XML.getDirectoryPath(fileName);
      return open(fileName);
    }
    return null;
  }

  /**
   * Removes the selected tab.
   *
   * @return true if the tab was removed
   */
  protected boolean removeSelectedTab() {
    int i = tabbedPane.getSelectedIndex();
    if(i<0) {
      return false;
    }
    String[] prevArgs = undoManager.getLauncherState();
    tabs.remove(getTab(i));
    tabbedPane.removeTabAt(i);
    previousNode = selectedNode;
    newNodeSelected = true;
    if(tabbedPane.getTabCount()==0) {
      tabSetName = null;
      title = null;
      if(prevArgs!=null) {
        // null new args indicate not redoable
        LauncherUndo.LoadEdit edit = undoManager.new LoadEdit(null, prevArgs);
        undoSupport.postEdit(edit);
      }
    }
    refreshGUI();
    return true;
  }

  /**
   * Removes all tabs.
   *
   * @return true if all tabs were removed
   */
  protected boolean removeAllTabs() {
    int n = tabbedPane.getTabCount();
    if(n==0) {
      return true;
    }
    boolean post = postEdits;
    postEdits = false;
    for(int i = n-1; i>=0; i--) {
      tabbedPane.removeTabAt(i);
    }
    if(tabbedPane.getTabCount()==0) {
      tabSetName = null;
      title = null;
      password = null;
    } else {
      previousNode = selectedNode;
      newNodeSelected = true;
    }
    refreshGUI();
    tabs.clear();
    for(int i = 0; i<tabbedPane.getTabCount(); i++) {
      tabs.add(tabbedPane.getComponentAt(i));
    }
    postEdits = post;
    return tabbedPane.getTabCount()==0;
  }

  /**
   * Refreshes string resources.
   */
  protected void refreshStringResources() {
    fileMenu.setText(LaunchRes.getString("Menu.File"));              //$NON-NLS-1$
    displayMenu.setText(LaunchRes.getString("Menu.Display"));        //$NON-NLS-1$
    openItem.setText(LaunchRes.getString("Menu.File.Open"));         //$NON-NLS-1$
    closeTabItem.setText(LaunchRes.getString("Menu.File.CloseTab")); //$NON-NLS-1$
    closeAllItem.setText(LaunchRes.getString("Menu.File.CloseAll")); //$NON-NLS-1$
    editItem.setText(LaunchRes.getString("Menu.File.Edit"));         //$NON-NLS-1$
    backItem.setText(LaunchRes.getString("Menu.File.Back"));         //$NON-NLS-1$
    helpMenu.setText(LaunchRes.getString("Menu.Help"));              //$NON-NLS-1$
    logItem.setText(LaunchRes.getString("Menu.Help.MessageLog"));    //$NON-NLS-1$
    inspectItem.setText(LaunchRes.getString("Menu.Help.Inspect"));   //$NON-NLS-1$
    String s = XML.getSimpleClassName(this.getClass());
    String about = LaunchRes.getString("Menu.Help.About") //$NON-NLS-1$
                   +" "+s+"...";                          //$NON-NLS-1$ //$NON-NLS-2$
    aboutItem.setText(about);
    String authorInfo = LaunchRes.getString("Menu.Help.AuthorInfo")+"..."; //$NON-NLS-1$ //$NON-NLS-2$
    authorInfoItem.setText(authorInfo);
    diagnosticMenu.setText(LaunchRes.getString("Menu.Help.Diagnostics")); //$NON-NLS-1$
    if(exitItem!=null) {
      exitItem.setText(LaunchRes.getString("Menu.File.Exit")); //$NON-NLS-1$
    }
    languageMenu.setText(LaunchRes.getString("Menu.Display.Language"));         //$NON-NLS-1$
    sizeUpItem.setText(LaunchRes.getString("Menu.Display.IncreaseFontSize"));   //$NON-NLS-1$
    sizeDownItem.setText(LaunchRes.getString("Menu.Display.DecreaseFontSize")); //$NON-NLS-1$
    lookFeelItem.setText(LaunchRes.getString("Menu.Display.LookAndFeel"));      //$NON-NLS-1$
    if(openFromJarMenu!=null) {
      openFromJarMenu.setText(LaunchRes.getString("Menu.File.OpenFromJar")); //$NON-NLS-1$
    }
    Locale[] locales = OSPRuntime.getInstalledLocales();
    for(int i = 0; i<locales.length; i++) {
      if(locales[i].getLanguage().equals(LaunchRes.locale.getLanguage())) {
        languageItems[i].setSelected(true);
      }
    }
  }

  /**
   * Refreshes the GUI.
   */
  protected void refreshGUI() {
    // make frame title
    String name = (title==null)
                  ? tabSetName
                  : title;
    if(name==null) {
      name = LaunchRes.getString("Frame.Title");           //$NON-NLS-1$
    } else {
      name = LaunchRes.getString("Frame.Title")+": "+name; //$NON-NLS-1$ //$NON-NLS-2$
    }
    frame.setTitle(name);
    // set tab properties
    LaunchPanel tab = getSelectedTab();
    if(tab!=null) {
      getSelectedTab().splitPane.setDividerLocation(divider);
    }
    // update undo/redo buttons
    backButton.setEnabled(undoManager.canUndo());
    forwardButton.setEnabled(undoManager.canRedo());
    // rebuild file menu
    fileMenu.removeAll();
    if(undoManager.canReload()) {
      fileMenu.add(backItem);
    }
    if((OSPRuntime.applet==null)) {
      if(fileMenu.getItemCount()>0) {
        fileMenu.addSeparator();
      }
      fileMenu.add(openItem);
    }
    if(openFromJarMenu!=null) {
      fileMenu.add(openFromJarMenu);
    }
    if(OSPRuntime.applet!=null) { // added by W. Christian
      fileMenu.add(hideItem);
      return;
    }
    if(tab!=null) {
      if(fileMenu.getItemCount()>0) {
        fileMenu.addSeparator();
      }
      boolean showCloseTab = true;
      if(getClass()==Launcher.class) {
        if(tab.getRootNode().isButtonView()) {
          showCloseTab = false;
          name = LaunchRes.getString("Frame.Title")+": "+                //$NON-NLS-1$ //$NON-NLS-2$
            tab.getRootNode().name;
        } else if(tabbedPane.getTabCount()==1) {
          showCloseTab = false;
        }
      }
      if(showCloseTab) {
        fileMenu.add(closeTabItem);
        closeAllItem.setText(LaunchRes.getString("Menu.File.CloseAll")); //$NON-NLS-1$
      } else {
        closeAllItem.setText(LaunchRes.getString("MenuItem.Close"));     //$NON-NLS-1$
      }
      fileMenu.add(closeAllItem);
    }
    if(editorEnabled && !OSPRuntime.isWebStart()) {
      fileMenu.addSeparator();
      fileMenu.add(editItem);
    }
    // added by W. Christian to capture screen
    JMenu printMenu = new JMenu(DisplayRes.getString("DrawingFrame.Print_menu_title"));                          //$NON-NLS-1$
    JMenuItem printFrameItem = new JMenuItem(DisplayRes.getString("DrawingFrame.PrintFrame_menu_item"));         //$NON-NLS-1$
    JMenuItem saveFrameAsEPSItem = new JMenuItem(DisplayRes.getString("DrawingFrame.SaveFrameAsEPS_menu_item")); //$NON-NLS-1$
    printMenu.add(printFrameItem);
    printMenu.add(saveFrameAsEPSItem);
    fileMenu.add(printMenu);
    // print action
    printFrameItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        PrintUtils.printComponent(frame);
      }
    });
    // save as EPS action
    saveFrameAsEPSItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        try {
          PrintUtils.saveComponentAsEPS(frame);
        } catch(IOException ex) {

        /** empty block */
        }
      }
    });
    // end W. Christian code
    fileMenu.addSeparator();
    if(exitItem!=null) {
      fileMenu.add(exitItem);
    }
    inspectItem.setEnabled((password==null)||(this instanceof LaunchBuilder));
    sizeDownItem.setEnabled(fileMenu.getFont().getSize()>baseMenuFontSize);
  }

  /**
   * Creates applet GUI items.
   */
  private void appletGUI() { // added by W. Christian
    hideItem = new JMenuItem(LaunchRes.getString("Menu.File.Hide")); //$NON-NLS-1$
    hideItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        exit();
      }
    });
  }

  /**
   * Creates the GUI.
   *
   * @param splash true to show the splash screen
   */
  protected void createGUI(boolean splash) {
  	// load icons--this will also set jarpath if any
    String imageFile = "/org/opensourcephysics/resources/tools/images/launch.gif"; //$NON-NLS-1$
    launchIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/launched.gif";    //$NON-NLS-1$
    launchedIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/singleton.gif";   //$NON-NLS-1$
    singletonIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/nofile.gif";      //$NON-NLS-1$
    noFileIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/greenfile.gif";   //$NON-NLS-1$
    greenFileIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/magentafile.gif"; //$NON-NLS-1$
    magentaFileIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/link.gif";        //$NON-NLS-1$
    linkIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/html.gif";        //$NON-NLS-1$
    htmlIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/launchempty.gif"; //$NON-NLS-1$
    launchEmptyIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/launchEJS.gif";   //$NON-NLS-1$
    ejsIcon = ResourceLoader.getIcon(imageFile);
    // create applet GUI items
    appletGUI(); // added by W. Christian
    // get existing frames
    Frame[] frames = Frame.getFrames();
    for(int i = 0, n = frames.length; i<n; i++) {
      existingFrames.add(frames[i]);
    }
    // instantiate the OSPLog and Translator
    OSPLog.getOSPLog();
    OSPRuntime.getTranslator();
    // set up the undo system
    undoManager = new LauncherUndo(this);
    undoSupport = new UndoableEditSupport();
    undoSupport.addUndoableEditListener(undoManager);
    // create the frame
    frame = new LauncherFrame();
    existingFrames.add(frame);
    if(splash&&(OSPRuntime.applet==null)) {
      splash();
    }
    // create xml inspector
    xmlInspector = new JDialog(frame, false);
    xmlInspector.setSize(new java.awt.Dimension(600, 300));
    tableInspector = new XMLTableInspector(true, false);
    // center inspectors on screen
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (dim.width-xmlInspector.getBounds().width)/2;
    int y = (dim.height-xmlInspector.getBounds().height)/2;
    xmlInspector.setLocation(x, y);
    x = (dim.width-tableInspector.getBounds().width)/2;
    y = (dim.height-tableInspector.getBounds().height)/2;
    tableInspector.setLocation(x, y);
    contentPane = new JPanel(new BorderLayout());
    contentPane.setPreferredSize(new Dimension(wInit, hInit));
    frame.setContentPane(contentPane);
    frame.setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    // create navigation bar
    navbar = new JToolBar();
    navbar.setFloatable(false);
    navbar.setBorder(BorderFactory.createEtchedBorder());
    imageFile = "/org/opensourcephysics/resources/tools/images/nav_open.gif";   //$NON-NLS-1$
    navOpenIcon = ResourceLoader.getIcon(imageFile);
    imageFile = "/org/opensourcephysics/resources/tools/images/nav_closed.gif"; //$NON-NLS-1$
    navClosedIcon = ResourceLoader.getIcon(imageFile);
    navButton = new JButton(navOpenIcon);
    navButton.setBorder(BorderFactory.createEmptyBorder());
    navButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        System.out.println(navbar.getComponentCount());
        if(navbar.getComponentCount()>1) {
          navbar.remove(backButton);
          navbar.remove(forwardButton);
          navButton.setIcon(navClosedIcon);
        } else {
          navbar.add(backButton);
          navbar.add(forwardButton);
          navButton.setIcon(navOpenIcon);
        }
        navbar.revalidate();
      }
    });
    navbar.add(navButton);
    imageFile = "/org/opensourcephysics/resources/tools/images/undo.gif"; //$NON-NLS-1$
    backButton = new JButton(ResourceLoader.getIcon(imageFile));
    //              backButton = new JButton("        ", launchIcon);
    backButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        undoManager.undo();
        refreshGUI();
      }
    });
    navbar.add(backButton);
    imageFile = "/org/opensourcephysics/resources/tools/images/redo.gif"; //$NON-NLS-1$
    forwardButton = new JButton(ResourceLoader.getIcon(imageFile));
    forwardButton.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        undoManager.redo();
        refreshGUI();
      }
    });
    navbar.add(forwardButton);
    backButton.setEnabled(false);
    forwardButton.setEnabled(false);
    // create text pane and scroller for button view
    textPane = new JTextPane();
    textPane.setEditable(false);
    textScroller = new JScrollPane(textPane);
    // create tabbed pane
    tabbedPane = new JTabbedPane(SwingConstants.BOTTOM);
    contentPane.add(tabbedPane, BorderLayout.CENTER);
    tabbedPane.addChangeListener(new ChangeListener() {

      public void stateChanged(ChangeEvent e) {
        previousNode = selectedNode;
        selectedNode = getSelectedNode();
        newNodeSelected = true;
        LaunchNode root = getRootNode();
        if((root!=null)&&root.isButtonView()&&(Launcher.this.getClass()==Launcher.class)) {
          showButtonView(root);
        } else {
          showTabbedPaneView();
          LaunchPanel tab = getSelectedTab();
          if(tab!=null) {
            if(getSelectedNode()!=null) {
              tab.displayHTML(selectedNode);
            } else {
              tab.displayHTML(root);
            }
          }
        }
        if((previousNode!=null)&&(selectedNode!=null)) {
          // set html properties of newly selected node
          if((selectedNode.htmlURL==null)&&!selectedNode.htmlData.isEmpty()) {
            int page = Math.max(0, selectedNode.htmlTabNumber);
            LaunchNode.HTML htmlData = (LaunchNode.HTML) selectedNode.htmlData.get(page);
            selectedNode.htmlURL = htmlData.url;
            selectedNode.htmlTabNumber = page;
          }
          // post undoable NavEdit
          if(postEdits) {
            UndoableEdit edit = undoManager.new NavEdit(previousNode, selectedNode);
            undoSupport.postEdit(edit);
          }
        }
        refreshGUI();
      }
    });
    tabListener = new MouseAdapter() {

      public void mousePressed(MouseEvent e) {
        if(contentPane.getTopLevelAncestor()!=frame) {
          return;
        }
        if(e.isPopupTrigger()||(e.getButton()==MouseEvent.BUTTON3)
            ||(e.isControlDown()&&(System.getProperty("os.name", "").indexOf("Mac")>-1))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          // make popup and add items
          JPopupMenu popup = new JPopupMenu();
          JMenuItem item = new JMenuItem(LaunchRes.getString("MenuItem.Close"));            //$NON-NLS-1$
          item.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
              removeSelectedTab();
            }
          });
          popup.add(item);
          popup.show(tabbedPane, e.getX(), e.getY()+8);
        }
      }
    };
    tabbedPane.addMouseListener(tabListener);
    // create the menu bar
    final JMenuBar menubar = new JMenuBar();
    fileMenu = new JMenu();
    fileMenu.addMouseListener(new MouseAdapter() {

      public void mousePressed(MouseEvent e) {
        if(splashDialog!=null) {
          splashDialog.dispose();
        }
        fileMenu.removeMouseListener(this);
      }
    });
    menubar.add(fileMenu);
    openItem = new JMenuItem();
    int mask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    openItem.setAccelerator(KeyStroke.getKeyStroke('O', mask));
    openItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        String[] prevArgs = undoManager.getLauncherState();
        if(prevArgs!=null) {
          String fileName = open();
          if(fileName!=null) {
            String[] args = new String[]{fileName};
            LauncherUndo.LoadEdit edit = undoManager.new LoadEdit(args, prevArgs);
            undoSupport.postEdit(edit);
            refreshGUI();
          }
        } else {
          open();
          refreshGUI();
        }
      }
    });
    closeTabItem = new JMenuItem();
    closeTabItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        removeSelectedTab();
      }
    });
    closeAllItem = new JMenuItem();
    closeAllItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        String[] prevArgs = undoManager.getLauncherState();
        if(removeAllTabs()&&(prevArgs!=null)) {
          // null new args indicate not redoable
          LauncherUndo.LoadEdit edit = undoManager.new LoadEdit(null, prevArgs);
          undoSupport.postEdit(edit);
        }
        refreshGUI();
      }
    });
    editItem = new JMenuItem();
    editItem.setAccelerator(KeyStroke.getKeyStroke('E', mask));
    if(OSPRuntime.isWebStart()){
      editItem.setEnabled(false);
    }
    editItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        if((password!=null)&&!pwRequiredToLoad&&!Password.verify(password, tabSetName)) {
          return;
        }
        if(previewing) {
          previewing = false;
          if(Launcher.this.spawner!=null) {
            LaunchNode node = Launcher.this.getSelectedNode();
            if(node!=null) {
              Launcher.this.spawner.setSelectedNode(node.getPathString());
            }
          }
          exit(); // edit previews by exiting to builder
        } else {
          LaunchBuilder builder;
          if(tabSetName==null) {
            builder = new LaunchBuilder(false);
            builder.newItem.doClick();
          } else {
            LaunchSet tabset = new LaunchSet(Launcher.this, tabSetName);
            XMLControlElement control = new XMLControlElement(tabset);
            // set null password so LaunchBuilder opens without verification
            control.setPassword(null);
            builder = new LaunchBuilder(control.toXML());
            LaunchNode node = Launcher.this.getSelectedNode();
            if(node!=null) {
              builder.setSelectedNode(node.getPathString());
            }
            builder.tabSetName = tabSetName;
            builder.password = password;
          }
          builder.spawner = Launcher.this;
          builder.jarBasePath = jarBasePath;
          Point p = Launcher.this.frame.getLocation();
          builder.frame.setLocation(p.x+24, p.y+24);
          builder.frame.setVisible(true);
          builder.frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        }
      }
    });
    backItem = new JMenuItem();
    backItem.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_LEFT, mask));
    backItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        undoManager.undo();
        refreshGUI();
      }
    });
    displayMenu = new JMenu();
    menubar.add(displayMenu);
    // language menu
    LaunchRes.addPropertyChangeListener("locale", new PropertyChangeListener() { //$NON-NLS-1$

      public void propertyChange(PropertyChangeEvent e) {
        refreshStringResources();
        refreshGUI();
      }
    });
    languageMenu = new JMenu();
    final Locale[] locales = OSPRuntime.getInstalledLocales();
    Action languageAction = new AbstractAction() {

      public void actionPerformed(ActionEvent e) {
        String language = e.getActionCommand();
        OSPLog.finest("setting language to "+language); //$NON-NLS-1$
        for(int i = 0; i<locales.length; i++) {
          if(language.equals(locales[i].getDisplayName())) {
            LaunchRes.setLocale(locales[i]);
            return;
          }
        }
      }
    };
    ButtonGroup languageGroup = new ButtonGroup();
    languageItems = new JMenuItem[locales.length];
    for(int i = 0; i<locales.length; i++) {
      languageItems[i] = new JRadioButtonMenuItem(locales[i].getDisplayName(locales[i]));
      languageItems[i].setActionCommand(locales[i].getDisplayName());
      languageItems[i].addActionListener(languageAction);
      languageMenu.add(languageItems[i]);
      languageGroup.add(languageItems[i]);
    }
    displayMenu.add(languageMenu);
    displayMenu.addSeparator();
    // font size listener
    FontSizer.addPropertyChangeListener("level", new PropertyChangeListener() { //$NON-NLS-1$

      public void propertyChange(PropertyChangeEvent e) {
        int level = ((Integer) e.getNewValue()).intValue();
        setFontLevel(level);
      }
    });
    sizeUpItem = new JMenuItem();
    sizeUpItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        FontSizer.levelUp();
      }
    });
    displayMenu.add(sizeUpItem);
    sizeDownItem = new JMenuItem();
    sizeDownItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        FontSizer.levelDown();
      }
    });
    displayMenu.add(sizeDownItem);
    displayMenu.addSeparator();
    lookFeelItem = new JCheckBoxMenuItem();
    lookFeelItem.setSelected(!OSPRuntime.javaLookAndFeel);
    lookFeelItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        boolean selected = lookFeelItem.isSelected();
        setLookAndFeel(!selected);
      }
    });
    displayMenu.add(lookFeelItem);
    helpMenu = new JMenu();
    helpMenu.addMouseListener(new MouseAdapter() {

      // hide splash dialog if visible
      public void mousePressed(MouseEvent e) {
        if(splashDialog!=null) {
          splashDialog.dispose();
        }
        helpMenu.removeMouseListener(this);
      }
    });
    helpMenu.addMouseListener(new MouseAdapter() {

      // refresh authorInfo item
      public void mouseEntered(MouseEvent e) {
        mousePressed(e);
      }

      public void mousePressed(MouseEvent e) {
        LaunchNode node = getSelectedNode();
        if(node!=null) {
          helpMenu.add(authorInfoItem);
        } else {
          helpMenu.remove(authorInfoItem);
        }
      }
    });
    menubar.add(helpMenu);
    logItem = new JMenuItem();
    logItem.setAccelerator(KeyStroke.getKeyStroke('L', mask));
    logItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        if(OSPRuntime.applet==null) { // not running as applet so create and position the log.
          OSPLog log = OSPLog.getOSPLog();
          if((log.getLocation().x==0)&&(log.getLocation().y==0)) {
            Point p = frame.getLocation();
            log.setLocation(p.x+28, p.y+28);
          }
        }
        OSPLog.showLog();
      }
    });
    helpMenu.add(logItem);
    inspectItem = new JMenuItem();
    helpMenu.add(inspectItem);
    inspectItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        LaunchSet tabSet = new LaunchSet(Launcher.this, tabSetName);
        tabSet.showHiddenNodes = Launcher.this instanceof LaunchBuilder;
        XMLControl xml = new XMLControlElement(tabSet);
        XMLTreePanel treePanel = new XMLTreePanel(xml, false);
        xmlInspector.setContentPane(treePanel);
        xmlInspector.setTitle(LaunchRes.getString("Inspector.Title.TabSet")+" \""+getDisplayName(tabSetName)+"\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        xmlInspector.setVisible(true);
      }
    });
    // diagnostics menu
    diagnosticMenu = new JMenu();
    helpMenu.add(diagnosticMenu);
    JMenuItem jarItem = new JMenuItem("Jar File"); //$NON-NLS-1$
    jarItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Diagnostics.aboutLaunchJar();
      }
    });
    diagnosticMenu.add(jarItem);
    JMenuItem vmItem = new JMenuItem("Java VM"); //$NON-NLS-1$
    vmItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Diagnostics.aboutJava();
      }
    });
    diagnosticMenu.add(vmItem);
    JMenuItem OSItem = new JMenuItem("OS"); //$NON-NLS-1$
    OSItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Diagnostics.aboutOS();
      }
    });
    diagnosticMenu.add(OSItem);
    JMenuItem qtItem = new JMenuItem("QuickTime"); //$NON-NLS-1$
    qtItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Diagnostics.aboutQTJava();
      }
    });
    diagnosticMenu.add(qtItem);
    JMenuItem j3dItem = new JMenuItem("Java 3D"); //$NON-NLS-1$
    j3dItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Diagnostics.aboutJava3D();
      }
    });
    diagnosticMenu.add(j3dItem);
    JMenuItem joglItem = new JMenuItem("JOGL"); //$NON-NLS-1$
    joglItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        Diagnostics.aboutJOGL();
      }
    });
    diagnosticMenu.add(joglItem);
    helpMenu.addSeparator();
    aboutItem = new JMenuItem();
    aboutItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        showAboutDialog();
      }
    });
    helpMenu.add(aboutItem);
    authorInfoItem = new JMenuItem();
    authorInfoItem.addActionListener(new ActionListener() {

      public void actionPerformed(ActionEvent e) {
        showAuthorInformation();
      }
    });
    helpMenu.add(authorInfoItem);
    if(OSPRuntime.applet==null) {
      // add window listener to exit
      frame.addWindowListener(new WindowAdapter() {

        public void windowClosing(WindowEvent e) {
          exit();
        }

        // Added by W. Christian to disable the Language menu
        public void windowGainedFocus(WindowEvent e) {
          OSPRuntime.setAuthorMode(false);
        }

        public void windowActivated(WindowEvent e) {
          OSPRuntime.setAuthorMode(false);
        }
      });
      // add exit menu item
      fileMenu.addSeparator();
      exitItem = new JMenuItem();
      exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', mask));
      exitItem.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent e) {
          exit();
        }
      });
    }
    // create and populate the internal xset menu
    if(OSPRuntime.getLaunchJarPath()!=null) {
      JarFile jar = OSPRuntime.getLaunchJar();
      if(jar!=null) {
        Action action = new AbstractAction() {

          public void actionPerformed(ActionEvent e) {
            // get name of file to open
            String fileName = ((JMenuItem) e.getSource()).getText();
            fileName = OSPRuntime.getLaunchJarName()+"!/"+fileName;                   //$NON-NLS-1$
            String[] prevArgs = undoManager.getLauncherState();
            if((prevArgs!=null)&&(open(fileName)!=null)) {
              String[] args = new String[]{fileName};
              LauncherUndo.LoadEdit edit = undoManager.new LoadEdit(args, prevArgs);
              undoSupport.postEdit(edit);
              refreshGUI();
            } else {
              open(fileName);
              refreshGUI();
            }
          }
        };
        // iterate thru JarFile entries and add menu items
        for(Enumeration e = jar.entries(); e.hasMoreElements(); ) {
          JarEntry entry = (JarEntry) e.nextElement();
          String name = entry.getName();
          if(name.endsWith(".xset")&&!name.startsWith(resourcesPath.substring(1))) { //$NON-NLS-1$
            if(name.startsWith(defaultFileName)) {
              continue;
            }
            if(openFromJarMenu==null) {
              openFromJarMenu = new JMenu();
            }
            JMenuItem item = new JMenuItem(name);
            item.addActionListener(action);
            openFromJarMenu.add(item);
          }
        }
      }
    }
    baseMenuFontSize = fileMenu.getFont().getSize();
    frame.setJMenuBar(menubar);
    frame.pack();
  } // end createGUI

  /**
   * Sets the font level.
   *
   * @param level the level
   */
  protected void setFontLevel(int level) {
    // set font levels of menubar and content pane
    FontSizer.setFonts(frame.getJMenuBar(), level);
    FontSizer.setFonts(frame.getContentPane(), level);
    // refresh string resources to adjust label sizes
    refreshStringResources();
    // refresh the tree to adjust node sizes
    LaunchPanel tab = getSelectedTab();
    if(tab!=null) {
      Enumeration en = tab.getRootNode().breadthFirstEnumeration();
      while(en.hasMoreElements()) {
        LaunchNode node = (LaunchNode) en.nextElement();
        tab.treeModel.nodeChanged(node);
      }
    }
    // refresh GUI to update font size menu
    refreshGUI();
  }

  /**
   * Gets the paths of currently open set and tabs.
   *
   * @return the open paths
   */
  protected Set getOpenPaths() {
    openPaths.clear();
    openPaths.add(tabSetName);
    for(int i = 0; i<tabbedPane.getTabCount(); i++) {
      LaunchPanel panel = (LaunchPanel) tabbedPane.getComponentAt(i);
      LaunchNode[] nodes = panel.getRootNode().getAllOwnedNodes();
      for(int j = 0; j<nodes.length; j++) {
        openPaths.add(nodes[j].getFileName());
      }
      openPaths.add(panel.getRootNode().getFileName());
    }
    return openPaths;
  }

  /**
   * Sets the look and feel.
   *
   * @param javaLF true to use default (Java) L&F
   */
  protected void setLookAndFeel(final boolean javaLF) {
    if(!isVisible()) {
      frame.addWindowListener(new WindowAdapter() {

        public void windowOpened(WindowEvent e) {
          setLookAndFeel(javaLF);
          frame.removeWindowListener(this);
        }
      });
      return;
    }
    if(OSPRuntime.javaLookAndFeel==javaLF) {
      return;
    }
    OSPRuntime.javaLookAndFeel = javaLF;
    JFrame.setDefaultLookAndFeelDecorated(javaLF);
    JDialog.setDefaultLookAndFeelDecorated(javaLF);
    exitCurrentApps();
    LauncherUndo undoManager = Launcher.this.undoManager;
    UndoableEditSupport undoSupport = Launcher.this.undoSupport;
    LaunchNode node = Launcher.this.getSelectedNode();
    Launcher.this.frame.dispose();
    Launcher launcher;
    LaunchSet tabset = new LaunchSet(Launcher.this, tabSetName);
    XMLControlElement control = new XMLControlElement(tabset);
    // set null password so LaunchBuilder opens without verification
    control.setPassword(null);
    control.setValue("default_look_and_feel", null); //$NON-NLS-1$
    if(Launcher.this instanceof LaunchBuilder) {
      launcher = new LaunchBuilder(control.toXML());
    } else {
      launcher = new Launcher(control.toXML());
    }
    launcher.setSelectedNode(node.getPathString());
    undoManager.setLauncher(launcher);
    launcher.undoManager = undoManager;
    launcher.undoSupport = undoSupport;
    launcher.tabSetName = tabSetName;
    launcher.password = password;
    launcher.jarBasePath = jarBasePath;
    launcher.frame.setDefaultCloseOperation(Launcher.this.frame.getDefaultCloseOperation());
    launcher.refreshGUI();
    launcher.setVisible(true);
  }

  /**
   * Shows the about dialog.
   */
  protected void showAboutDialog() {
    String newline = XML.NEW_LINE;
    String aboutString = "Launcher "+version+" "+releaseDate+newline //$NON-NLS-1$ //$NON-NLS-2$
                         +"Open Source Physics Project"+newline      //$NON-NLS-1$
                         +"www.opensourcephysics.org";               //$NON-NLS-1$
    JOptionPane.showMessageDialog(frame, aboutString, LaunchRes.getString("Help.About.Title")+" Launcher", //$NON-NLS-1$ //$NON-NLS-2$
                                  JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Shows the metadata dialog.
   */
  protected void showAuthorInformation() {
    LaunchNode node = getSelectedNode();
    if(node!=null) {
      String line = XML.NEW_LINE;
      String info = "";                                                                             //$NON-NLS-1$
      if(!node.getAuthor().trim().equals("")) {                                                     //$NON-NLS-1$
        info += LaunchRes.getString("Label.Author")+": ";                                           //$NON-NLS-1$ //$NON-NLS-2$
        info += node.getAuthor()+line;
      }
      if(!node.getKeywords().trim().equals("")) {                                                   //$NON-NLS-1$
        info += LaunchRes.getString("Label.Keywords")+": ";                                         //$NON-NLS-1$ //$NON-NLS-2$
        info += node.getKeywords()+line;
      }
      if(!node.getCourseLevel().trim().equals("")) {                                                //$NON-NLS-1$
        info += LaunchRes.getString("Label.Level")+": ";                                            //$NON-NLS-1$ //$NON-NLS-2$
        info += node.getCourseLevel()+line;
      }
      if(!node.getLanguages().trim().equals("")) {                                                  //$NON-NLS-1$
        info += LaunchRes.getString("Label.Languages")+": ";                                        //$NON-NLS-1$ //$NON-NLS-2$
        info += node.getLanguages()+line;
      }
      if(!node.getComment().trim().equals("")) {                                                    //$NON-NLS-1$
        info += LaunchRes.getString("Label.Comments")+": ";                                         //$NON-NLS-1$ //$NON-NLS-2$
        info += node.getComment()+line;
      }
      if(info.equals("")) {   //$NON-NLS-1$
        info = LaunchRes.getString("Dialog.AuthorInfo.NoInfo");                                     //$NON-NLS-1$
      }
      JOptionPane.showMessageDialog(frame, info, LaunchRes.getString("Menu.Help.AuthorInfo")+": \"" //$NON-NLS-1$ //$NON-NLS-2$
                                    +node.getName()+"\"",                                           //$NON-NLS-1$
                                    JOptionPane.INFORMATION_MESSAGE);
    }
  }

  /**
   * Determines whether the specified node is a link to another xset, tab or node.
   * To be a link, there must be no launch class but one or more arguments.
   *
   * @param node the launch node to verify
   * @return <code>true</code> if the node is a link
   */
  protected boolean isLink(LaunchNode node) {
    if((node==null)||!node.isLeaf()||((node.launchClassName!=null)&&!node.launchClassName.equals(""))) { //$NON-NLS-1$
      return false;
    }
    return(!node.args[0].equals("")||                              //$NON-NLS-1$
          ((node.args.length>1)&&!node.args[1].equals(""))||       //$NON-NLS-1$
                ((node.args.length>2)&&!node.args[2].equals(""))); //$NON-NLS-1$
  }

  /**
   * Determines whether the specified node has an associated EJS model (xml file).
   *
   * @param node the launch node to check
   * @return <code>true</code> if the node has an EJS model
   */
  protected boolean hasEJSModel(LaunchNode node) {
    if((node==null)||!node.isLeaf()) {
      return false;
    }
    return EjsTool.hasEjsModel(node.getLaunchClass());
  }

  /**
   * Determines whether the specified node is launchable.
   *
   * @param node the launch node to verify
   * @return <code>true</code> if the node is launchable
   */
  protected boolean isLaunchable(LaunchNode node) {
    if((node==null)||!node.isLeaf()) {
      return false;
    }
    return isLaunchable(node.getLaunchClass());
  }

  /**
   * Enables hyperlinks for the specified JEditorPane.
   *
   * @param textPane the editor pane
   * @param enabled true to enable hyperlinks
   */
  protected void setLinksEnabled(JEditorPane textPane, boolean enabled) {
    if((linkListener==null)&&enabled) {
      linkListener = new HyperlinkListener() {

        HyperlinkEvent event;
        public void hyperlinkUpdate(HyperlinkEvent e) {
          if(e.getEventType()==HyperlinkEvent.EventType.ACTIVATED) {
            // workaround so each event is handled only once
            if(event==e) {
              return;
            }
            event = e;
            URL url = e.getURL();
            // identify urls on web: look for http protocol? or file protocol?
            if(!url.getHost().equals("")) { //$NON-NLS-1$
              //  // open link in external browser if supported by this
              //  // virtual machine (VM) on this host
              //  // need to do this using reflection...
              //  if (Desktop.isDesktopSupported()) {
              //    Desktop desktop = Desktop.getDesktop();
              //    if (desktop.isSupported(Desktop.Action.BROWSE)) {
              //      try {
              //        desktop.browse(url.toURI());
              //      }
              //      catch(Exception ex) {
              //        ex.printStackTrace();
              //      }
              //    }
              //  }
              return;
            }
            URL prev = selectedNode.htmlURL;
            if(prev!=url) {
              String undoPath = selectedNode.getPathString();
              Integer undoPage = new Integer(selectedNode.htmlTabNumber);
              Object[] undoData = new Object[]{null, undoPath, undoPage, prev};
              Object[] redoData = null;
              // check to see if link is an anchor in same page
              if(prev.getPath().equals(url.getPath())) {
                redoData = new Object[]{null, undoPath, undoPage, url};
              } else {
                Object[] nodeData = getNodeAndPage(url);
                if(nodeData!=null) {
                  redoData = new Object[]{null, nodeData[0], nodeData[1], url};
                }
                // no redo node found, so redo = undo node
                else {
                  redoData = new Object[]{null, undoPath, undoPage, url};
                }
              }
              if(redoData!=null) {
                // post undoable edit
                if(postEdits) {
                  UndoableEdit edit = undoManager.new NavEdit(undoData, redoData);
                  undoSupport.postEdit(edit);
                }
                // select new node
                // prevent duplicate NavEdit while selecting node
                postEdits = false;
                String nodePath = (String) redoData[1];
                int page = ((Integer) redoData[2]).intValue();
                setSelectedNode(nodePath, page, url);
                postEdits = true;
              }
            }
          }
        }
      };
    }
    if(enabled) {
      textPane.addHyperlinkListener(linkListener);
    } else {
      textPane.removeHyperlinkListener(linkListener);
    }
  }

  /**
   * Gets the node path and page number associated with an html URL.
   * May return null.
   *
   * @param html the URL
   * @return [0] String node path, [1] Integer pageNumber
   */
  protected Object[] getNodeAndPage(URL html) {
    String urlPath = html.getFile();
    for(int i = 0; i<getTabCount(); i++) {
      LaunchPanel tab = getTab(i);
      Enumeration e = tab.getRootNode().breadthFirstEnumeration();
      while(e.hasMoreElements()) {
        LaunchNode node = (LaunchNode) e.nextElement();
        for(int j = 0; j<node.getHTMLCount(); j++) {
          URL next = node.getHTML(j).getURL();
          if(next==null) {
            continue;
          }
          String nextPath = next.getFile();
          if(nextPath.equals(urlPath)) { // found match
            String nodePath = node.getPathString();
            return new Object[]{nodePath, new Integer(j)};
          }
        }
      }
    }
    return null;
  }

  /**
   * Determines whether the specified class is launchable.
   *
   * @param type the launch class to verify
   * @return <code>true</code> if the class is launchable
   */
  protected static boolean isLaunchable(Class type) {
    if(type==null) {
      return false;
    }
    try {
      //throw exception if main method does not exist; return value not used
      type.getMethod("main", new Class[]{String[].class}); //$NON-NLS-1$
      return true;
    } catch(NoSuchMethodException ex) {
      return false;
    }
  }

  /**
   * Handles a mouse pressed event.
   *
   * @param e the mouse event
   * @param tab the launch panel receiving the event
   */
  protected void handleMousePressed(MouseEvent e, final LaunchPanel tab) {
    LaunchNode selectedNode = getSelectedNode();
    if(e.isPopupTrigger()||(e.getButton()==MouseEvent.BUTTON3)
        ||(e.isControlDown()&&(System.getProperty("os.name", "").indexOf("Mac")>-1))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
      // make sure node is selected for right-clicks
      TreePath path = tab.tree.getPathForLocation(e.getX(), e.getY());
      if(path==null) {
        return;
      }
      tab.tree.setSelectionPath(path);
      final LaunchNode node = getSelectedNode();
      if(node==null) {
        return;
      }
      // add items to popup menu
      JMenuItem inspectItem = new JMenuItem(LaunchRes.getString("MenuItem.Inspect"));   //$NON-NLS-1$
      inspectItem.addActionListener(new ActionListener() {

        public void actionPerformed(ActionEvent e) {
          inspect(node);
        }
      });
      inspectItem.setEnabled((password==null)||(this instanceof LaunchBuilder));
      popup.add(inspectItem);
      if(node.getLaunchClass()!=null) {
        if(node.launchCount==0) {
          popup.addSeparator();
          JMenuItem launchItem = new JMenuItem(LaunchRes.getString("MenuItem.Launch"));       //$NON-NLS-1$
          popup.add(launchItem);
          launchItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
              node.launch(tab);
            }
          });
        } else {
          popup.addSeparator();
          JMenuItem terminateItem = new JMenuItem(LaunchRes.getString("MenuItem.Terminate")); //$NON-NLS-1$
          popup.add(terminateItem);
          terminateItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
              node.terminateAll();
            }
          });
          if(node.launchCount>1) {
            terminateItem.setText(LaunchRes.getString("MenuItem.TerminateAll")); //$NON-NLS-1$
          }
          if(!node.isSingleton()) {
            JMenuItem launchItem = new JMenuItem(LaunchRes.getString("MenuItem.Relaunch")); //$NON-NLS-1$
            popup.add(launchItem);
            launchItem.addActionListener(new ActionListener() {

              public void actionPerformed(ActionEvent e) {
                node.launch(tab);
              }
            });
          }
        }
        if(hasEJSModel(node)) {
          popup.addSeparator();
          JMenuItem ejsItem = new JMenuItem(LaunchRes.getString("Popup.MenuItem.EjsModel")); //$NON-NLS-1$
          popup.add(ejsItem);
          ejsItem.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
              EjsTool.runEjs(node.getLaunchClass());
            }
          });
        }
      }
      // show popup when running Launcher (LaunchBuilder)
      if(getClass().equals(Launcher.class)) {
        popup.show(tab, e.getX()+4, e.getY()+12);
      }
    } else if((e.getClickCount()==2)&&isLaunchable(selectedNode)) {
      if(selectedNode.launchCount==0) {
        selectedNode.launch(tab);
      } else if(selectedNode.isSingleton()||(selectedNode.isSingleVM()&&selectedNode.isSingleApp())) {
        JOptionPane.showMessageDialog(frame, LaunchRes.getString("Dialog.Singleton.Message")+               //$NON-NLS-1$
          " \""+selectedNode.toString()+"\"",                                                               //$NON-NLS-1$ //$NON-NLS-2$
          LaunchRes.getString("Dialog.Singleton.Title"),                                                    //$NON-NLS-1$
          JOptionPane.INFORMATION_MESSAGE);
      } else {
        int selected = JOptionPane.showConfirmDialog(frame, LaunchRes.getString("Dialog.Relaunch.Message"), //$NON-NLS-1$
          LaunchRes.getString("Dialog.Relaunch.Title"),                                                     //$NON-NLS-1$
          JOptionPane.YES_NO_OPTION);
        if(selected==JOptionPane.YES_OPTION) {
          selectedNode.launch(tab);
        }
      }
      if(selectedNode.launchPanel!=null) {
        selectedNode.launchPanel.repaint();
      }
    } else if((e.getClickCount()==2)&&isLink(selectedNode)) {
      String[] prevArgs = undoManager.getLauncherState();
      if((open(selectedNode.args)!=null)&&(prevArgs!=null)) {
        UndoableEdit edit = undoManager.new LoadEdit(selectedNode.args, prevArgs);
        undoSupport.postEdit(edit);
      }
      refreshGUI();
    } else if((e.getClickCount()==2)&&(selectedNode.getLaunchClass()==null)&&(selectedNode.launchClassName!=null)
              &&!selectedNode.launchClassName.equals("")) {                                                 //$NON-NLS-1$
      // create jar list
      String[] jars = LaunchClassChooser.parsePath(selectedNode.getClassPath());
      String jarList = "";                                                                                  //$NON-NLS-1$
      for(int i = 0; i<jars.length; i++) {
        if(!jarList.equals("")) {                                                                           //$NON-NLS-1$
          if(jars[i].equals(OSPRuntime.getLaunchJarName())) {
            continue;
          }
          // separate jars with commas
          jarList += ", ";                                                                                  //$NON-NLS-1$
        }
        jarList += jars[i];
      }
      JOptionPane.showMessageDialog(frame, LaunchRes.getString("Dialog.ClassNotFound.Message1")+            //$NON-NLS-1$
        selectedNode.launchClassName+XML.NEW_LINE+LaunchRes.getString("Dialog.ClassNotFound.Message2")+     //$NON-NLS-1$
          XML.NEW_LINE+jarList, LaunchRes.getString("Dialog.ClassNotFound.Title"),                          //$NON-NLS-1$
                                JOptionPane.WARNING_MESSAGE);
    }
  }

  private void exitCurrentApps() {
    final Frame[] prevFrames = Frame.getFrames();
    for(int i = 0, n = prevFrames.length; i<n; i++) {
      if(existingFrames.contains(prevFrames[i])) {
        continue; // don't mess with pre-existing frames such as the applet plugin
      }
      if(!(prevFrames[i] instanceof LauncherFrame)) {
        WindowListener[] listeners = prevFrames[i].getWindowListeners();
        for(int j = 0; j<listeners.length; j++) {
          listeners[j].windowClosing(null);
        }
        if(prevFrames[i].isVisible()) {
          prevFrames[i].dispose();
        }
      }
    }
  }

  /**
   * Displays a splash screen while loading.
   */
  private void splash() {
    int w = 360;
    int h = 120;
    if(splashDialog==null) {
      splashDialog = new JDialog(frame, false);
      splashDialog.setUndecorated(true);
      Color darkred = new Color(128, 0, 0);
      JPanel splash = new JPanel(new BorderLayout());
      // white background to start
      splash.setBackground(Color.white);
      splash.setPreferredSize(new Dimension(w, h));
      splash.addMouseListener(new MouseAdapter() {

        public void mousePressed(MouseEvent e) {
          splashDialog.dispose();
        }
      });
      frame.addMouseListener(new MouseAdapter() {

        public void mousePressed(MouseEvent e) {
          splashDialog.dispose();
          frame.removeMouseListener(this);
        }
      });
      Border etch = BorderFactory.createEtchedBorder();
      splash.setBorder(etch);
      splashDialog.setContentPane(splash);
      Box labels = Box.createVerticalBox();
      JLabel titleLabel = new JLabel("OSP Launcher"); //$NON-NLS-1$
      if(this instanceof LaunchBuilder) {
        titleLabel.setText("OSP Launch Builder"); //$NON-NLS-1$
      }
      Font font = titleLabel.getFont().deriveFont(Font.BOLD);
      titleLabel.setFont(font.deriveFont(24f));
      titleLabel.setForeground(darkred);
      titleLabel.setAlignmentX(0.5f);
      titleLabel.setHorizontalAlignment(SwingConstants.CENTER);
      creditsLabel = new JLabel(" "); //$NON-NLS-1$
      font = font.deriveFont(Font.PLAIN);
      font = font.deriveFont(12f);
      creditsLabel.setFont(font);
      creditsLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 10, 2));
      creditsLabel.setHorizontalAlignment(SwingConstants.CENTER);
      creditsLabel.setAlignmentX(0.5f);
      splashPathLabel = new JLabel(" ") {    //$NON-NLS-1$

        public void setText(String s) {
          int max = 80;
          if((s!=null)&&(s.length()>max)) {
            s = s.substring(0, max-4)+"..."; //$NON-NLS-1$
          }
          super.setText(s);
        }
      };
      splashPathLabel.setFont(font);
      splashPathLabel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
      splashPathLabel.setHorizontalAlignment(SwingConstants.CENTER);
      splashPathLabel.setAlignmentX(0.5f);
      labels.add(Box.createGlue());
      labels.add(titleLabel);
      labels.add(Box.createGlue());
      labels.add(splashPathLabel);
      labels.add(creditsLabel);
      splash.add(labels, BorderLayout.CENTER);
      splashDialog.pack();
      splashTimer = new javax.swing.Timer(4000, new ActionListener() {

        public void actionPerformed(ActionEvent e) {
          if(frame.isShowing()) {
            splashDialog.dispose();
            splashTimer.stop();
          }
        }
      });
    }
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int x = dim.width/2;
    int y = dim.height/2;
    splashDialog.setLocation(x-w/2, y-h/2);
    splashDialog.setVisible(true);
    splashTimer.start();
  }

  /**
   * Exits this application.
   */
  protected void exit() {
    if(!terminateApps()) {
      // change default close operation to prevent window closing
      final int op = frame.getDefaultCloseOperation();
      frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
      // restore default close operation later
      Runnable runner = new Runnable() {

        public void run() {
          frame.setDefaultCloseOperation(op);
        }
      };
      SwingUtilities.invokeLater(runner);
      return;
    }
    // HIDE_ON_CLOSE apps should exit
    if((OSPRuntime.applet==null)&&(frame.getDefaultCloseOperation()==WindowConstants.HIDE_ON_CLOSE)) {
      System.exit(0);
    }
    // all applets and non-HIDE_ON_CLOSE apps should just hide themselves
    else {
      exitCurrentApps(); // close existing programs
      frame.setVisible(false);
    }
  }

  /**
   * Terminates running apps.
   *
   * @return false if process is cancelled by the user
   */
  protected boolean terminateApps() {
    if(frame.getDefaultCloseOperation()==WindowConstants.HIDE_ON_CLOSE) {
      // ask to terminate apps running in this process
      boolean approved = false;
      Frame[] frames = Frame.getFrames();
      for(int i = 0, n = frames.length; i<n; i++) {
        if(!approved&&frames[i].isVisible()&&!(frames[i] instanceof LauncherFrame)&&!(frames[i] instanceof OSPLog)
            &&!(frames[i] instanceof EncryptionTool)) {
          if(existingFrames.contains(frames[i])) {
            continue;                                                                                          // don't mess with pre-existing frames such as the applet plugin
          }
          int selected = JOptionPane.showConfirmDialog(frame,
            LaunchRes.getString("Dialog.Terminate.Message")+XML.NEW_LINE                                       //$NON-NLS-1$
            +LaunchRes.getString("Dialog.Terminate.Question"), LaunchRes.getString("Dialog.Terminate.Title"),  //$NON-NLS-1$ //$NON-NLS-2$
              JOptionPane.YES_NO_OPTION);
          if(selected==JOptionPane.YES_OPTION) {
            approved = true;
          } else {
            return false;
          }
        }
      }
      // ask to terminate apps running in separate processes
      approved = false;
      boolean declined = false;
      // look for nodes with running processes
      Component[] comps = tabbedPane.getComponents();
      for(int i = 0; i<comps.length; i++) {
        LaunchPanel tab = (LaunchPanel) comps[i];
        Enumeration e = tab.getRootNode().breadthFirstEnumeration();
        while(e.hasMoreElements()) {
          LaunchNode node = (LaunchNode) e.nextElement();
          if(!node.processes.isEmpty()) {
            if(!approved&&!declined) {                                                                         // ask for approval
              int selected = JOptionPane.showConfirmDialog(frame,
                LaunchRes.getString("Dialog.TerminateSeparateVM.Message")                                      //$NON-NLS-1$
                +XML.NEW_LINE+LaunchRes.getString("Dialog.TerminateSeparateVM.Question"), LaunchRes.getString( //$NON-NLS-1$
                  "Dialog.TerminateSeparateVM.Title"),                                                         //$NON-NLS-1$
                  JOptionPane.YES_NO_OPTION);
              approved = (selected==JOptionPane.YES_OPTION);
              declined = !approved;
            }
            if(approved) {                                                                                     // terminate processes
              for(Iterator it = node.processes.iterator(); it.hasNext(); ) {
                Process proc = (Process) it.next();
                it.remove();
                proc.destroy();
              }
            } else {
              return false;
            }
          }
        }
      }
    }
    return true;
  }

  /**
   * A unique frame class for Launcher. This is used to differentiate Launcher
   * from other apps when disposing of frames in singleApp mode.
   */
  private class LauncherFrame extends JFrame {

    public LauncherFrame() {
      setName("LauncherTool"); //$NON-NLS-1$
      setIconImage(ResourceLoader.getImage(OSPRuntime.OSP_ICON_FILE));
    }
  }

  /**
   * A cell renderer class to show launchable nodes.
   */
  private class LaunchRenderer extends DefaultTreeCellRenderer {

    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
        boolean leaf, int row, boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      LaunchNode node = (LaunchNode) value;
      setToolTipText(node.tooltip.equals("")   //$NON-NLS-1$
                     ? null
                     : node.tooltip);
      if((node.getFileName()!=null)&&(Launcher.this instanceof LaunchBuilder)) {
        setToolTipText(LaunchRes.getString("ToolTip.FileName")+" \""+node.getFileName()+"\"");              //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        setIcon(getFileIcon(node));
      } else if(node.launchCount>0) {
        if(node.isSingleton()) {
          setIcon(singletonIcon);
        } else if(node.isSingleVM()&&node.isSingleApp()) {
          setIcon(singletonIcon);
        } else {
          setIcon(launchedIcon);
        }
      } else if(hasEJSModel(node)) {
        setIcon(ejsIcon);
      } else if(isLaunchable(node)) {
        setIcon(launchIcon);
      } else if(isLink(node)) {
        setIcon(linkIcon);
      } else if(node.isLeaf()) {
        if((node.getLaunchClass()==null)&&(node.launchClassName!=null)&&!node.launchClassName.equals("")) { //$NON-NLS-1$
          setIcon(launchEmptyIcon);
        } else if(node.getHTMLCount()>0) {
          int count = 0;
          for(Iterator it = node.htmlData.iterator(); it.hasNext(); ) {
            HTML html = (HTML) it.next();
            if(html.url!=null) {
              count++;
            }
          }
          if(count>0) {
            setIcon(htmlIcon);
          } else {
            setIcon(noFileIcon);
          }
        } else {
          setIcon(noFileIcon);
        }
      }
      return this;
    }
  }

  /**
   * Gets an appropriate File icon for a node (must have non-null filename)
   *
   * @param node the launch node
   * @return the icon
   */
  protected Icon getFileIcon(LaunchNode node) {
    if(node.getFileName().length()==0) {
      return null;
    }
    File file = node.getFile();
    Resource res = node.getResource();
    boolean changed = changedFiles.contains(node.getFileName());
    // yellow file icon if changed
    if(changed) {
      return yellowFileIcon;
      // ghost file icon if resource is null or parent self contained
    } else if((res==null)||node.isParentSelfContained()) {
      return ghostFileIcon;
      // white file icon if has writable file
    } else if((file!=null)&&file.canWrite()) {
      return whiteFileIcon;
      // magenta icon if opened from jar or zip file
    } else if(file==null) {
      return magentaFileIcon;
    }
    // red icon if opened from read-only file
    return redFileIcon;
  }

  /**
   * A class to save and load a set of launch tabs and Launcher static fields.
   */
  public class LaunchSet implements XML.ObjectLoader {

    private Launcher launcher;
    private String name;
    boolean failedToLoad = false;
    public boolean showHiddenNodes = true;
    public LaunchSet() {
      launcher = Launcher.this;
    }

    protected LaunchSet(Launcher launcher, String path) {
      this.launcher = launcher;
      name = XML.getName(XML.forwardSlash(path));
    }

    public void saveObject(XMLControl control, Object obj) {
      LaunchSet tabset = (LaunchSet) obj;
      Launcher launcher = tabset.launcher;
      control.setValue("classpath", classPath);  //$NON-NLS-1$
      control.setValue("title", launcher.title); //$NON-NLS-1$
      // save dimensions and split pane divider location
      Dimension dim = launcher.contentPane.getSize();
      control.setValue("width", dim.width);                                  //$NON-NLS-1$
      control.setValue("height", dim.height);                                //$NON-NLS-1$
      control.setValue("divider", launcher.divider);                         //$NON-NLS-1$
      control.setValue("default_look_and_feel", OSPRuntime.javaLookAndFeel); //$NON-NLS-1$
      control.setValue("editor_enabled", launcher.editorEnabled);            //$NON-NLS-1$
      if(saveState) {
        // save currently selected node
        LaunchNode node = launcher.getSelectedNode();
        control.setValue("selected_node", node.getPathString()); //$NON-NLS-1$
        // save array of expanded node collections (one for each tab)
        int n = launcher.tabbedPane.getTabCount();
        Collection[] expansions = new Collection[n];
        for(int i = 0; i<n; i++) {
          LaunchPanel tab = launcher.getTab(i);
          expansions[i] = tab.getExpandedNodes();
        }
        control.setValue("expanded", expansions);                //$NON-NLS-1$
      }
      // save collection of tab root nodes
      Collection nodes = new ArrayList();
      for(int i = 0; i<launcher.tabs.size(); i++) {
        LaunchNode root = ((LaunchPanel) launcher.tabs.get(i)).getRootNode();
        if(root.isHiddenInLauncher()&&!tabset.showHiddenNodes) {
          continue;
        }
        // initialize selfContained, previewing and saveHiddenNodes properties
        root.parentSelfContained = false;
        root.previewing = false;
        root.saveHiddenNodes = tabset.showHiddenNodes;
        if(launcher.selfContained) {
          // save tab root
          root.setSelfContained(false);
          root.parentSelfContained = true;
          nodes.add(root);
        } else if(launcher.previewing) {
          // preview tab root
          root.previewing = true;
          nodes.add(root);
        } else if((root.getFileName()==null)||root.getFileName().equals("")) { //$NON-NLS-1$
          // save tab root
          nodes.add(root);
        } else {
          // save tab root filename
          nodes.add(root.getFileName());
        }
      }
      control.setValue("launch_nodes", nodes); //$NON-NLS-1$
      boolean hasPW = (launcher.password!=null)&&!launcher.password.equals(""); //$NON-NLS-1$
      if(hasPW&&pwRequiredToLoad) {
        control.setValue("pw_required_by_launcher", true); //$NON-NLS-1$
      }
      control.setValue("xml_password", launcher.password); //$NON-NLS-1$
    }

    public Object createObject(XMLControl control) {
      return null;
    }

    public Object loadObject(XMLControl control, Object obj) {
      LaunchSet tabset = (LaunchSet) obj;
      final Launcher launcher = tabset.launcher;
      // load a different launch set
      if(control.getPropertyNames().contains("launchset")) {         //$NON-NLS-1$
        String path = launcher.open(control.getString("launchset")); //$NON-NLS-1$
        tabset.failedToLoad = path==null;
        return obj;
      }
      // load static properties
      if(control.getPropertyNames().contains("classpath")) { //$NON-NLS-1$
        classPath = control.getString("classpath");          //$NON-NLS-1$
      }
      if(control.getPropertyNames().contains("default_look_and_feel")) {      //$NON-NLS-1$
        final boolean selected = control.getBoolean("default_look_and_feel"); //$NON-NLS-1$
        Runnable runner = new Runnable() {

          public void run() {
            launcher.setLookAndFeel(selected);
          }
        };
        SwingUtilities.invokeLater(runner);
      }
      // read selected node and set saveState flag
      String selectedPath = control.getString("selected_node"); //$NON-NLS-1$
      launcher.saveState = selectedPath!=null;
      // load launch nodes
      Collection nodes = (Collection) control.getObject("launch_nodes"); //$NON-NLS-1$
      if((nodes!=null)&&!nodes.isEmpty()) {
        int i = launcher.tabbedPane.getSelectedIndex();
        Iterator it = nodes.iterator();
        boolean tabAdded = false;
        LaunchNode buttonNode = null;
        while(it.hasNext()) {
          Object next = it.next();
          // prevent circular references
          if((tabset.name!=null)&&tabset.name.equals(next)) {
            continue;
          }
          if(next instanceof String) {
            String path = XML.getResolvedPath((String) next, tabSetBasePath);
            if(launcher.open(path)!=null) {
              tabAdded = true;
            }
          } else if(next instanceof LaunchNode) {
            // add the child node
            LaunchNode node = (LaunchNode) next;
            tabAdded = launcher.addTab(node)||tabAdded;
            if((launcher.getClass()==Launcher.class)&&node.isButtonView()&&(buttonNode==null)) {
              buttonNode = node;
            }
          }
        }
        if(!launcher.saveState) {
          // select the first button node or first added tab
          if(buttonNode!=null) {
            for(int j = 0; j<launcher.tabbedPane.getTabCount(); j++) {
              if(launcher.getTab(j).getRootNode()==buttonNode) {
                launcher.tabbedPane.setSelectedIndex(j);
              }
            }
          } else if(tabAdded) {
            launcher.tabbedPane.setSelectedIndex(i+1);
          }
        }
      }
      if(launcher.saveState) {
        Collection[] expansions = (Collection[]) control.getObject("expanded"); //$NON-NLS-1$
        if(expansions!=null) {
          for(int i = 0; i<expansions.length; i++) {
            LaunchPanel tab = launcher.getTab(i);
            tab.setExpandedNodes(expansions[i]);
          }
        }
        launcher.setSelectedNode(selectedPath);
      }
      launcher.title = control.getString("title"); //$NON-NLS-1$
      // load security items
      if(control.getPropertyNames().contains("editor_enabled")) {      //$NON-NLS-1$
        launcher.editorEnabled = control.getBoolean("editor_enabled"); //$NON-NLS-1$
      }
      launcher.password = control.getString("xml_password");                     //$NON-NLS-1$
      launcher.pwRequiredToLoad = control.getBoolean("pw_required_by_launcher"); //$NON-NLS-1$
      // load dimensions
      if(control.getPropertyNames().contains("width")&&control.getPropertyNames().contains("height")) { //$NON-NLS-1$ //$NON-NLS-2$
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        dim.width = Math.min((8*dim.width)/10, control.getInt("width"));                                //$NON-NLS-1$
        dim.height = Math.min((8*dim.height)/10, control.getInt("height"));                             //$NON-NLS-1$
        launcher.contentPane.setPreferredSize(dim);
        launcher.frame.pack();
      }
      // load divider position
      if(control.getPropertyNames().contains("divider")) { //$NON-NLS-1$
        launcher.divider = control.getInt("divider");      //$NON-NLS-1$
        launcher.refreshGUI();
      }
      return obj;
    }
  }

  // ________________________________ static methods _____________________________

  /**
   * Launches an application with no arguments.
   *
   * @param type the class to be launched
   */
  public static void launch(Class type) {
    launch(type, null, null);
  }

  /**
   * Launches an application with an array of string arguments.
   *
   * @param type the class to be launched
   * @param args the String array of arguments
   */
  public static void launch(Class type, String[] args) {
    launch(type, args, null);
  }

  /**
   * Launches an application asociated with a launch node.
   *
   * @param type the class to be launched
   * @param args the argument array (may be null)
   * @param node the launch node (may be null)
   */
  public static void launch(final Class type, String[] args, final LaunchNode node) {
    if(type==null) {
      OSPLog.info(LaunchRes.getString("Log.Message.NoClass"));                                                       //$NON-NLS-1$
      JOptionPane.showMessageDialog(null, LaunchRes.getString("Dialog.NoLaunchClass.Message"),                       //$NON-NLS-1$
                                    LaunchRes.getString("Dialog.NoLaunchClass.Title"), JOptionPane.WARNING_MESSAGE); //$NON-NLS-1$
      return;
    }
    String desc = LaunchRes.getString("Log.Message.Launching")+" "+type+", args "; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    if(args==null) {
      desc += args;
    } else {
      desc += "{";      //$NON-NLS-1$
      for(int i = 0; i<args.length; i++) {
        desc += args[i];
        if(i<args.length-1) {
          desc += ", "; //$NON-NLS-1$
        }
      }
      desc += "}";      //$NON-NLS-1$
    }
    OSPLog.fine(desc);
    // launch the app in single vm mode
    if(OSPRuntime.launchingInSingleVM||!newVMAllowed) {
      OSPRuntime.launchingInSingleVM = true;
      OSPLog.finer(LaunchRes.getString("Log.Message.LaunchCurrentVM"));     //$NON-NLS-1$
      // get array of frames before launching
      final Frame[] prevFrames = Frame.getFrames();
      // dispose of previous frames if single app mode
      if(singleAppMode) {
        OSPLog.finer(LaunchRes.getString("Log.Message.LaunchSingleApp"));   //$NON-NLS-1$
        boolean vis = OSPLog.isLogVisible();
        for(int i = 0, n = prevFrames.length; i<n; i++) {
          if(existingFrames.contains(prevFrames[i])) {
            continue;                                                       // don't mess with pre-exisitng frames such as the applet plugin
          }
          if(!(prevFrames[i] instanceof LauncherFrame)) {
          	// inform window listeners so they can close running apps
            WindowListener[] listeners = prevFrames[i].getWindowListeners();
            for(int j = 0; j<listeners.length; j++) {
              listeners[j].windowClosing(null);
            }
            prevFrames[i].dispose();
          }
        }
        if(vis) {
          OSPLog.showLog();
        }
        // terminate processes running in other VMs using node.terminateAll()?
      }
      // set xml class loader
      if(node!=null) {
        String classPath = node.getClassPath();
        XML.setClassLoader(LaunchClassChooser.getClassLoader(classPath));
      }
      // launch in singleVM by invoking main method from separate daemon thread
      final String[] arg = args;
      final Runnable launchRunner = new Runnable() {

        public void run() {
          activeNode = node;
          try {
            Method m = type.getMethod("main", new Class[]{String[].class}); //$NON-NLS-1$
            m.invoke(type, new Object[]{arg});                              // may not return!
          } catch(Exception ex) {
            ex.printStackTrace();
          }
          // main method returned, so remove launchThread from node map
          node.threads.remove(this);
          activeNode = null;
          // find frames associated with launched app and store in node
          if(frameFinder!=null) {
            findFramesFor(node, prevFrames, this);
            if(frameFinder!=null) {
              frameFinder.stop();
              frameFinder = null;
            }
          }
        }
      };
      // create timer to look for new frames in case main method doesn't return
      if(frameFinder!=null) {
        frameFinder.stop();
      }
      frameFinder = new javax.swing.Timer(1000, new ActionListener() {

        public void actionPerformed(ActionEvent e) {
          findFramesFor(node, prevFrames, launchRunner); // stops timer if finds frames
        }
      });
      Thread launchThread = new Thread(launchRunner);
      launchThread.setDaemon(true);
      node.threads.put(launchRunner, launchThread);
      launchThread.start();
      frameFinder.start();
      return;
    }
    // launch the app in a separate java process
    OSPLog.finer(LaunchRes.getString("Log.Message.LaunchSeparateVM")); //$NON-NLS-1$
    // construct the command to execute
    final Vector cmd = new Vector();
    cmd.add("java"); //$NON-NLS-1$
    if((classPath!=null)&&!classPath.equals("")) {                          //$NON-NLS-1$
      String jar = getDefaultJar();
      if((jar!=null)&&(classPath.indexOf(jar)==-1)) {
        classPath += ";"+jar;                                               //$NON-NLS-1$
      }
      // convert any colons to semicolons
      int i = classPath.indexOf(":");                                       //$NON-NLS-1$
      while(i!=-1) {
        classPath = classPath.substring(0, i)+";"+classPath.substring(i+1); //$NON-NLS-1$
        i = classPath.indexOf(":");                                         //$NON-NLS-1$
      }
      // replace semicolons with platform-dependent path separator
      char pathSeparator = System.getProperty("path.separator").charAt(0);  //$NON-NLS-1$
      classPath = classPath.replace(';', pathSeparator);
      cmd.add("-classpath");                                                //$NON-NLS-1$
      cmd.add(classPath);
    }
    cmd.add(type.getName());
    if(args!=null) {
      for(int i = 0; (i<args.length)&&(args[i]!=null); i++) {
        cmd.add(args[i]);
      }
    }
    // create a launch thread for separate VM
    Runnable launchRunner = new Runnable() {

      public void run() {
        OSPLog.finer(LaunchRes.getString("Log.Message.Command")+" "+cmd.toString()); //$NON-NLS-1$ //$NON-NLS-2$
        String[] cmdarray = (String[]) cmd.toArray(new String[0]);
        String[] envVars = new String[]{"osp_launcher=true"};                        //$NON-NLS-1$
        try {
          Process proc = Runtime.getRuntime().exec(cmdarray, envVars);
          BufferedInputStream errStream = new BufferedInputStream(proc.getErrorStream());
          StringBuffer buff = new StringBuffer();
          while(true) {
            int datum = errStream.read();
            if(datum==-1) {
              break;
            }
            buff.append((char) datum);
          }
          errStream.close();
          String msg = buff.toString().trim();
          if(msg.length()>0) {
            OSPLog.info("error buffer: "+buff.toString());                           //$NON-NLS-1$
          }
          if(node!=null) {
            node.processes.add(proc);
          }
          proc.waitFor();
          if(node!=null) {
            node.threadRunning(false);
            node.processes.remove(proc);
          }
        } catch(Exception ex) {
          OSPLog.info(ex.toString());
          if(node!=null) {
            node.threadRunning(false);
          }
        }
      }
    };
    if(node!=null) {
      node.threadRunning(true);
    }
    new Thread(launchRunner).start();
  }

  /**
   * Sets the static jarsOnly property.
   *
   * @param onlyJars true to restrict class paths to jar files
   */
  public static void setJarsOnly(boolean onlyJars) {
    LaunchClassChooser.jarsOnly = onlyJars;
  }

  /**
   * Main entry point when used as application.
   *
   * @param args args[0] may be an xml file name
   */
  public static void main(String[] args) {
    //    java.util.Locale.setDefault(new java.util.Locale("es"));
    //    OSPLog.setLevel(ConsoleLevel.ALL);
    Launcher launcher = new Launcher();
    if((args!=null)&&(args.length>0)) {
      launcher.open(args);
    } else {
      String path = null;
      // look for default file with launchJarName or defaultFileName
      if(OSPRuntime.getLaunchJarName()!=null) {
        path = launcher.open(XML.stripExtension(OSPRuntime.getLaunchJarName())+".xset"); //$NON-NLS-1$
      }
      if(path==null) {
        path = launcher.open(defaultFileName+".xset");                                  //$NON-NLS-1$
      }
      if(path==null) {
        path = launcher.open(defaultFileName+".xml");                                   //$NON-NLS-1$
      }
    }
    launcher.refreshGUI();
    // center frame on the screen
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (dim.width-launcher.frame.getBounds().width)/2;
    int y = (dim.height-launcher.frame.getBounds().height)/2;
    launcher.frame.setLocation(x, y);
    //((LauncherFrame) launcher.frame).setKeepHidden(false);
    launcher.frame.setVisible(true);
  }

  /**
   * Gets a class chooser for selecting launchable classes from jar files.
   *
   * @return the jar class chooser
   */
  protected LaunchClassChooser getClassChooser() {
    if(classChooser==null) {
      classChooser = new LaunchClassChooser(contentPane);
    }
    return classChooser;
  }

  /**
   * Gets a file chooser for selecting xml files.
   *
   * @return the xml chooser
   */
  protected static JFileChooser getXMLChooser() {
    if(chooser!=null) {
      return chooser;
    }
    chooser = new JFileChooser(new File(OSPRuntime.chooserDir));
    // add xml file filters
    launcherFileFilter = new FileFilter() {

      // accept all directories, *.xml, *.xset and zip files.
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
        if((extension!=null)&&(extension.equals("xset")||extension.equals("xml")||extension.equals("zip"))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          return true;
        }
        return false;
      }

      // the description of this filter
      public String getDescription() {
        return LaunchRes.getString("FileChooser.LauncherFilter.Description"); //$NON-NLS-1$
      }
    };
    xmlFileFilter = new FileFilter() {

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
        if((extension!=null)&&extension.equals("xml")) { //$NON-NLS-1$
          return true;
        }
        return false;
      }

      // the description of this filter
      public String getDescription() {
        return LaunchRes.getString("FileChooser.XMLFilter.Description"); //$NON-NLS-1$
      }
    };
    xsetFileFilter = new FileFilter() {

      // accept all directories and *.xset files.
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
        if((extension!=null)&&extension.equals("xset")) { //$NON-NLS-1$
          return true;
        }
        return false;
      }

      // the description of this filter
      public String getDescription() {
        return LaunchRes.getString("FileChooser.XSETFilter.Description"); //$NON-NLS-1$
      }
    };
    chooser.addChoosableFileFilter(xmlFileFilter);
    chooser.addChoosableFileFilter(xsetFileFilter);
    chooser.addChoosableFileFilter(launcherFileFilter);
    return chooser;
  }

  /**
   * Gets the display name of the specified file name.
   *
   * @param fileName the file name
   * @return the bare name without path or extension
   */
  protected static String getDisplayName(String fileName) {
    fileName = XML.getName(fileName);
    int i = fileName.lastIndexOf("."); //$NON-NLS-1$
    if(i!=-1) {
      return fileName.substring(0, i);
    }
    return fileName;
  }

  /**
   * Gets the name of the jar containing the default launcher xml file, if any.
   *
   * @return the jar name
   */
  protected static String getDefaultJar() {
    URL url = ClassLoader.getSystemResource(defaultFileName+".xset"); //$NON-NLS-1$
    if(url==null) {
      url = ClassLoader.getSystemResource(defaultFileName+".xml"); //$NON-NLS-1$
    }
    if(url==null) {
      return null;
    }
    String path = url.getPath();
    // trim trailing slash and file name
    int i = path.indexOf("/"+defaultFileName); //$NON-NLS-1$
    if(i==-1) {
      return null;
    }
    path = path.substring(0, i);
    // jar name is followed by "!"
    i = path.lastIndexOf("!"); //$NON-NLS-1$
    if(i==-1) {
      return null;
    }
    // trim and return jar name
    return path.substring(path.lastIndexOf("/")+1, i); //$NON-NLS-1$
  }

  /**
   * Displays the properties of the specified node in an xml table inspector.
   *
   * @param node the launch node
   */
  private void inspect(LaunchNode node) {
    XMLControl xml = new XMLControlElement(node);
    if(hasEJSModel(node)) {
      String name = XML.getSimpleClassName(node.getLaunchClass());
      xml.setValue("EJS_model", name+".xml"); //$NON-NLS-1$ //$NON-NLS-2$
    }
    XMLTable table = new XMLTable(xml);
    tableInspector.setContentPane(new JScrollPane(table));
    tableInspector.setTitle(LaunchRes.getString("Inspector.Title.Node")+" \""+node.name+"\""); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
    tableInspector.setVisible(true);
  }

  private static void findFramesFor(final LaunchNode node, Frame[] prevFrames, final Runnable runner) {
    // get current frames
    Frame[] frames = Frame.getFrames();
    // make newFrames collection
    final Collection newFrames = new ArrayList();
    for(int i = 0; i<frames.length; i++) {
      // ignore message frames, DO_NOTHING frames and incidental frames
      if(frames[i] instanceof JFrame) {
        JFrame frame = (JFrame) frames[i];
        if((frame.getDefaultCloseOperation()==WindowConstants.DO_NOTHING_ON_CLOSE)||(frame instanceof MessageFrame)
            ||(frame instanceof LauncherFrame)) {
          continue;
        }
      }
      if((frames[i].getClass().getName().indexOf("SharedOwnerFrame")>-1)||( //$NON-NLS-1$
          frames[i].getClass().getName().indexOf("QTFrame")>-1)) {          //$NON-NLS-1$
        continue;
      }
      newFrames.add(frames[i]);
    }
    // throw out previous frames
    for(int i = 0; i<prevFrames.length; i++) {
      newFrames.remove(prevFrames[i]);
    }
    // return if no new frames found
    if(newFrames.isEmpty()) {
      return;
    }
    // look thru new frames for "control frame"
    // "control frame" is JFrame/EXIT_ON_CLOSE or OSPFrame/wishesToExit()
    frames = (Frame[]) newFrames.toArray(new Frame[0]);
    newFrames.clear();
    FrameCloser frameCloser = new FrameCloser(node, newFrames, runner);
    for(int i = 0; i<frames.length; i++) {
      if(frames[i] instanceof JFrame) {                                   // found new frame
        JFrame frame = (JFrame) frames[i];
        if(((frame instanceof AppFrame)&&((AppFrame) frame).wishesToExit())
            ||(frame.getDefaultCloseOperation()==JFrame.EXIT_ON_CLOSE)) { // found control frame
          if(frame.getDefaultCloseOperation()==JFrame.EXIT_ON_CLOSE) {
            // change default close operation from EXIT_ON_CLOSE to DISPOSE_ON_CLOSE
            // so exiting a launched app does not close Launcher
            frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
          }
          // add window listener so control frames can close associated windows
          frame.addWindowListener(frameCloser);
        }
        newFrames.add(frame);
      }
    }
    // add frames to node
    if(node!=null) {
      node.frames.addAll(newFrames);
      node.launchCount++;
      // turn off and release timer
      if(frameFinder!=null) {
        frameFinder.stop();
        frameFinder = null;
      }
    }
    // repaint
    if(node.launchPanel!=null) {
      node.launchPanel.repaint();
    }
  }

  // a class to refresh a node when a launched frame closes
  static class FrameCloser extends WindowAdapter {

    LaunchNode node;
    Collection frames;
    Runnable runner;
    FrameCloser(LaunchNode node, Collection newFrames, Runnable runner) {
      frames = newFrames;
      this.node = node;
      this.runner = runner;
    }

    public void windowClosing(WindowEvent e) {
    	OSPLog.fine("Closing frames for node "+node); //$NON-NLS-1$
      Iterator it = frames.iterator();
      // dispose of control frame and associated frames
      while(it.hasNext()) {
        Frame frame = (Frame) it.next();
        // remove this frame closer so it only operates once
        frame.removeWindowListener(this);
        frame.dispose();
      }
      if(node!=null) {
        Thread thread = (Thread) node.threads.get(runner);
        if(thread!=null) {
          thread.interrupt();
          node.threads.put(runner, null);
        }
        node.frames.removeAll(frames);
        node.launchCount = Math.max(0, --node.launchCount);
        if(node.launchPanel!=null) {
          node.launchPanel.repaint();
        }
      }
    }
  }

  /**
   * A class for displaying html pages in a scrolled textPane.
   */
  public class HTMLPane {

    public JTextPane editorPane;
    JScrollPane scroller;
    // constructor
    HTMLPane() {
      editorPane = new JTextPane();
      editorPane.setEditable(false);
      editorPane.addMouseListener(new MouseAdapter() {

        public void mousePressed(MouseEvent e) {
          if(!undoManager.canUndo()) {
            return;
          }
          if(e.isPopupTrigger()||(e.getButton()==MouseEvent.BUTTON3)
              ||(e.isControlDown()&&(System.getProperty("os.name", "").indexOf("Mac")>-1))) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            // make popup and add back item
            JPopupMenu popup = new JPopupMenu();
            JMenuItem item = new JMenuItem(LaunchRes.getString("Popup.MenuItem.Back"));       //$NON-NLS-1$
            item.addActionListener(new ActionListener() {

              public void actionPerformed(ActionEvent e) {
                undoManager.undo();
              }
            });
            popup.add(item);
            popup.show(editorPane, e.getX(), e.getY()+8);
          }
        }
      });
      scroller = new JScrollPane(editorPane);
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
