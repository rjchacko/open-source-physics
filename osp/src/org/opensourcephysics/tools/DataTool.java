/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.beans.*;
import java.rmi.*;
import java.util.*;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.io.File;
import java.lang.reflect.Method;
import javax.swing.*;
import javax.swing.event.*;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;

/**
 * This provides a GUI for analyzing OSP Data objects.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class DataTool extends OSPFrame implements Tool, PropertyChangeListener {
  // static fields
  public static boolean loadClass=false;
  protected static JFileChooser chooser;
  protected static OSPLog log = OSPLog.getOSPLog();
  protected static Dimension dim = new Dimension(720, 500);
  protected static int buttonHeight = 28;


  // instance fields
  protected JTabbedPane tabbedPane;
  protected boolean useChooser = true;
  protected JPanel contentPane = new JPanel(new BorderLayout());
  protected PropertyChangeSupport support;
  protected XMLControl control = new XMLControlElement();
  protected JobManager jobManager = new JobManager(this);
  protected JMenu addMenu;
  protected JMenu subtractMenu;
  protected JMenu multiplyMenu;
  protected JMenu divideMenu;
  protected JMenu fileMenu;
  protected JMenuItem openItem;
  protected JMenuItem saveItem;
  protected JMenuItem saveAsItem;
  protected JMenuItem closeItem;
  protected JMenuItem closeAllItem;
  protected JMenuItem printItem;
  protected JMenuItem exitItem;
  protected JMenu editMenu;
  protected JMenu copyMenu;
  protected JMenuItem copyImageItem;
  protected JMenuItem copyTabItem;
  protected JMenuItem copyDataItem;
  protected JMenu pasteMenu;
  protected JMenuItem pasteNewTabItem;
  protected JMenuItem pasteColumnsItem;
  protected JMenu helpMenu;
  protected JMenuItem helpItem;
  protected JMenuItem logItem;
  protected JMenuItem aboutItem;
  protected FunctionTool dataFunctionTool;
  protected JLabel helpLabel;
  protected TextFrame helpFrame;
  protected String helpPath = "data_tool_help.html"; //$NON-NLS-1$
  protected String helpBase = "http://www.opensourcephysics.org/online_help/tools/"; //$NON-NLS-1$

  /**
   * A shared data tool.
   */
  final static DataTool DATATOOL = new DataTool();

  /**
   * Gets the shared DataTool.
   *
   * @return the shared DataTool
   */
  public static DataTool getTool() {
    return DATATOOL;
  }

  /**
   * Constructs a blank DataTool.
   */
  public DataTool() {
    this(ToolsRes.getString("DataTool.Frame.Title"), "DataTool"); //$NON-NLS-1$ //$NON-NLS-2$
  }

  /**
   * Constructs a DataTool and opens the specified xml file.
   *
   * @param fileName the name of the xml file
   */
  public DataTool(String fileName) {
    this();
    open(fileName);
  }

  /**
   * Constructs a DataTool and loads data from an xml control.
   *
   * @param control the xml control
   */
  public DataTool(XMLControl control) {
    this();
    addTab(control);
  }

  /**
   * Constructs a DataTool and loads the specified data object.
   *
   * @param data the data
   */
  public DataTool(Data data) {
    this();
    addTab(data);
  }

  /**
   * Constructs a DataTool and loads a data object into a named tab.
   *
   * @param data the data
   * @param name the tab name
   */
  public DataTool(Data data, String name) {
    this();
    addTab(data, name);
  }

  /**
   * Adds a tab and loads data from an xml control.
   *
   * @param control the xml control
   *
   * @return a collection of newly loaded Data objects
   */
  public DataToolTab addTab(XMLControl control) {
  	// if control is for DataToolTab class, load tab from control and add it
    if (DataToolTab.class.isAssignableFrom(control.getObjectClass())) {
    	DataToolTab tab = (DataToolTab)control.loadObject(null);
    	tab.dataTool = this;
    	addTab(tab);
    	return tab;
    }
    // otherwise load data from control into a null tab (new one created)
    return loadData(null, control, useChooser);
  }

  /**
   * Adds a tab for the specified Data object. The tab name will be
   * that of the Data object if it defines a getName() method.
   *
   * @param data the Data
   * @return the newly added tab
   */
  public DataToolTab addTab(Data data) {
    // try to get name of data from getName() method
    String name = ""; //$NON-NLS-1$
    try {
      Method m = data.getClass().getMethod("getName", new Class[0]); //$NON-NLS-1$
      name = (String)m.invoke(data, new Object[0]);
    } catch(Exception ex) {/** empty block */}
    return addTab(data, name);
  }

  /**
   * Adds a tab for the specified Data object and proposes a name
   * for the tab. The name will be modified if not unique.
   *
   * @param data the Data
   * @param name a proposed tab name
   * @return the newly added tab
   */
  public DataToolTab addTab(Data data, String name) {
    DataToolTab tab = new DataToolTab(data, this);
    tab.setName(name);
    addTab(tab);
    return tab;
  }

  /**
   * Adds a tab for the specified Data object. The tab name will be
   * that of the Data object if it defines a getName() method.
   *
   * @param data the Data
   * @return the newly added tab
   */
  public DataToolTab removeTab(Data data) {
    DataToolTab tab = getTab(data);
    if (tab != null) {
      removeTab(getTabIndex(data)); 
      return tab;
    }
    return null;
  }

  /**
   * Removes the tab at the specified index.
   *
   * @param index the tab number
   */
  public void removeTab(int index) {
    if(index>=0 && index < tabbedPane.getTabCount()) {
      String title = tabbedPane.getTitleAt(index);
      OSPLog.finer("removing tab "+title); //$NON-NLS-1$
      tabbedPane.removeTabAt(index);
      refreshTabTitles();
    }
  }

  /**
   * Updates the data.
   *
   * @param data the Data
   */
  public void update(Data data) {
  	DataToolTab tab = getTab(data); // tab may be null
  	if (tab != null) tab.reloadData(data);
  }

  /**
   * Returns the tab containing the specified Data object. May return null.
   *
   * @param data the Dataset
   * @return the tab
   */
  public DataToolTab getTab(Data data) {
    int i = getTabIndex(data);
    return i > -1? (DataToolTab) tabbedPane.getComponentAt(i): null;
  }

  /**
   * Returns the tab at the specified index. May return null.
   *
   * @param index the tab index
   * @return the tab
   */
  public DataToolTab getTab(int index) {
    return index > -1 && index < tabbedPane.getTabCount()?
    			(DataToolTab) tabbedPane.getComponentAt(index): null;
  }

  /**
   * Returns the tab count.
   *
   * @return the number of tabs
   */
  public int getTabCount() {
    return tabbedPane.getTabCount();
  }

  /**
   * Opens an xml file specified by name.
   *
   * @param fileName the file name
   * @return the file name, if successfully opened (datasets loaded)
   */
  public String open(String fileName) {
    OSPLog.fine("opening "+fileName); //$NON-NLS-1$
    // read the file into an XML control
    XMLControlElement control = new XMLControlElement(fileName);
    if(addTab(control) != null) {
    	refreshFunctionTool();
      return fileName;
    }
    OSPLog.finest("no data found"); //$NON-NLS-1$
    return null;
  }

  /**
   * Sends a job to this tool and specifies a tool to reply to.
   *
   * @param job the Job
   * @param replyTo the tool to notify when the job is complete (may be null)
   * @throws RemoteException
   */
  public void send(Job job, Tool replyTo) throws RemoteException {
    XMLControlElement control = new XMLControlElement(job.getXML());
    if(control.failedToRead()||control.getObjectClass()==Object.class) {
      return;
    }
    // log the job in
    jobManager.log(job, replyTo);
    // if control is for a Data object, load it into new or existing tab
    if (Data.class.isAssignableFrom(control.getObjectClass())) {
    	Data data = (Data)control.loadObject(null, true, true);
    	DataToolTab tab = getTab(data); // tab may be null
    	loadData(data, tab); // will update tab or make new tab if null
      refreshTabTitles();
    }
    else addTab(control); // adds Data objects found in XMLControl
//    // associate data with this job for easy replies
//    Iterator it = dataLoaded.iterator();
//    while(it.hasNext()) {
//      jobManager.associate(job, it.next());
//    }
  }

  /**
   * Sets the useChooser flag.
   *
   * @param useChooser true to load datasets with a chooser
   */
  public void setUseChooser(boolean useChooser) {
    this.useChooser = useChooser;
  }

  /**
   * Gets the useChooser flag.
   *
   * @return true if loading datasets with a chooser
   */
  public boolean isUseChooser() {
    return useChooser;
  }

  /**
   * Listens for property changes "function" and "visible"
   *
   * @param e the event
   */
	public void propertyChange(PropertyChangeEvent e) {
		String name = e.getPropertyName();
		if (name.equals("function")) { //$NON-NLS-1$
	    DataToolTab tab = getSelectedTab();
	    if (tab != null) {
		    tab.dataTable.refreshTable();
		    tab.statsTable.refreshStatistics();
		    tab.statsTable.refreshTable();
		    tab.refresh();
	    }
		}
	}

