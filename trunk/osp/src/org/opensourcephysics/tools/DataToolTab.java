/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;
import java.beans.*;
import java.lang.reflect.Method;
import java.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;
import org.opensourcephysics.display.dialogs.ScaleInspector;

/**
 * This tab displays and analyses a single Data object in a DataTool.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class DataToolTab extends JPanel {

  // instance fields
	protected DataTool dataTool; // the tool that creates this tab
  protected final Data owner; // the Data object that creates/owns this tab
  protected Data[] guests = new Data[0]; // guest Data objects 
  protected DatasetManager dataManager = new DatasetManager(); // local datasets
  protected JSplitPane[] splitPanes;
  protected DataToolPlotter plot;
  protected DataToolTable dataTable;
  protected DataToolStatsTable statsTable;
  protected DataToolPropsTable propsTable;
  protected JScrollPane statsScroller, propsScroller;
  protected JCheckBox statsCheckbox, fitCheckbox, propsCheckbox;
  protected DatasetCurveFitter curveFitter;
  protected JButton dataBuilderButton;
  protected SelectionBox selectionBox = new SelectionBox();
  protected Point zoomPoint;
  protected Action fitAction, propsAndStatsAction;
  protected String fileName;
  protected JButton helpButton;

  /**
   * Constructs a DataToolTab for the specified Data and DataTool.
   *
   * @param data the Data object
   * @param tool the DataTool
   */
  public DataToolTab(Data data, DataTool tool) {
  	dataTool = tool;
  	owner = data;
    ArrayList list = owner.getDatasets();
    if (list != null && list.size()>0) {
    	// create datasets for the table: one for each unique variable name
    	// dataset values are set by the owner but display properties 
    	// are set by the user via working datasets
	    for (Iterator it = list.iterator(); it.hasNext();) {
	    	final Dataset next = (Dataset)it.next();
	    	if (next == null) continue;
	      XMLControlElement xml = new XMLControlElement(next);
	      double[] x = next.getXPoints();
	      // first dataset: always load x-column
	    	if (dataManager.getDatasets().isEmpty()){
	    		Dataset local = new HighlightableDataset(); 
		      xml.loadObject(local, true, true);
		      // copy x-column into y
		      local.setXYColumnNames(next.getXColumnName(), next.getXColumnName());
		      local.clear();
		      local.append(x, x);
	    		local.setXColumnVisible(false);
	      	dataManager.addDataset(local);
	    	}
	      // load y-column if x is a match and y is not
	    	if (isDuplicateColumn(next.getXColumnName(), x)
	    				&& !isDuplicateColumn(next.getYColumnName(), next.getYPoints())){
	    		Dataset local = new HighlightableDataset(); 
		      xml.loadObject(local, true, true);
	    		local.setXColumnVisible(false);
	      	dataManager.addDataset(local);
	    	}
	      // if neither x nor y is a match, then load new tab
	    	else if (!isDuplicateColumn(next.getXColumnName(), x)
	    				&& !isDuplicateColumn(next.getYColumnName(), next.getYPoints())){
	    		Runnable runner = new Runnable() {
            public synchronized void run() {
    	    		dataTool.addTab(next, next.getName());
            }
          };
          SwingUtilities.invokeLater(runner);
	    	}
	    }
    }
    createGUI();
    refreshGUI();
  }

  /**
   * Adds new data to this tab.
   * 
   * @param data the data to add
   * @return true if added
   */
  public boolean addData(Data data) {
  	boolean added = false;
    ArrayList list = data.getDatasets();
    if (list != null && list.size()>0) {
    	// add Datasets for which the x-column matches an existing column
	    for (Iterator it = list.iterator(); it.hasNext();) {
	    	// create local dataset and load properties
	    	final Dataset next = (Dataset)it.next();
	      XMLControlElement xml = new XMLControlElement(next);
	      // load y-column if x is a match and y is not
	    	if (isDuplicateColumn(next.getXColumnName(), next.getXPoints())
	    				&& !isDuplicateColumn(next.getYColumnName(), next.getYPoints())){
	    		Dataset local = new HighlightableDataset(); 
		      xml.loadObject(local, true, true);
	    		local.setXColumnVisible(false);
	      	// assign a unique column name
		      String yName = next.getYColumnName();
	      	String yColName = getUniqueYColumnName(local, yName, false);
	      	local.setXYColumnNames(local.getXColumnName(), yColName);
	      	dataManager.addDataset(local);
	      	dataTable.getWorkingData(yColName);
	      	added = true;
	    	}
      	if (added) {
	      	int len = guests.length;
	      	Data[] newData = new Data[len+1];
	      	System.arraycopy(guests, 0, newData, 0, len);
	      	newData[len] = data;
	      	guests = newData;
	      }
	    }
	    dataTable.refreshTable();
    }
    return added;
  }
  
  /**
   * Sets the x and y columns by name.
   * 
   * @param xColName the name of the horizontal axis variable
   * @param yColName the name of the vertical axis variable
   */
  public void setXYColumns(String xColName, String yColName) {
  	dataTable.setWorkingColumns(xColName, yColName);
  }
  
  /**
   * Sets the connected property for a given a column.
   * 
   * @param colName the name of the column
   * @param connected true to connect points with lines
   */
  public void setConnected(String colName, boolean connected) {
  	Dataset working = dataTable.getWorkingData(colName);
  	if (working != null) working.setConnected(connected);
  }
  
  /**
   * Sets the markers visible property for a given a column.
   * 
   * @param colName the name of the column
   * @param visible true to show markers
   */
  public void setMarkersVisible(String colName, boolean visible) {
  	DataToolTable.WorkingDataset working = dataTable.getWorkingData(colName);
  	if (working != null) working.setMarkersVisible(visible);
  }
  
  // _______________________ protected & private methods __________________________

  /**
   * Gets the working dataset.
   *
   * @return the first two data columns in the datatable (x-y order)
   */
  protected DataToolTable.WorkingDataset getWorkingData() {
    return dataTable.getWorkingData();
  }
  
  /**
   * Returns the data object that owns this tab.
   *
   * @return the owner Data
   */
  protected Data getOwner() {
    return owner;
  }

  /**
   * Returns a name that is unique to this tab.
   *
   * @param f the data function
   * @param proposed the proposed name for the function
   * @return unique name
   */
  private String getUniqueYColumnName(Dataset d, String proposed, boolean askUser) {
 	  // check for duplicate name
  	if (askUser) {
    	int tries = 0, maxTries = 2;
    	while (isDuplicateName(d, proposed) && tries < maxTries) {
    		tries++;
    		proposed = JOptionPane.showInputDialog(this, 
   	  				"\""+proposed+"\" "+ //$NON-NLS-1$ //$NON-NLS-2$
      				ToolsRes.getString("DataFunctionPanel.Dialog.DuplicateName.Message"), //$NON-NLS-1$
      				ToolsRes.getString("DataFunctionPanel.Dialog.DuplicateName.Title"), //$NON-NLS-1$
      				JOptionPane.WARNING_MESSAGE);
    	}
  	}
  	int i = 0;
  	String name = proposed;
    while(isDuplicateName(d, name)) {
    	i++;
    	name = proposed+i;
    }
    return name;
  }

  /**
   * Returns true if name is a duplicate.
   *
   * @param f the data function
   * @param name the proposed name for the function
   * @return true if duplicate
   */
  private boolean isDuplicateName(Dataset d, String name) {
 	  boolean taken = dataManager.getDataset(0).getXColumnName().equals(name);
 	  Iterator it = dataManager.getDatasets().iterator();
 	  while (it.hasNext()) {
 		  Dataset next = (Dataset)it.next();
 		  if (next == d) continue;
 		  taken = taken || next.getYColumnName().equals(name);
 	  }
    return taken;
  }

  /**
   * Reloads data from a Data source.
   */
  protected void reloadData(Data data) {
  	// update existing datasets in data manager
    ArrayList list = dataManager.getDatasets();
    if (list != null) {
      for (Iterator it = list.iterator(); it.hasNext();) {
      	Dataset local = (Dataset)it.next();
      	Dataset match = getMatchingID(local, data);
      	if (match != null && match != local) {
      		local.clear();
      		if (local.getYColumnName().equals(match.getXColumnName())) {
      			local.append(match.getXPoints(), match.getXPoints());
  		      local.setXYColumnNames(match.getXColumnName(), match.getXColumnName());
      		}
      		else {
      			local.append(match.getXPoints(), match.getYPoints());
  		      local.setXYColumnNames(match.getXColumnName(), match.getYColumnName());
      		}
      		local.setName(match.getName());
      	}	
      }
    }
  	// add new datasets to table
    list = data.getDatasets();
    if (list != null) {
      for (Iterator it = list.iterator(); it.hasNext();) {
      	Dataset next = (Dataset)it.next();
      	Dataset match = getMatchingID(next, dataManager);
      	if (match == null) { // next is not in this tab
      		// if next is owner of another tab, update that tab
      		DataToolTab tab = dataTool.getTab(next);
      		if (tab != null && tab != this) dataTool.update(next);
      		else addData(next);
      	}
      }
    }
    refresh();
    dataTable.refreshTable();
    statsTable.refreshStatistics();
    statsTable.refreshTable();
  }

  /**
   * Creates the GUI.
   */
  protected void createGUI() {
    setLayout(new BorderLayout());
    splitPanes = new JSplitPane[3];
    // splitPanes[0] is plot/fitter on left, tables on right
    splitPanes[0] = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    splitPanes[0].setResizeWeight(1);
    splitPanes[0].setOneTouchExpandable(true);
    // splitPanes[1] is plot on top, fitter on bottom
    splitPanes[1] = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPanes[1].setResizeWeight(1);
    splitPanes[1].setDividerSize(0);
    splitPanes[1].setOneTouchExpandable(true);
    // splitPanes[2] is stats/props tables on top, data table on bottom
    splitPanes[2] = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
    splitPanes[2].setDividerSize(0);
    splitPanes[2].setEnabled(false);
    // create data table
    dataTable = new DataToolTable(this);
    dataTable.setRowNumberVisible(true);
    dataTable.setColumnSelectionAllowed(false);
    JScrollPane dataScroller = new JScrollPane(dataTable);
    dataTable.refreshTable();
    dataTable.getColumnModel().addColumnModelListener(new TableColumnModelListener() {
      public void columnAdded(TableColumnModelEvent e) {/** empty block */}
      public void columnRemoved(TableColumnModelEvent e) {/** empty block */}
      public void columnSelectionChanged(ListSelectionEvent e) {/** empty block */}
      public void columnMarginChanged(ChangeEvent e) {/** empty block */}
      public void columnMoved(TableColumnModelEvent e) {
      	if (e.getToIndex() == 1 || e.getToIndex() == 2) {
      		// check to see if xData has changed
      	}
        selectionBox.setSize(0, 0);
        refresh();
        // construct equation string
  	    String depVar = GUIUtils.removeSubscripting(getWorkingData().getColumnName(1));
  	    String indepVar = GUIUtils.removeSubscripting(getWorkingData().getColumnName(0));
        curveFitter.eqnField.setText(depVar + " = " + //$NON-NLS-1$
                                     curveFitter.fit.getExpression(indepVar));
        if (dataTool != null) dataTool.refreshTabTitles();
      }
    });
    ListSelectionModel selectionModel = dataTable.getSelectionModel();
    selectionModel.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
        try {
          curveFitter.setData(dataTable.getSelectedData());
        }
        catch (Exception ex) {ex.printStackTrace();}
      }
    });
    // create fit action and checkbox
    fitAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        // hide/remove curveFit
        splitPanes[1].setDividerSize(splitPanes[2].getDividerSize());
        splitPanes[1].setDividerLocation(1.0);
        plot.removeDrawables(FunctionDrawer.class);
        // restore if checked
        boolean vis = fitCheckbox.isSelected();
        splitPanes[1].setEnabled(vis);
        if (vis) {
          int max = splitPanes[1].getDividerLocation();
          int h = curveFitter.getPreferredSize().height;
          splitPanes[1].setDividerSize(splitPanes[0].getDividerSize());
          splitPanes[1].setDividerLocation(max-h-10);
          plot.addDrawable(curveFitter.getDrawer());
        }
        refresh();
      }
    };
    fitCheckbox = new JCheckBox();
    fitCheckbox.setSelected(false);
    fitCheckbox.setOpaque(false);
    fitCheckbox.addActionListener(fitAction);
    // create dataBuilder button
    dataBuilderButton = DataTool.createButton(
    			ToolsRes.getString("DataToolTab.Button.DataBuilder.Text")); //$NON-NLS-1$
    dataBuilderButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Tooltip")); //$NON-NLS-1$
    dataBuilderButton.addActionListener(new ActionListener() {
		  public void actionPerformed(ActionEvent e) {
		  	if (dataTool != null) {
		     	dataTool.getDataFunctionTool().setSelectedPanel(getName());
		     	dataTool.getDataFunctionTool().setVisible(true);
		  	}
		  }
		});
    // create help button
    helpButton = DataTool.createButton(
    			ToolsRes.getString("Tool.Button.Help")); //$NON-NLS-1$
    helpButton.setToolTipText(ToolsRes.getString("Tool.Button.Help.ToolTip")); //$NON-NLS-1$
    helpButton.addActionListener(new ActionListener() {
		  public void actionPerformed(ActionEvent e) {
		  	if (dataTool != null) {
		  		dataTool.helpItem.doClick();
		  	}
		  }
		});
    // create propsAndStatsAction
    propsAndStatsAction = new AbstractAction() {
      public void actionPerformed(ActionEvent e) {
        boolean statsVis = statsCheckbox.isSelected();
        boolean propsVis = propsCheckbox.isSelected();
      	if (statsVis) statsTable.refreshStatistics();
        int statsHeight = statsTable.getPreferredSize().height;
        int propsHeight = propsTable.getPreferredSize().height;
        if (statsVis && propsVis) {
        	Box box = Box.createVerticalBox();
        	box.add(statsScroller);
        	box.add(propsScroller);
          splitPanes[2].setTopComponent(box);
          splitPanes[2].setDividerLocation(statsHeight+propsHeight+10);
        }
        else if (statsVis) {
          splitPanes[2].setTopComponent(statsScroller);
          splitPanes[2].setDividerLocation(statsHeight+4);
        }
        else if (propsVis) {
          splitPanes[2].setTopComponent(propsScroller);
          splitPanes[2].setDividerLocation(propsHeight+4);
        }
        else splitPanes[2].setDividerLocation(0);
      }
    };
    // create stats checkbox
    statsCheckbox = new JCheckBox(ToolsRes.getString("Checkbox.Statistics.Label"), false); //$NON-NLS-1$
    statsCheckbox.setOpaque(false);
    statsCheckbox.setToolTipText(ToolsRes.getString("Checkbox.Statistics.ToolTip")); //$NON-NLS-1$
    statsCheckbox.addActionListener(propsAndStatsAction);
    // create properties checkbox
    propsCheckbox = new JCheckBox(ToolsRes.getString("DataToolTab.Checkbox.Properties.Text"), true); //$NON-NLS-1$
    propsCheckbox.setToolTipText(ToolsRes.getString("DataToolTab.Checkbox.Properties.Tooltip")); //$NON-NLS-1$
    propsCheckbox.setOpaque(false);
    propsCheckbox.addActionListener(propsAndStatsAction);
    // create plotting panel
    plot = new DataToolPlotter(getWorkingData());
    plot.addDrawable(getWorkingData());
    plot.setTitle(getWorkingData().getName());
    plot.addDrawable(selectionBox);
    MouseInputListener mouseSelector = new MouseInputAdapter() {
      ArrayList rowsInside = new ArrayList(); // points inside selectionBox
      public void mousePressed(MouseEvent e) {
        Point p = e.getPoint();
        int mask = InputEvent.BUTTON3_DOWN_MASK;
        boolean rightClick = e.isPopupTrigger() || (e.getModifiersEx() & mask) == mask ||
           (e.isControlDown() && System.getProperty("os.name", "").indexOf("Mac")>-1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        selectionBox.xstart = p.x;
        selectionBox.ystart = p.y;
        rowsInside.clear();
        if(rightClick) {
          if (selectionBox.isZoomable()) {
            plot.getZoomInItem().setText(ToolsRes.getString("MenuItem.ZoomToBox")); //$NON-NLS-1$
          }
          else {
            zoomPoint = e.getPoint();
            plot.getZoomInItem().setText(ToolsRes.getString("MenuItem.ZoomIn")); //$NON-NLS-1$
          }
        }
        else {
          selectionBox.setSize(0, 0);
        }
        if (!(e.isControlDown() || e.isShiftDown() || rightClick)) {
          dataTable.clearSelection();
        }
      }
      public void mouseDragged(MouseEvent e) {
        int mask = InputEvent.BUTTON3_DOWN_MASK;
        boolean rightButton = (e.getModifiersEx() & mask) == mask ||
           (e.isControlDown() && System.getProperty("os.name", "").indexOf("Mac")>-1); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (rightButton) return;
        Dataset data = dataTable.getWorkingData();
        Point mouse = e.getPoint();
        selectionBox.visible = true;
        selectionBox.setSize(mouse.x - selectionBox.xstart,
                             mouse.y - selectionBox.ystart);
        double[] xpoints = data.getXPoints();
        double[] ypoints = data.getYPoints();
        for(int i = 0;i<xpoints.length;i++) {
          double xp = plot.xToPix(xpoints[i]);
          double yp = plot.yToPix(ypoints[i]);
          Integer index = new Integer(i);
          if (selectionBox.contains(xp, yp)) {
            if (!rowsInside.contains(index)) { // needs to be added
              rowsInside.add(index);
              dataTable.getSelectionModel().addSelectionInterval(i, i);
            }
          }
          else if (rowsInside.contains(index)) { // needs to be removed
            dataTable.getSelectionModel().removeSelectionInterval(i, i);
            rowsInside.remove(index);
          }
        }
        dataTable.getSelectedData();
        plot.repaint();
      }
      public void mouseReleased(MouseEvent e) {
        plot.repaint();
      }
    };
    plot.addMouseListener(mouseSelector);
    plot.addMouseMotionListener(mouseSelector);
    // create toolbar
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.setBorder(BorderFactory.createEtchedBorder());
    toolbar.add(propsCheckbox);
    toolbar.add(fitCheckbox);
    toolbar.add(statsCheckbox);
    toolbar.add(Box.createGlue());
    toolbar.add(dataBuilderButton);
    toolbar.add(helpButton);
    // create curve fitter
    curveFitter = new DatasetCurveFitter(getWorkingData());
    curveFitter.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("drawer") //$NON-NLS-1$
        			&& fitCheckbox.isSelected()) {
          plot.removeDrawables(FunctionDrawer.class);
          plot.addDrawable((FunctionDrawer)e.getNewValue());
        }
        plot.repaint();
      }
    });
    // create statistics table
    statsTable = new DataToolStatsTable(dataTable);
    statsScroller = new JScrollPane(statsTable) {
    	public Dimension getPreferredSize() {
    		Dimension dim = statsTable.getPreferredSize();
    		return dim;
    	}
    };
    // create properties table
    propsTable = new DataToolPropsTable(dataTable);
    propsTable.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent e) {
        if (e.getPropertyName().equals("display")) { //$NON-NLS-1$
        	refresh();
        }
      }
    });
    propsScroller = new JScrollPane(propsTable) {
    	public Dimension getPreferredSize() {
    		Dimension dim = propsTable.getPreferredSize();
    		return dim;
    	}
    };
    // assemble components
    add(toolbar, BorderLayout.NORTH);
    add(splitPanes[0], BorderLayout.CENTER);
    splitPanes[0].setLeftComponent(splitPanes[1]);
    splitPanes[0].setRightComponent(splitPanes[2]);
    splitPanes[1].setTopComponent(plot);
    splitPanes[1].setBottomComponent(curveFitter);
    splitPanes[2].setBottomComponent(dataScroller);
  }

  /**
   * Refreshes the GUI.
   */
  protected void refreshGUI() {
    dataBuilderButton.setText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Text")); //$NON-NLS-1$
    dataBuilderButton.setToolTipText(ToolsRes.getString("DataToolTab.Button.DataBuilder.Tooltip")); //$NON-NLS-1$
    statsCheckbox.setText(ToolsRes.getString("Checkbox.Statistics.Label")); //$NON-NLS-1$
    statsCheckbox.setToolTipText(ToolsRes.getString("Checkbox.Statistics.ToolTip")); //$NON-NLS-1$
    fitCheckbox.setText(ToolsRes.getString("Checkbox.Fits.Label")); //$NON-NLS-1$
    fitCheckbox.setToolTipText(ToolsRes.getString("Checkbox.Fits.ToolTip")); //$NON-NLS-1$
    helpButton.setText(ToolsRes.getString("Tool.Button.Help")); //$NON-NLS-1$
    helpButton.setToolTipText(ToolsRes.getString("Tool.Button.Help.ToolTip")); //$NON-NLS-1$
    curveFitter.refreshGUI();
    statsTable.refreshGUI();
    propsTable.refreshGUI();
  }

  	
  /**
   * Initializes this panel.
   */
  protected void init() {
    splitPanes[0].setDividerLocation(0.7);
    splitPanes[1].setDividerLocation(1.0);
    curveFitter.splitPane.setDividerLocation(0.4);
    propsAndStatsAction.actionPerformed(null);
    for (int i = 0; i < dataTable.getColumnCount(); i++) {
    	String colName = dataTable.getColumnName(i);
    	dataTable.getWorkingData(colName);
    }
  }

  /**
   * Returns the dataset with matching ID in the specified Data object. 
   * May return null.
   * 
   * @param dataset the Dataset to match
   * @param data the Data object to search
   * @return the matching Dataset, if any
   */
  private Dataset getMatchingID(Dataset dataset, Data data) {
    ArrayList list = data.getDatasets();
    if (list == null) return null;
    for (Iterator it = list.iterator(); it.hasNext();) {
    	Dataset next = (Dataset)it.next();
    	// dataset matches if it has the same ID
    	if (dataset.getID() == next.getID()) return next;
    }  		
  	return null;
  }
  
  /**
   * Returns true if the name and data match an existing column.
   * 
   * @param name the name
   * @param data the data array
   * @return true if data is a duplicate
   */
  private boolean isDuplicateColumn(String name, double[] data) {
  	Iterator it = dataManager.getDatasets().iterator();
  	while (it.hasNext()) {
  		Dataset next = (Dataset)it.next();
  		double[] y = next.getYPoints();
  		if (name.equals(next.getYColumnName())
  				&& isDuplicate(data, next.getYPoints())) {
  			// next is duplicate column: add new points if any
  			if (data.length > y.length) {
  				next.clear();
  				next.append(data, data);
	  		}  			
  			return true;
  		}
  	}
    return false;
  }
  
  /**
   * Returns true if two data arrays have identical values.
   * 
   * @param data0 data array 0
   * @param data1 data array 1
   * @return true if identical
   */
  private boolean isDuplicate(double[] data0, double[] data1) {
    int len = Math.min(data0.length, data1.length);
    for (int i = 0; i < len; i++) {
    	if (Double.isNaN(data0[i]) && Double.isNaN(data1[i])) continue;
    	if (data0[i] != data1[i]) return false;
    }
    return true;
  }
  
  /**
   * Returns true if this tab is owned by the specified Data object.
   * 
   * @param data the Data object
   * @return true if data owns this tab
   */
  protected boolean isOwnedBy(Data data) {
    // try to get name of data from getName() method
    String name = null;
    try {
      Method m = data.getClass().getMethod("getName", new Class[0]); //$NON-NLS-1$
      name = (String)m.invoke(data, new Object[0]);
    } catch(Exception ex) {/** empty block */}
    // return true if data name is the name of this tab
    if (name != null && name.equals(getName())) return true;
  	return data == owner;
  }
  
  /**
   * Refreshes the display.
   */
  public void refresh() {
    // get data for curve fitting and plotting
    curveFitter.setData(dataTable.getSelectedData());
    plot.removeDrawables(Dataset.class);
    Dataset workingData = getWorkingData();
    int labelCol = dataTable.convertColumnIndexToView(0);
    String xName = dataTable.getColumnName(labelCol == 0? 1: 0);
    Map datasets = dataTable.workingMap;
    for (Iterator it = datasets.values().iterator(); it.hasNext();) {
    	DataToolTable.WorkingDataset next = (DataToolTable.WorkingDataset)it.next();
    	String colName = GUIUtils.removeSubscripting(next.getYColumnName());
    	if (next == workingData || colName.equals(xName))
    		continue;
    	if (next.isMarkersVisible() || next.isConnected()) {
      	next.clearHighlights();
    		if (!next.isMarkersVisible()) 
    			next.setMarkerShape(Dataset.NO_MARKER);
        plot.addDrawable(next);
    	}
    }
    plot.addDrawable(workingData);
    if (fitCheckbox.isSelected()) { // draw curve fit on top of dataset
      plot.removeDrawable(curveFitter.getDrawer());
      plot.addDrawable(curveFitter.getDrawer());
    }
    plot.setTitle(workingData.getName());
    plot.setXLabel(workingData.getColumnName(0));
    plot.setYLabel(workingData.getColumnName(1));
    repaint();
  }

  class SelectionBox extends Rectangle implements Drawable {
    boolean visible = true;
    int xstart, ystart;
    int zoomSize = 10;
    Color color = new Color(0, 255, 0, 127);
    public void setSize(int w, int h) {
      int xoffset = Math.min(0, w);
      int yoffset = Math.min(0, h);
      w = Math.abs(w);
      h = Math.abs(h);
      super.setLocation(xstart+xoffset, ystart+yoffset);
      super.setSize(w, h);
    }
    public void draw(DrawingPanel drawingPanel, Graphics g) {
      if (visible) {
        Graphics2D g2 = (Graphics2D)g;
        g2.setColor(color);
        g2.draw(this);
      }
    }
    public boolean isZoomable() {
      return (getBounds().width > zoomSize && getBounds().height > zoomSize);
    }
  }


  /**
   * Class to plot datasets. This overrides DrawingPanel zoom and popup methods.
   */
  class DataToolPlotter extends PlottingPanel {

    DataToolPlotter(Dataset dataset) {
      super(dataset.getColumnName(0), dataset.getColumnName(1), ""); //$NON-NLS-1$
      setAntialiasShapeOn(true);
    }

    /**
     * Gets the zoomIn menu item.
     */
    protected JMenuItem getZoomInItem() {
      return zoomInItem;
    }

    /**
     * Zooms out by a factor of two.
     */
    protected void zoomOut() {
      double dx = xmax-xmin;
      double dy = ymax-ymin;
      setPreferredMinMax(xmin-dx/2, xmax+dx/2, ymin-dy/2, ymax+dy/2);
      validImage = false;
      selectionBox.setSize(0, 0);
      repaint();
    }

    /**
     * Zooms in to the selection box.
     */
    protected void zoomIn() {
      int w = selectionBox.getBounds().width;
      int h = selectionBox.getBounds().height;
      if (selectionBox.isZoomable()) {
        int x = selectionBox.getBounds().x;
        int y = selectionBox.getBounds().y;
        double xmin = pixToX(x);
        double xmax = pixToX(x+w);
        double ymax = pixToY(y);
        double ymin = pixToY(y+h);
        setPreferredMinMax(xmin, xmax, ymin, ymax); // zoom both axes
        validImage = false;
        selectionBox.setSize(0, 0);
        repaint();
      }
      else if (zoomPoint != null) {
        double dx = xmax-xmin;
        double dy = ymax-ymin;
        double xcenter = pixToX(zoomPoint.x);
        double ycenter = pixToY(zoomPoint.y);
        setPreferredMinMax(xcenter-dx/4, xcenter+dx/4, ycenter-dy/4, ycenter+dy/4);
        validImage = false;
        selectionBox.setSize(0, 0);
        repaint();
      }
    }

    protected void buildPopupmenu() {
      popupmenu.setEnabled(true);
      // create zoom menu items
      zoomInItem = new JMenuItem(ToolsRes.getString("MenuItem.ZoomIn")); //$NON-NLS-1$
      zoomInItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          plot.zoomIn();
        }
      });
      popupmenu.add(zoomInItem);
      zoomOutItem = new JMenuItem(ToolsRes.getString("MenuItem.ZoomOut")); //$NON-NLS-1$
      zoomOutItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          plot.zoomOut();
        }
      });
      popupmenu.add(zoomOutItem);
      JMenuItem zoomFitItem = new JMenuItem(ToolsRes.getString("MenuItem.ZoomToFit")); //$NON-NLS-1$
      zoomFitItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          plot.setAutoscaleX(true);
          plot.setAutoscaleY(true);
          selectionBox.setSize(0, 0);
          refresh();
        }
      });
      popupmenu.add(zoomFitItem);
      scaleItem = new JMenuItem(ToolsRes.getString("MenuItem.Scale")); //$NON-NLS-1$
      scaleItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          ScaleInspector plotInspector = new ScaleInspector(DataToolPlotter.this);
          plotInspector.setLocationRelativeTo(DataToolPlotter.this);
          plotInspector.updateDisplay();
          plotInspector.setVisible(true);
        }
      });
      popupmenu.add(scaleItem);
      popupmenu.addSeparator();
      snapshotItem = new JMenuItem(ToolsRes.getString("MenuItem.Snapshot")); //$NON-NLS-1$
      snapshotItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          snapshot();
        }
      });
      popupmenu.add(snapshotItem);
      popupmenu.addSeparator();
      propertiesItem = new JMenuItem(ToolsRes.getString("MenuItem.Inspect")); //$NON-NLS-1$
      propertiesItem.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          showInspector();
        }
      });
      popupmenu.add(propertiesItem);
    }
  }

