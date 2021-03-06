/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.frames;
import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.text.DecimalFormat;
import java.util.Arrays;

/**
 * A table model for this frame.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class TableData extends AbstractTableModel {
  ArrayList rowList = new ArrayList();
  ArrayList colNames = new ArrayList();
  ArrayList formats = new ArrayList();
  boolean columnVisibilities[]= new boolean[1]; // boolean values indicating if a column is visible
  boolean rowNumberVisible = true;
  int colCount = 0, maxRows=-1;

  public TableData() {
     columnVisibilities[0]=true;
  }

  /**
   * Sets the maximum number of rows the data can hold
   */
  public void setMaxPoints(int max) { // Paco added this method
    maxRows = max;
    if (maxRows<=0 || rowList.size()<=max) return;
    // Reset the table to that size
    for (int j=0,n=rowList.size()-max; j<n; j++) rowList.remove(0);
    colCount = 0;
    for (int j=0,n=rowList.size(); j<n; j++) {
      Object r = rowList.get(j);
      if(!r.getClass().isArray()) continue;
      int length = 0;
      if(r instanceof double[])      length = ((double[]) r).length;
      else if(r instanceof byte[])   length = ((byte[])   r).length;
      else if(r instanceof int[])    length = ((int[])    r).length;
      else if(r instanceof String[]) length = ((String[]) r).length;
      colCount = Math.max(colCount,length);
    }
  }

  /**
   * Clear the data
   */
  public void clear() { // Paco added this method
    rowList.clear();
    colCount = 0;
  }

  /**
   * Appends a  row to this table.
   *
   * @param obj Object
   * @throws IllegalArgumentException
   */
  public synchronized void appendRow(Object obj) throws IllegalArgumentException {
    if(!obj.getClass().isArray()) {
      throw new IllegalArgumentException("A TableData row must be an array.");
    }
    // make sure ultimate component class is acceptable
    Class componentType = obj.getClass().getComponentType();
    String type = componentType.getName();
    if(type.equals("double")) {
      appendDoubles((double[]) obj);
    } else if(type.equals("int")) {
      appendInts((int[]) obj);
    } else if(type.equals("byte")) {
      appendBytes((byte[]) obj);
    } else if(type.equals("string")) {
      appendStrings((String[]) obj);
    } else {
      Object[] row = (Object[]) obj;
      String[] strings = new String[row.length];
      for(int i = 0, n = row.length;i<n;i++) {
        strings[i] = row[i].toString();
      }
      appendStrings(strings);
    }
  }

  /**
   * Appends a row of data.
   *
   * @param x double[]
   */
  void appendDoubles(double[] x) {
    double[] row;
    if(x==null) {
      return;
    }
    row = new double[x.length];
    System.arraycopy(x, 0, row, 0, x.length);
    if (maxRows>0 && rowList.size()>=maxRows) rowList.remove(0); // Paco added this line
    rowList.add(row);
    colCount = Math.max(colCount, row.length+1);
  }

  /**
   * Appends a row of data.
   *
   * @param x double[]
   */
  void appendInts(int[] x) {
    int[] row;
    if(x==null) {
      return;
    }
    row = new int[x.length];
    System.arraycopy(x, 0, row, 0, x.length);
    if (maxRows>0 && rowList.size()>=maxRows) rowList.remove(0); // Paco added this line
    rowList.add(row);
    colCount = Math.max(colCount, row.length+1);
  }

  /**
   * Appends a row of data.
   *
   * @param x double[]
   */
  void appendBytes(byte[] x) {
    byte[] row;
    if(x==null) {
      return;
    }
    row = new byte[x.length];
    System.arraycopy(x, 0, row, 0, x.length);
    if (maxRows>0 && rowList.size()>=maxRows) rowList.remove(0); // Paco added this line
    rowList.add(row);
    colCount = Math.max(colCount, row.length+1);
  }

  /**
   * Appends a row of data.
   *
   * @param x double[]
   */
  void appendStrings(String[] x) {
    String[] row;
    if(x==null) {
      return;
    }
    row = new String[x.length];
    System.arraycopy(x, 0, row, 0, x.length);
    if (maxRows>0 && rowList.size()>=maxRows) rowList.remove(0); // Paco added this line
    rowList.add(row);
    colCount = Math.max(colCount, row.length+1);
  }

  /**
   *  Method setColumnVisible
   *
   * @param  columnIndex
   * @param  visible
   */
  public void setColumnVisible(int columnIndex, boolean visible){
     ensureCapacity(columnIndex+1);
     columnVisibilities[columnIndex] = visible;
  }


  /**
   *  Sets the display row number flag. Table displays row number.
   *
   * @param  vis  <code>true<\code> if table display row number
   */
  public void setRowNumberVisible(boolean vis) {
    rowNumberVisible = vis;
  }

  /**
   *  Sets the column names in a JTable.
   *
   * @param  column  the column index
   * @param  name
   */
  public void setColumnNames(int column, String name) {
    while(column>=colNames.size()) {
      colNames.add(""+(char) ('A'+column));
    }
    colNames.set(column, name);
  }

  /**
   *  Sets the column decimal format.
   *
   * @param  column  the column index
   * @param  format  the format
   */
  public void setColumnFormat(int column, String format) {
    while(column>=formats.size()) {
      formats.add(null);
    }
    formats.set(column, new DecimalFormat(format));
  }

  /**
   * Gets the column decimal format.
   * @return  the format
   */
  public DecimalFormat getColumnFormat(int column){
     for (int i = 0, n = Math.min(column, columnVisibilities.length); i<n; i++){
        if (!columnVisibilities[i]) column++; // add one for every invisible column
     }
     if (column<=formats.size()){
        return (DecimalFormat) formats.get(column-1);
     }
     return null;
  }


  /**
   * Gets the number of columns.
   *
   * @return the column count
   */
  public int getColumnCount() {
    if(getRowCount()==0) {
      return 0;
    }
    int count=(rowNumberVisible) ? colCount : colCount-1;
    for(int i=0, n=Math.min(count,columnVisibilities.length); i<n; i++){
       if (!columnVisibilities[i]) count--; // subtract one for every invisible column
    }
    return count;
  }

  /**
   * Gets the name of the specified column.
   *
   * @param column the column index
   * @return the column name
   */
  public String getColumnName(int column) {
    if(column==0&&rowNumberVisible) {
      return "row #";
    }
    if(!rowNumberVisible) {
      column++;
    }
    for (int i = 0, n = Math.min(column, columnVisibilities.length); i<n; i++){
       if (!columnVisibilities[i]) column++; // add one for every invisible column
    }
    if(column<=colNames.size()) {
      return(String) colNames.get(column-1);
    }
    return ""+(char) ('A'+column);
  }

  /**
   * Gets the number of rows.
   *
   * @return the row count
   */
  public int getRowCount() {
    return rowList.size();
  }

  /**
   * Gets the value at the given cell.
   *
   * @param row the row index
   * @param column the column index
   * @return the value
   */
  public Object getValueAt(int row, int column) {
    if(column==0&&rowNumberVisible) {
      return new Integer(row);
    }
    if(!rowNumberVisible) {
      column++;
    }
    if(row>=rowList.size()) {
      return "";
    }
    Object r = rowList.get(row);
    if(!r.getClass().isArray()) {
      return "";
    }
    for (int i = 0, n = Math.min(column, columnVisibilities.length); i<n; i++){
       if (!columnVisibilities[i]) column++; // add one for every invisible column
    }
    if(r instanceof double[]) {
      double[] array = (double[]) r;
      if(column>array.length) {
        return "";
      }
      DecimalFormat format = getColumnFormat(column);
      if(format==null) {
        return new Double(array[column-1]);
      } else {
        return format.format(array[column-1]);
      }
    }
    if(r instanceof byte[]) {
      byte[] array = (byte[]) r;
      if(column>array.length) {
        return "";
      }
      return new Byte(array[column-1]);
    }
    if(r instanceof int[]) {
      int[] array = (int[]) r;
      if(column>array.length) {
        return "";
      }
      return new Integer(array[column-1]);
    }
    if(r instanceof String[]) {
      String[] array = (String[]) r;
      if(column>array.length) {
        return "";
      }
      return array[column-1];
    }
    return "";
  }

  private void ensureCapacity(int minimumCapacity) {
   if(columnVisibilities==null) {
      columnVisibilities = new boolean[(minimumCapacity*3)/2+1];
      Arrays.fill(columnVisibilities, true);
   } else if(columnVisibilities.length<minimumCapacity) {
      boolean[] temp = columnVisibilities;
      columnVisibilities = new boolean[(minimumCapacity*3)/2+1];
      System.arraycopy(temp, 0, columnVisibilities, 0, temp.length);
      Arrays.fill(columnVisibilities, temp.length, columnVisibilities.length, true);
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