//______________________________ protected methods ________________________

  /**
   * Gets a unique name.
   *
   * @param proposed the proposed name
   * @return the unique name
   */
  protected String getUniqueName(String proposed) {
  	if (proposed == null || proposed.equals("")) //$NON-NLS-1$
  		proposed = ToolsRes.getString("DataToolTab.DefaultName"); //$NON-NLS-1$
 	  // construct a unique name from proposed by adding trailing digit if nec
  	ArrayList taken = new ArrayList();
    for (int i = 0; i < getTabCount(); i++) {
    	DataToolTab tab = getTab(i);
    	taken.add(tab.getName());
    }
    if (!taken.contains(proposed)) return proposed;
    // strip existing numbered subscript if any
    int n = proposed.lastIndexOf("_"); //$NON-NLS-1$
    if (n > -1) {
    	String end = proposed.substring(n+1);
    	try {
    		Integer.parseInt(end);
    		proposed = proposed.substring(0, n);
    	}
    	catch (Exception ex) {/** empty block */}
    }
    proposed += "_"; //$NON-NLS-1$
  	int i = 1;
  	String name = proposed+i;
    while(taken.contains(name)) {
      i++;
      name = proposed+i;
    }
    return name;
  }

  /**
   * Loads data from an xml control into a specified tab. If no tab is
   * specified, then a new one is created.
   *
   * @param tab the tab to load (may be null)
   * @param control the xml control describing the data
   * @param useChooser true to present data choices to user
   *
   * @return the loaded tab
   */
  protected DataToolTab loadData(DataToolTab tab, XMLControl control,
  			boolean useChooser) {
    java.util.List xmlControls;
    // first populate the list with Data XMLControls
    if(useChooser) {
      // get user-selected data objects from an xml tree chooser
      XMLTreeChooser chooser = new XMLTreeChooser(
          ToolsRes.getString("Chooser.Title"), ToolsRes.getString("Chooser.Label"), this); //$NON-NLS-1$ //$NON-NLS-2$
      xmlControls = chooser.choose(control, Data.class);
    }
    else {
      // find all Data objects in the control
      XMLTree tree = new XMLTree(control);
      tree.setHighlightedClass(Data.class);
      tree.selectHighlightedProperties();
      xmlControls = tree.getSelectedProperties();
      if(xmlControls.isEmpty()) {
        JOptionPane.showMessageDialog(null, ToolsRes.getString("Dialog.NoDatasets.Message")); //$NON-NLS-1$
      }
    }
    // load the list of Data XMLControls
    if(!xmlControls.isEmpty()) {
      Iterator it = xmlControls.iterator();
      while(it.hasNext()) {
        XMLControl next = (XMLControl) it.next();
        Data data = null;
	      if (next instanceof XMLControlElement) {
	        XMLControlElement element = (XMLControlElement)next;
	        data = (Data)element.loadObject(null, true, true);
	      }
	      else data = (Data) next.loadObject(null);
	      if (data == null) continue;
	      tab = loadData(data, tab);
      }
    }
    return tab;
  }

  /**
   * Loads data into a specified tab. If tab is null, a new one is created.
   *
   * @param tab the tab to load (may be null)
   *
   * @return the loaded tab
   */
  protected DataToolTab loadData(Data data, DataToolTab tab) {
    // try to get name of data from getName() method
    String name = ""; //$NON-NLS-1$
    try {
      Method m = data.getClass().getMethod("getName", new Class[0]); //$NON-NLS-1$
      name = (String)m.invoke(data, new Object[0]);
    } catch(Exception ex) {/** empty block */}
    // if tab is null, create and add a new tab
    if (tab == null) {
      tab = addTab(data, name);
    }
    // else reload data
    else tab.reloadData(data);
    return tab;
  }

  /**
   * Adds a tab. The tab must be named before calling this method.
   *
   * @param tab a DataToolTab
   */
  private void addTab(final DataToolTab tab) {
  	// assign a unique name (also traps for null name)
  	tab.setName(getUniqueName(tab.getName()));
    tab.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        if (!tab.fitCheckbox.isSelected()) {
          tab.splitPanes[1].setDividerLocation(1.0);
        }
      }
    });
    OSPLog.finer("adding tab "+tab.getName()); //$NON-NLS-1$
    tabbedPane.addTab("", tab); //$NON-NLS-1$
    tabbedPane.setSelectedComponent(tab);
    validate();
    tab.init();
    tab.refresh();
    refreshTabTitles();
  }

  /**
   * Gets the currently selected DataToolTab, if any.
   *
   * @return the selected tab
   */
  protected DataToolTab getSelectedTab() {
    return(DataToolTab) tabbedPane.getSelectedComponent();
  }

  /**
   * Selects a DataToolTab.
   *
   * @param tab the tab to select
   */
  protected void setSelectedTab(DataToolTab tab) {
    tabbedPane.setSelectedComponent(tab);
  }

  /**
   * Opens an xml file selected with a chooser.
   *
   * @return the name of the opened file
   */
  protected String open() {
    int result = OSPRuntime.getChooser().showOpenDialog(null);
    if(result==JFileChooser.APPROVE_OPTION) {
      OSPRuntime.chooserDir = OSPRuntime.getChooser().getCurrentDirectory().toString();
      String fileName = OSPRuntime.getChooser().getSelectedFile().getAbsolutePath();
      fileName = XML.getRelativePath(fileName);
      return open(fileName);
    }
    return null;
  }

  /**
   * Saves the current xml control to the specified file.
   *
   * @param fileName the file name
   * @return the name of the saved file, or null if not saved
   */
  protected String save(String fileName) {
  	DataToolTab tab = getSelectedTab();
  	if (fileName == null || fileName.equals("")) return saveAs(); //$NON-NLS-1$
    XMLControl control = new XMLControlElement(tab);
//    if (control == null) return null;
    if (control.write(fileName) == null) return null;
    tab.fileName = fileName;
    return fileName;
  }

  /**
   * Saves the currently displayed xml control to a file selected with a chooser.
   *
   * @return the name of the saved file, or null if not saved
   */
  protected String saveAs() {
    int result = OSPRuntime.getChooser().showSaveDialog(this);
    if(result==JFileChooser.APPROVE_OPTION) {
      OSPRuntime.chooserDir = OSPRuntime.getChooser().getCurrentDirectory().toString();
      File file = OSPRuntime.getChooser().getSelectedFile();
      // check to see if file already exists
      if(file.exists()) {
        int selected = JOptionPane.showConfirmDialog(null,
        				ToolsRes.getString("EncryptionTool.Dialog.ReplaceFile.Message")+file.getName()+"?",  //$NON-NLS-1$ //$NON-NLS-2$
        				ToolsRes.getString("EncryptionTool.Dialog.ReplaceFile.Title"),  //$NON-NLS-1$
        				JOptionPane.YES_NO_CANCEL_OPTION);
        if(selected!=JOptionPane.YES_OPTION) {
          return null;
        }
      }
      String fileName = file.getAbsolutePath();
      if(fileName==null||fileName.trim().equals("")) { //$NON-NLS-1$
        return null;
      }
      return save(XML.getRelativePath(fileName));
    }
    return null;
  }

  /**
   * Returns the index of the tab containing the specified Data object.
   *
   * @param data the Dataset
   * @return the name of the opened file
   */
  protected int getTabIndex(Data data) {
    for(int i = 0; i<tabbedPane.getTabCount(); i++) {
      DataToolTab tab = (DataToolTab) tabbedPane.getComponentAt(i);
      if (tab.isOwnedBy(data)) return i;
    }
    return -1;
  }

  /**
   * Constructs a DataTool with title and name.
   */
  protected DataTool(String title, String name) {
    super(title);
    setName(name);
    createGUI();
    Toolbox.addTool(name, this);
    ToolsRes.addPropertyChangeListener("locale", new PropertyChangeListener() { //$NON-NLS-1$
      public void propertyChange(PropertyChangeEvent e){
        refreshGUI();
      }
    });
  }

  /**
   * Removes all tabs except the specified index.
   *
   * @param index the tab number
   */
  protected void removeAllButTab(int index) {
    for (int i = tabbedPane.getTabCount()-1; i >= 0; i--) {
      if (i ==index) continue;
      String title = tabbedPane.getTitleAt(i);
      OSPLog.finer("removing tab "+title); //$NON-NLS-1$
      tabbedPane.removeTabAt(i);
    }
    refreshTabTitles();
  }

  /**
   * Removes all tabs.
   */
  protected void removeAllTabs() {
    for (int i = tabbedPane.getTabCount()-1; i >= 0; i--) {
      String title = tabbedPane.getTitleAt(i);
      OSPLog.finer("removing tab "+title); //$NON-NLS-1$
      tabbedPane.removeTabAt(i);
    }
  }

  protected void refreshTabTitles() {
  	String vars = ""; //$NON-NLS-1$
  	String tabName = ""; //$NON-NLS-1$
  	int n = tabbedPane.getSelectedIndex();
    // show variables being plotted
    String[] tabTitles = new String[tabbedPane.getTabCount()];
    for(int i = 0;i<tabTitles.length;i++) {
      DataToolTab tab = (DataToolTab)tabbedPane.getComponentAt(i);
      String dataName = tab.getName();
      Dataset dataset = tab.getWorkingData();
      String col0 = GUIUtils.removeSubscripting(dataset.getColumnName(0));
      String col1 = GUIUtils.removeSubscripting(dataset.getColumnName(1));
      String s = " (" + col0 +", " //$NON-NLS-1$ //$NON-NLS-2$
      		+ col1 + ")"; //$NON-NLS-1$
      tabTitles[i] = dataName+ s;
      if (i == n) {
      	vars = s;
      	tabName = " \""+dataName+"\""; //$NON-NLS-1$ //$NON-NLS-2$
      }
    }
    // set tab titles
    for(int i = 0;i<tabTitles.length;i++) {
      tabbedPane.setTitleAt(i, tabTitles[i]);
    }
    // set text of copy menu items
	  copyDataItem.setText(ToolsRes.getString(
	  			"DataTool.MenuItem.CopyData")+vars); //$NON-NLS-1$
	  copyTabItem.setText(ToolsRes.getString(
	  			"DataTool.MenuItem.CopyTab")+tabName); //$NON-NLS-1$
  }

  /**
   * Gets the function tool for defining custom data functions.
   */
  protected FunctionTool getDataFunctionTool() {
  	if (dataFunctionTool == null) { // create new tool if none exists
  		dataFunctionTool = new FunctionTool(this);
  		dataFunctionTool.setHelpPath("data_builder_help.html"); //$NON-NLS-1$
  		dataFunctionTool.addPropertyChangeListener("function", this); //$NON-NLS-1$
  		dataFunctionTool.setTitle(ToolsRes.getString("DataTool.DataBuilder.Title")); //$NON-NLS-1$
  	}
  	refreshFunctionTool();
  	return dataFunctionTool;
  }

  /**
   * Refreshes the function tool.
   */
  protected void refreshFunctionTool() {
  	if (dataFunctionTool == null) return;
		// add and remove DataFunctionPanels based on current tabs
  	ArrayList tabNames = new ArrayList();
    for(int i = 0; i<tabbedPane.getTabCount(); i++) {
      DataToolTab tab = getTab(i);
      tabNames.add(tab.getName());
      if (dataFunctionTool.getPanel(tab.getName()) == null) {
	      FunctionPanel panel = new DataFunctionPanel(tab.dataManager);
	      dataFunctionTool.addPanel(tab.getName(), panel);
      }
    }
    ArrayList remove = new ArrayList();
    for(Iterator it = dataFunctionTool.panels.keySet().iterator(); it.hasNext();) {
      String name = it.next().toString();
      if (!tabNames.contains(name))
      	remove.add(name);
    }
    for(Iterator it = remove.iterator(); it.hasNext();) {
      String name = it.next().toString();
      dataFunctionTool.removePanel(name);
    }
  }

  /**
   * Creates the GUI.
   */
  protected void createGUI() {
    // configure the frame
    contentPane.setPreferredSize(dim);
    setContentPane(contentPane);
    JPanel centerPanel = new JPanel(new BorderLayout());
    contentPane.add(centerPanel, BorderLayout.CENTER);
    setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
    this.addComponentListener(new ComponentAdapter() {
      public void componentResized(ComponentEvent e) {
        DataToolTab tab = getSelectedTab();
        if (tab == null) return;
        if (!tab.propsCheckbox.isSelected()
        			&& !tab.statsCheckbox.isSelected()) {
          tab.splitPanes[2].setDividerLocation(0);
        }
      }
    });
    // create tabbed pane
    tabbedPane = new JTabbedPane(SwingConstants.TOP);
    centerPanel.add(tabbedPane, BorderLayout.CENTER);
    tabbedPane.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
      	DataToolTab tab = getSelectedTab();
      	if (tab != null) {
    	    tab.dataTable.refreshTable();
    	    tab.statsTable.refreshStatistics();
    	    tab.statsTable.refreshTable();
    	    tab.refresh();
      	}
      }
    });
    tabbedPane.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        if(e.isPopupTrigger()||e.getButton()==MouseEvent.BUTTON3||(e.isControlDown()&&System.getProperty("os.name", "").indexOf("Mac")>-1)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          final int index = tabbedPane.getSelectedIndex();
          // make popup with name change and close items
          JPopupMenu popup = new JPopupMenu();
          JMenuItem item = new JMenuItem(ToolsRes.getString("DataTool.MenuItem.Name")); //$NON-NLS-1$
          popup.add(item);
          item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	DataToolTab tab = getTab(index);
            	String name = tab.getName();
              Object input = JOptionPane.showInputDialog(DataTool.this,
         	  				ToolsRes.getString("DataTool.Dialog.Name.Message"), //$NON-NLS-1$
            				ToolsRes.getString("DataTool.Dialog.Name.Title"), //$NON-NLS-1$
            				JOptionPane.QUESTION_MESSAGE, null, null, name);
          		if (input == null) return;
          		// hide tab name so getUniqueName not confused
          		tab.setName(""); //$NON-NLS-1$
              tab.setName(getUniqueName(input.toString()));
              refreshTabTitles();
              refreshFunctionTool();
            }
          });
          popup.addSeparator();
          item = new JMenuItem(ToolsRes.getString("MenuItem.Close")); //$NON-NLS-1$
          popup.add(item);
          item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              removeTab(index);
            }
          });
          item = new JMenuItem(ToolsRes.getString("MenuItem.CloseOthers")); //$NON-NLS-1$
          popup.add(item);
          item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              removeAllButTab(index);
            }
          });
          item = new JMenuItem(ToolsRes.getString("MenuItem.CloseAll")); //$NON-NLS-1$
          popup.add(item);
          item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
              removeAllTabs();
            }
          });
          popup.show(tabbedPane, e.getX(), e.getY()+8);
        }
      }
    });
    // create the menu bar
    int keyMask = Toolkit.getDefaultToolkit().getMenuShortcutKeyMask();
    JMenuBar menubar = new JMenuBar();
    fileMenu = new JMenu();
    menubar.add(fileMenu);
    openItem = new JMenuItem();
    openItem.setAccelerator(KeyStroke.getKeyStroke('O', keyMask));
    openItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        open();
      }
    });
    fileMenu.add(openItem);
    closeItem = new JMenuItem();
    closeItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int index = tabbedPane.getSelectedIndex();
        removeTab(index);
      }
    });
    fileMenu.add(closeItem);
    closeAllItem = new JMenuItem();
    closeAllItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        removeAllTabs();
      }
    });
    fileMenu.add(closeAllItem);
    fileMenu.addSeparator();
    // save item
    saveItem = new JMenuItem();
    saveItem.setAccelerator(KeyStroke.getKeyStroke('S', keyMask));
    saveItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	DataToolTab tab = getSelectedTab();
      	save(tab.fileName);
      }
    });
    fileMenu.add(saveItem);
    // save as item
    saveAsItem = new JMenuItem();
    saveAsItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	saveAs();
      }
    });
    fileMenu.add(saveAsItem);
    fileMenu.addSeparator();
    printItem = new JMenuItem();
    printItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SnapshotTool.getTool().printImage(DataTool.this);
      }
    });
    fileMenu.add(printItem);
    fileMenu.addSeparator();
    exitItem = new JMenuItem();
    exitItem.setAccelerator(KeyStroke.getKeyStroke('Q', keyMask));
    exitItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        removeAllTabs();
        System.exit(0);
      }
    });
    fileMenu.add(exitItem);
    editMenu = new JMenu();
    editMenu.addMouseListener(new MouseAdapter() {
      public void mouseEntered(MouseEvent e) {mousePressed(e);}
      public void mousePressed(MouseEvent e) {
        // ignore when menu is about to close
        if (!editMenu.isPopupMenuVisible()) return;
        // enable paste menu if clipboard contains xml string data
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        Transferable data = clipboard.getContents(null);
        if (data != null && data.isDataFlavorSupported(DataFlavor.stringFlavor)) {
          try {
            control.readXML( (String) data.getTransferData(DataFlavor.stringFlavor));
            pasteMenu.setEnabled(!control.failedToRead());
          }
          catch (Exception ex) {pasteMenu.setEnabled(false);}
        }
        else pasteMenu.setEnabled(false);
      }
    });
    menubar.add(editMenu);
    copyMenu = new JMenu();
    editMenu.add(copyMenu);
    copyDataItem = new JMenuItem();
    copyDataItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        DataToolTable.WorkingDataset working = getSelectedTab().getWorkingData();
        if(working==null) {
          return;
        }
        DatasetManager data = new DatasetManager();
        // load x data into new dataset
        Dataset xsource = working.getXSource();
        Dataset xdata = new Dataset();
        Dataset ysource = working.getYSource();
        Dataset ydata = new Dataset();
        xdata.append(xsource.getYPoints(), xsource.getYPoints());
        xdata.setMarkerColor(xsource.getEdgeColor());
        xdata.setLineColor(xsource.getLineColor());
        xdata.setMarkerSize(xsource.getMarkerSize());
        xdata.setMarkerShape(xsource.getMarkerShape());
        xdata.setConnected(xsource.isConnected());
        xdata.setXYColumnNames(xsource.getYColumnName(), xsource.getYColumnName());
        ydata.append(xsource.getYPoints(), ysource.getYPoints());
        ydata.setMarkerColor(ysource.getEdgeColor());
        ydata.setLineColor(ysource.getLineColor());
        ydata.setMarkerSize(ysource.getMarkerSize());
        ydata.setMarkerShape(ysource.getMarkerShape());
        ydata.setConnected(ysource.isConnected());
        ydata.setXYColumnNames(xsource.getYColumnName(), ysource.getYColumnName());
        data.addDataset(xdata);
        data.addDataset(ydata);
        control = new XMLControlElement(data);
        control.setValue("data_tool_transfer", true); //$NON-NLS-1$
        StringSelection s = new StringSelection(control.toXML());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(s, s);
      }
    });
    copyMenu.add(copyDataItem);
    copyTabItem = new JMenuItem();
    copyTabItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        int i = tabbedPane.getSelectedIndex();
        String title = tabbedPane.getTitleAt(i);
        OSPLog.finest("copying tab "+title); //$NON-NLS-1$
        XMLControl control = new XMLControlElement(getSelectedTab());
        StringSelection data = new StringSelection(control.toXML());
        Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
        clipboard.setContents(data, data);
      }
    });
    copyMenu.add(copyTabItem);
    copyImageItem = new JMenuItem();
    copyImageItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        SnapshotTool.getTool().copyImage(DataTool.this);
      }
    });
    copyImageItem.setAccelerator(KeyStroke.getKeyStroke('C', keyMask));
    copyMenu.add(copyImageItem);
    pasteMenu = new JMenu();
    editMenu.add(pasteMenu);
    pasteColumnsItem = new JMenuItem();
    pasteColumnsItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
      	if (getSelectedTab() == null) return;
        try {
          Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
          Transferable data = clipboard.getContents(null);
          String dataString = (String) data.getTransferData(DataFlavor.stringFlavor);
          if(dataString!=null) {
            XMLControl control = new XMLControlElement();
            control.readXML(dataString);
            if(!control.failedToRead()) {
              OSPLog.finest("pasting into tab"); //$NON-NLS-1$
            }
            if (Dataset.class.isAssignableFrom(control.getObjectClass())
            			|| control.getBoolean("data_tool_transfer")) { //$NON-NLS-1$
            	loadData(getSelectedTab(), control, false);
            }
            else if(loadData(getSelectedTab(), control, true) == null) {
              OSPLog.finest("no data found"); //$NON-NLS-1$
            }
          }
        } catch(Exception ex) {ex.printStackTrace();}
      }
    });
    pasteMenu.add(pasteColumnsItem);
    pasteNewTabItem = new JMenuItem();
    pasteNewTabItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        try {
          Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
          Transferable data = clipboard.getContents(null);
          String dataString = (String) data.getTransferData(DataFlavor.stringFlavor);
          if(dataString!=null) {
            XMLControl control = new XMLControlElement();
            control.readXML(dataString);
            if(!control.failedToRead()) {
              OSPLog.finest("pasting new tab"); //$NON-NLS-1$
            }
            if (Dataset.class.isAssignableFrom(control.getObjectClass())
            			|| control.getBoolean("data_tool_transfer")) { //$NON-NLS-1$
            	if (loadData(null, control, false) != null) {
            		int i = getTabCount()-1;
            		tabbedPane.setSelectedIndex(i);
            	}
            }
            else if(addTab(control) == null) {
              OSPLog.finest("no data found"); //$NON-NLS-1$
            }
            refreshFunctionTool();
          }
        } catch(Exception ex) {ex.printStackTrace();}
      }
    });
    pasteMenu.add(pasteNewTabItem);
    helpMenu = new JMenu();
    menubar.add(helpMenu);
    helpItem = new JMenuItem();
    helpItem.setAccelerator(KeyStroke.getKeyStroke('H', keyMask));
    helpItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (helpFrame == null) {
	       	 String help = XML.getResolvedPath(helpPath, helpBase);
	         if (ResourceLoader.getResource(help) != null)
	        	 helpFrame = new TextFrame(help);
	         else {
	        	 String classBase = "/org/opensourcephysics/resources/tools/html/"; //$NON-NLS-1$
	        	 help = XML.getResolvedPath(helpPath, classBase);
	        	 helpFrame = new TextFrame(help);
	         }
           helpFrame.setSize(760, 560);
           // center on the screen
           Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
           int x = (dim.width-helpFrame.getBounds().width)/2;
           int y = (dim.height-helpFrame.getBounds().height)/2;
           helpFrame.setLocation(x, y);
         }
         helpFrame.setVisible(true);
      }
    });
    helpMenu.add(helpItem);
    helpMenu.addSeparator();
    logItem = new JMenuItem();
    logItem.setAccelerator(KeyStroke.getKeyStroke('L', keyMask));
    logItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if(log.getLocation().x==0&&log.getLocation().y==0) {
          Point p = getLocation();
          log.setLocation(p.x+28, p.y+28);
        }
        log.setVisible(true);
      }
    });
    helpMenu.add(logItem);
    helpMenu.addSeparator();
    aboutItem = new JMenuItem();
    aboutItem.setAccelerator(KeyStroke.getKeyStroke('A', keyMask));
    aboutItem.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        showAboutDialog();
      }
    });
    helpMenu.add(aboutItem);
    setJMenuBar(menubar);
    // create help label for status bar
    helpLabel = new JLabel("", SwingConstants.LEADING); //$NON-NLS-1$
    helpLabel.setFont(new JTextField().getFont());
    helpLabel.setBorder(BorderFactory.createEmptyBorder(1, 2, 1, 2));
    centerPanel.add(helpLabel, BorderLayout.SOUTH);
    refreshGUI();
    pack();
    // center this on the screen
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
    int x = (dim.width-getBounds().width)/2;
    int y = (dim.height-getBounds().height)/2;
    setLocation(x, y);
  }

  /**
   * Refreshes the GUI.
   */
  protected void refreshGUI() {
    setTitle(ToolsRes.getString("DataTool.Frame.Title")); //$NON-NLS-1$
    fileMenu.setText(ToolsRes.getString("Menu.File")); //$NON-NLS-1$
    openItem.setText(ToolsRes.getString("MenuItem.Open")); //$NON-NLS-1$
    closeItem.setText(ToolsRes.getString("MenuItem.Close")); //$NON-NLS-1$
    closeAllItem.setText(ToolsRes.getString("MenuItem.CloseAll")); //$NON-NLS-1$
    saveItem.setText(ToolsRes.getString("DataTool.MenuItem.Save")); //$NON-NLS-1$
    saveAsItem.setText(ToolsRes.getString("DataTool.MenuItem.SaveAs")); //$NON-NLS-1$
    printItem.setText(ToolsRes.getString("DataTool.MenuItem.Print")); //$NON-NLS-1$
    exitItem.setText(ToolsRes.getString("MenuItem.Exit")); //$NON-NLS-1$
    editMenu.setText(ToolsRes.getString("Menu.Edit")); //$NON-NLS-1$
    copyMenu.setText(ToolsRes.getString("DataTool.Menu.Copy")); //$NON-NLS-1$
    copyImageItem.setText(ToolsRes.getString("DataTool.MenuItem.CopyImage")); //$NON-NLS-1$
    pasteMenu.setText(ToolsRes.getString("MenuItem.Paste")); //$NON-NLS-1$
    pasteNewTabItem.setText(ToolsRes.getString("DataTool.MenuItem.PasteNewTab")); //$NON-NLS-1$
    pasteColumnsItem.setText(ToolsRes.getString("DataTool.MenuItem.PasteNewColumns")); //$NON-NLS-1$
    helpMenu.setText(ToolsRes.getString("Menu.Help")); //$NON-NLS-1$
    helpItem.setText(ToolsRes.getString("DataTool.MenuItem.Help")); //$NON-NLS-1$
    logItem.setText(ToolsRes.getString("MenuItem.Log")); //$NON-NLS-1$
    aboutItem.setText(ToolsRes.getString("MenuItem.About")); //$NON-NLS-1$
    helpLabel.setText(ToolsRes.getString("DataTool.StatusBar.Help.DragColumns")); //$NON-NLS-1$
    int n = tabbedPane.getTabCount();
    for(int i = 0; i<n; i++) {
    	DataToolTab tab = (DataToolTab)tabbedPane.getComponentAt(i);
      tab.refreshGUI();
    }
  }

  /**
   * Shows the about dialog.
   */
  protected void showAboutDialog() {
    String aboutString = getName()+" 1.2  Feb 2008\n" //$NON-NLS-1$
				+"Douglas Brown, Author\n" //$NON-NLS-1$
    		+"Open Source Physics Project\n" //$NON-NLS-1$
    		+"www.opensourcephysics.org"; //$NON-NLS-1$
    JOptionPane.showMessageDialog(this, aboutString,
                                  ToolsRes.getString("Dialog.About.Title")+" "+getName(), //$NON-NLS-1$ //$NON-NLS-2$
                                  JOptionPane.INFORMATION_MESSAGE);
  }

  /**
   * Creates a button with a specified maximum height.
   *
   * @param text the button text
   * @param h the button height
   * @return the button
   */
  protected static JButton createButton(String text) {
    JButton button = new JButton(text) {
      public Dimension getMaximumSize() {
        Dimension dim = super.getMaximumSize();
        dim.height = buttonHeight;
        return dim;
      }
    };
    return button;
  }

}
/*
 * Open Source Physics software is free software; you can redistribute
 * it and/or modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License,
 * or(at your option) any later version.

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
