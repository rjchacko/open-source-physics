/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.tools;

import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import org.opensourcephysics.display.*;

/**
 * This is a DataTable for working with highlightable datasets.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class DataToolTable extends DataTable {
	
	protected static Color xAxisColor = new Color(255, 255, 153); //yellow
	protected static Color yAxisColor = new Color(204, 255, 204); //light green

	
	/** tab that displays this table */
	DataToolTab dataToolTab;
	 
	/** dataManager contains all datasets displayed in the table */
	DatasetManager dataManager;
	 
	/** workingData contains the first two table data columns in x-y order */
	WorkingDataset workingData;
  
  /** selectedData contains the selected rows of workingData */
  HighlightableDataset selectedData = new HighlightableDataset();
  
  /** cell renderer for the table header */
  HeaderRenderer sortRenderer;
  
  /** cell renderer for the labels */
  LabelRenderer labelRenderer = new LabelRenderer();
  
  /** model rows in the current selection */
  int[] selectedModelRows = new int[0];
  
  /** prevents changes to selectedModelRows when true */ 
  boolean ignoreRefresh = false;
  
  /** maps variable name to workingDataset */ 
  HashMap workingMap = new HashMap();
  
  /**
   * Constructs a DataToolTable for the specified Dataset manager.
   *
   * @param datasets the Dataset array
   */
  public DataToolTable(DataToolTab tab) {
  	dataToolTab = tab;
    dataManager = tab.dataManager;
  	add(dataManager);
    setRowNumberVisible(true);
    setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    sortRenderer = new HeaderRenderer(getTableHeader().getDefaultRenderer());
    getTableHeader().setDefaultRenderer(sortRenderer);
    getTableHeader().setToolTipText(ToolsRes.getString(
				"DataToolTable.Header.Tooltip")); //$NON-NLS-1$
    getTableHeader().addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent e) {
				TableColumnModel tcm = getColumnModel();
				final int col = tcm.getColumnIndexAtX(e.getX());
        if(e.isPopupTrigger()
        			||e.getButton()==MouseEvent.BUTTON3
        			||(e.isControlDown()&&System.getProperty
        			("os.name", "").indexOf("Mac")>-1)) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
          int labelCol = convertColumnIndexToView(0);
          int yCol = labelCol < 2? 2: 1;
        	if (col == labelCol || col <= yCol) return;
          // make popup with close item
        	String text = ToolsRes.getString(
    					"DataToolTable.Popup.MenuItem.DeleteColumn"); //$NON-NLS-1$
        	String var = getColumnName(col);
        	text += " \"" +var+"\""; //$NON-NLS-1$ //$NON-NLS-2$  
          JMenuItem item = new JMenuItem(text);
          JPopupMenu popup = new JPopupMenu();
          popup.add(item);
          item.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	String name = getColumnName(col);
            	Dataset data = getDataset(name);
            	if (data == null) return;
            	int i = dataManager.getDatasetIndex(data.getYColumnName());
            	if (data instanceof DataFunction) {
            		FunctionTool tool = dataToolTab.dataTool.getDataFunctionTool();
            		FunctionPanel panel = tool.getPanel(dataToolTab.getName());
            		panel.functionEditor.removeObject(data, true);
            	}
            	else {
	            	dataManager.removeDataset(i);
	            	workingMap.remove(name);
            	}
            	refreshTable();
            }
          });
          popup.show(getTableHeader(), e.getX(), e.getY()+8);
        }
        else {
          // set sort renderer column
  				sortRenderer.sortedColumn = convertColumnIndexToModel(col);
  				// select previously selected model rows in table
  				ignoreRefresh = true;
  				clearSelection();
  				int rowViewCol = convertColumnIndexToView(0);
  				for (int j = 0; j < selectedModelRows.length; j++) {
  					// find table row with desired model row number
  			  	for (int i = 0; i < getRowCount(); i++) {
  			  		Integer val = (Integer)getModel().getValueAt(i, rowViewCol);
  			  		if (val.intValue() == selectedModelRows[j]) { // found the row
  			  			addRowSelectionInterval(i, i);
  			  			break;
  			  		}
  			  	}
  				}
  				ignoreRefresh = false;
  				// refresh working data and highlights
  				getSelectedData();
        }
			}
		});
    ListSelectionModel selectionModel = getSelectionModel();
    selectionModel.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent e) {
      	if (ignoreRefresh) return;
		  	int[] selectedRows = getSelectedRows(); // selected view rows
		  	selectedModelRows = new int[selectedRows.length];
		  	int col = convertColumnIndexToView(0);
		  	for (int i = 0; i < selectedRows.length; i++) {
		  		int row = selectedRows[i];
		  		Integer val = (Integer)getModel().getValueAt(row, col);
		  		selectedModelRows[i] = val.intValue();
		  	}
      }
    });
    addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent e) {
        // clear selection if pressed on the row column
        if (convertColumnIndexToModel(getSelectedColumn()) == 0) {
          removeRowSelectionInterval(0, getRowCount()-1);
          selectedData.clearHighlights();
        }
      }
    });
  }

  /**
   * Gets the working data for a specified column name.
   * The working y-data is the named table column
   * The working x-data is the x (yellow) table column
   *
   * @param colName the name of the data column
   * @return the working dataset
   */
  protected WorkingDataset getWorkingData(String colName) {
  	if (colName == null) return null;
  	// find or create working data
  	WorkingDataset working = (WorkingDataset)workingMap.get(colName);
  	if (working == null) {
    	Dataset ySource = getDataset(colName);
    	if (ySource == null) return null;
  		working = new WorkingDataset(ySource);
  		workingMap.put(colName, working);
  	}
  	// set x-column source of working data
    int labelCol = convertColumnIndexToView(0);
    String xName = getColumnName(labelCol == 0? 1: 0);
  	Dataset xSource = getDataset(xName);
  	if (xSource == null) return null;
		working.setXSource(xSource);
  	return working;
  }

  /**
   * Gets the working data: first two data columns in x-y order
   *
   * @return the working dataset
   */
  protected WorkingDataset getWorkingData() {
    int labelCol = convertColumnIndexToView(0);
    int yCol = labelCol < 2? 2: 1;
    String yName = getColumnName(yCol);
    workingData = getWorkingData(yName);
  	return workingData;
  }

  /**
   * Gets the source dataset associated with table column name.
   *
   * @return the dataset
   */
  protected Dataset getDataset(String colName) {
  	int i = dataManager.getDatasetIndex(colName);
  	if (i > -1) {
  		return dataManager.getDataset(i);
  	}
  	// check all datasets in dataManager to see if subscripting removed
  	java.util.Iterator it = dataManager.getDatasets().iterator();
  	while (it.hasNext()) {
  		Dataset next = (Dataset)it.next();
  		if (GUIUtils.removeSubscripting(next.getYColumnName()).equals(colName))
  			return next;
  	}
  	return null;
  }

  /**
   * Gets the selected data. The returned dataset consists of the selected
   * rows in the first two columns of the table in x-y order.
   * This also sets the highlights of the working data.
   *
   * @return the data in the selected rows, or all data if no rows are selected
   */
  protected HighlightableDataset getSelectedData() {
  	getWorkingData();
    double[] xValues, yValues;
    double[] x = workingData.getXPoints();
    double[] y = workingData.getYPoints();
    workingData.clearHighlights();
    if (getSelectedRowCount() == 0) { // nothing selected
      xValues = x;
      yValues = y;
    }
    else {
      int[] rows = selectedModelRows;
      xValues = new double[rows.length];
      yValues = new double[rows.length];
      for (int i = 0; i < rows.length; i++) {
      	if (rows[i] >= x.length) {
          xValues[i] = Double.NaN;
      		continue;
      	}
        xValues[i] = x[rows[i]];
        yValues[i] = y[rows[i]];
        workingData.setHighlighted(rows[i], true);
      }
    }
    selectedData.clear();
    selectedData.append(xValues, yValues);
    selectedData.setXYColumnNames(workingData.getColumnName(0),
                                    workingData.getColumnName(1));
    selectedData.setMarkerShape(workingData.getMarkerShape());
    selectedData.setMarkerSize(workingData.getMarkerSize());
    selectedData.setConnected(workingData.isConnected());
    selectedData.setLineColor(workingData.getLineColor());
    selectedData.setName(workingData.getName());
    selectedData.setMarkerColor(workingData.getFillColor(), workingData.getEdgeColor());
    return selectedData;
  }
  
  /**
   * Deselects all selected columns and rows. Overrides JTable method.
   */
  public void clearSelection() {
    if (workingData != null) {
      workingData.clearHighlights();
      selectedData.clearHighlights();
    }
    super.clearSelection();
  }

  /**
   * Refreshes the data in the table. Overrides DataTable method.
   */
  public void refreshTable() {
  	// save model column order
  	TableModel model = getModel();
  	boolean noView = convertColumnIndexToView(0) == -1;
  	int[] modelColumns = new int[model.getColumnCount()];
  	for (int i = 0; i < modelColumns.length; i++) {
  		modelColumns[i] = convertColumnIndexToModel(i);
  	}
  	// save selected rows
  	int[] selectedRows = getSelectedRows();
    super.refreshTable();
    if (noView) return;
    // restore column order
  	for (int i = 0; i < modelColumns.length; i++) {
  		// for each model column i
  		for (int j = i; j < modelColumns.length; j++) {
  			// find its current view column and move it
  			if (convertColumnIndexToModel(j) == modelColumns[i]) {
  	  		moveColumn(j, i);
  	  		break;
  			}
  		}
  	}
    // restore selected rows
  	for (int i = 0; i < selectedRows.length; i++) {
    	addRowSelectionInterval(selectedRows[i], selectedRows[i]);
  	}
  }

  /**
   * Sets the working columns by name.
   * 
   * @param xColName the name of the horizontal axis variable
   * @param yColName the name of the vertical axis variable
   */
  public void setWorkingColumns(String xColName, String yColName) {
  	// move labels to column 0
  	int labelCol = convertColumnIndexToView(0);
  	getColumnModel().moveColumn(labelCol, 0);
  	xColName = GUIUtils.removeSubscripting(xColName);
  	yColName = GUIUtils.removeSubscripting(yColName);
  	// find xCol and move to column 1
  	for (int i = 1; i < this.getColumnCount(); i++) {
  		if (xColName.equals(getColumnName(i))) {
  	  	getColumnModel().moveColumn(i, 1);
  	  	break;
  		}
  	}
  	// find y and move to column 2  	
  	for (int i = 2; i < this.getColumnCount(); i++) {
  		if (yColName.equals(getColumnName(i))) {
  	  	getColumnModel().moveColumn(i, 2);
  	  	break;
  		}
  	}
  }

  /**
   * Sets the label column width
   */
  protected void setLabelColumnWidth(int w) {
    labelColumnWidth = w;
  }

  /**
   * A header cell renderer that identifies sorted, working and comparison columns.
   */
  class HeaderRenderer implements TableCellRenderer {
  	
  	int sortedColumn;
  	TableCellRenderer renderer;

    /**
     * Constructor
     */
    public HeaderRenderer(TableCellRenderer renderer) {
      this.renderer = renderer;
    }

    /**
     * Returns a label for the specified cell.
     *
     * @param table ignored
     * @param value the row number to be displayed
     * @param isSelected ignored
     * @param hasFocus ignored
     * @param row ignored
     * @param column the column number
     * @return a label with the row number
     */
    public Component getTableCellRendererComponent(JTable table, 
    			Object value, boolean isSelected, boolean hasFocus, 
    			int row, int col) {
    	Component c = renderer.getTableCellRendererComponent(
    				table, value, isSelected, hasFocus, row, col);
    	if (c instanceof JLabel) {
    		JLabel label = (JLabel)c;
        int labelCol = convertColumnIndexToView(0);
        int xCol = labelCol == 0? 1: 0;
        int yCol = labelCol < 2? 2: 1;
        if (col == xCol) label.setBackground(xAxisColor);
        else if (col == yCol) label.setBackground(yAxisColor);
        col = convertColumnIndexToModel(col);
        if (col == sortedColumn) {
          label.setFont(label.getFont().deriveFont(Font.BOLD));
        }
    	}
      return c;
    }
  }
  
  /**
   * A dataset whose y values and display properties depend on a source dataset.
   */
  class WorkingDataset extends HighlightableDataset {
  	
  	private Dataset yData;
  	private Dataset xData;
  	boolean markersVisible;
  	int markerType;
  	
  	public WorkingDataset(Dataset yDataset) {
  		yData = yDataset;
  		setColor(yData.getFillColor(), yData.getLineColor());
  		markerType = yData.getMarkerShape();
  		setMarkerShape(markerType);
  		markersVisible = (markerType != Dataset.NO_MARKER);
  		if (markerType == Dataset.NO_MARKER)
  			markerType = Dataset.CIRCLE;
  		setMarkerSize(yData.getMarkerSize());
  		setConnected(yData.isConnected());
  	}
  	
    public boolean isMarkersVisible() {
      return markersVisible;
    }

    public void setMarkersVisible(boolean visible) {
    	if (!visible && markersVisible) {
    		markerType = getMarkerShape();
    		setMarkerShape(Dataset.NO_MARKER);
    	}
    	else if (visible) {
    		setMarkerShape(markerType);
    	}
  		markersVisible = visible;
    }

    public void setColor(Color edgeColor, Color lineColor) {
      Color fill = new Color(edgeColor.getRed(), edgeColor.getGreen(),
            edgeColor.getBlue(), 100);
      setMarkerColor(fill, edgeColor);
      setLineColor(lineColor);
      yData.setMarkerColor(fill, edgeColor);
      yData.setLineColor(lineColor);
    }

    public void setConnected(boolean connected) {
      super.setConnected(connected);
      yData.setConnected(connected);
    }

    public void setMarkerSize(int size) {
      super.setMarkerSize(size);
      yData.setMarkerSize(size);
    }

    public void setMarkerShape(int shape) {
      super.setMarkerShape(shape);
      if (shape != Dataset.NO_MARKER) {
      	yData.setMarkerShape(shape);
    		markerType = shape;
      }
    }
    
    Dataset getYSource() {
    	return yData;
    }
    
    Dataset getXSource() {
    	return xData;
    }
    
    void setXSource(Dataset xDataset) {
    	xData = xDataset;
      clear();
      double[] x = xData.getYPoints();
      double[] y = yData.getYPoints();
      if (x.length != y.length) {
  	  	int n = Math.min(x.length, y.length);
  	  	double[] nx = new double[n];
  	  	System.arraycopy(x, 0, nx, 0, n);
  	  	double[] ny = new double[n];
  	  	System.arraycopy(y, 0, ny, 0, n);
  	  	append(nx, ny);
      }
      else append(x, y);
      setXYColumnNames(xData.getYColumnName(), yData.getYColumnName());
    }

  }
  
  /**
   * A class to render labels.
   */
  class LabelRenderer extends JLabel implements TableCellRenderer {
  	
    public LabelRenderer() {
      setOpaque(true); // make background visible.
      setHorizontalAlignment(SwingConstants.RIGHT);
      setForeground(Color.black);
      setBackground(javax.swing.UIManager.getColor("Panel.background")); //$NON-NLS-1$
    }

    public Component getTableCellRendererComponent(JTable table, Object value,
        boolean isSelected, boolean hasFocus, int row, int col) {
      setText(value.toString());
      return this;
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