//__________________________ static methods ___________________________

  /**
   * Returns an ObjectLoader to save and load data for this class.
   *
   * @return the object loader
   */
  public static XML.ObjectLoader getLoader() {
    return new Loader();
  }

  /**
   * A class to save and load data for this class.
   */
  static class Loader implements XML.ObjectLoader {

    public void saveObject(XMLControl control, Object obj) {
      DataToolTab tab = (DataToolTab)obj;
      // save name
      control.setValue("name", tab.getName()); //$NON-NLS-1$
      // save datasets but leave out data functions
      DatasetManager data = new DatasetManager();
      ArrayList functions = new ArrayList();
      for (Iterator it = tab.dataManager.getDatasets().iterator(); it.hasNext();) {
      	Dataset next = (Dataset)it.next();
      	if (next instanceof DataFunction) functions.add(next);
      	else data.addDataset(next);
      }
      control.setValue("data", data); //$NON-NLS-1$
      // save data functions
      if (!functions.isEmpty()) {
    		DataFunction[] f = (DataFunction[])functions.toArray(new DataFunction[0]);
        control.setValue("data_functions", f); //$NON-NLS-1$
      }
      // save fit function panels
      if (tab.curveFitter.fitBuilder != null) {
	      ArrayList fits = new ArrayList(tab.curveFitter.fitBuilder.panels.values());
	      control.setValue("fits", fits); //$NON-NLS-1$
      }
      // save selected fit name
      control.setValue("selected_fit", tab.curveFitter.getSelectedFitName()); //$NON-NLS-1$
      // save autofit status
      control.setValue("autofit", tab.curveFitter.autofitCheckBox.isSelected()); //$NON-NLS-1$
      // save fit color
      control.setValue("fit_color", tab.curveFitter.color); //$NON-NLS-1$
      // save fit visibility
      control.setValue("fit_visible", tab.fitCheckbox.isSelected()); //$NON-NLS-1$
      // save props visibility
      control.setValue("props_visible", tab.propsCheckbox.isSelected()); //$NON-NLS-1$
      // save statistics visibility
      control.setValue("stats_visible", tab.statsCheckbox.isSelected()); //$NON-NLS-1$
      // save splitPane location
      int loc = tab.splitPanes[0].getDividerLocation();
      control.setValue("split_pane", loc); //$NON-NLS-1$
      // save x and y working data names
      Dataset d = tab.getWorkingData();
      String[] names = new String[] {d.getXColumnName(), d.getYColumnName()};
      control.setValue("working_columns", names); //$NON-NLS-1$
    }

    public Object createObject(XMLControl control){
      // load data
      DatasetManager data = (DatasetManager)control.getObject("data"); //$NON-NLS-1$
      return new DataToolTab(data, null);
    }

    public Object loadObject(XMLControl control, Object obj) {
      final DataToolTab tab = (DataToolTab)obj;      
      // load tab name
      tab.setName(control.getString("name")); //$NON-NLS-1$
      // load data functions
      Iterator it = control.getPropertyContent().iterator();
      while (it.hasNext()) {
      	XMLProperty prop = (XMLProperty)it.next();
      	if (prop.getPropertyName().equals("data_functions")) { //$NON-NLS-1$
        	XMLControl[] children = prop.getChildControls();
        	for (int i = 0; i < children.length; i++) {
        		DataFunction f = new DataFunction(tab.dataManager);
        		children[i].loadObject(f);
          	f.setXColumnVisible(false);
          	tab.dataManager.addDataset(f);
        	}
          // refresh dataFunctions
          ArrayList datasets = tab.dataManager.getDatasets();
          for (int i = 0; i < datasets.size(); i++) {
            if (datasets.get(i) instanceof DataFunction) {
            	((DataFunction)datasets.get(i)).refreshFunctionData();
            }    	
          }
          break;
      	}
      }
      // load user fit function panels
      ArrayList fits = (ArrayList )control.getObject("fits"); //$NON-NLS-1$
      if (fits != null) {
        for (it = fits.iterator(); it.hasNext();) {
        	FitFunctionPanel panel = (FitFunctionPanel)it.next();
          tab.curveFitter.addUserFit(panel);
        }
      }
      // select fit
      String fitName = control.getString("selected_fit"); //$NON-NLS-1$
      tab.curveFitter.fitDropDown.setSelectedItem(fitName);
      // load autofit
      boolean autofit = control.getBoolean("autofit"); //$NON-NLS-1$
      tab.curveFitter.autofitCheckBox.setSelected(autofit);
      // load fit color
      Color color = (Color)control.getObject("fit_color"); //$NON-NLS-1$
      tab.curveFitter.setColor(color);
      // load fit visibility
      boolean vis = control.getBoolean("fit_visible"); //$NON-NLS-1$
      tab.fitCheckbox.setSelected(vis);
      // load props visibility
      vis = control.getBoolean("props_visible"); //$NON-NLS-1$
      tab.propsCheckbox.setSelected(vis);
      // load stats visibility
      vis = control.getBoolean("stats_visible"); //$NON-NLS-1$
      tab.statsCheckbox.setSelected(vis);
      // load splitPane location
      final int loc = control.getInt("split_pane"); //$NON-NLS-1$
      // load working columns
      String[] names = (String[])control.getObject("working_columns"); //$NON-NLS-1$
      tab.dataTable.setWorkingColumns(names[0], names[1]);
      Runnable runner = new Runnable() {
        public synchronized void run() {
          tab.fitAction.actionPerformed(null);
          tab.propsAndStatsAction.actionPerformed(null);
          tab.splitPanes[0].setDividerLocation(loc);
          tab.dataTable.refreshTable();
        }
      };
      SwingUtilities.invokeLater(runner);
      return obj;
    }
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
