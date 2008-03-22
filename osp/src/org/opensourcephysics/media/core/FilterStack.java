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

import java.beans.*;
import java.util.*;

import java.awt.image.*;
import javax.swing.JDialog;

/**
 * This is a Filter that contains and manages a series of Filters.
 *
 * @author Douglas Brown
 * @version 1.0
 */
public class FilterStack extends Filter implements PropertyChangeListener {

  // instance fields
  private ArrayList filters = new ArrayList();
  private Filter postFilter;
  private int indexRemoved = -1;

  /**
   * Constructs a FilterStack object.
   */
  public FilterStack() {/** empty block */}

  /**
   * Adds a filter to the end of the stack. Multiple filters are applied
   * in the order they are added.
   *
   * @param filter the filter
   */
  public void addFilter(Filter filter) {
    filters.add(filter);
    filter.stack = this;
    filter.addPropertyChangeListener(this);
    support.firePropertyChange("image", null, null); //$NON-NLS-1$
    support.firePropertyChange("filter", null, filter); //$NON-NLS-1$
  }

  /**
   * Adds a filter at the specified index. Multiple filters are applied
   * in index order.
   *
   * @param filter the filter
   */
  public void insertFilter(Filter filter, int index) {
  	index = Math.min(index, filters.size());
  	index = Math.max(index, 0);
    filters.add(index, filter);
    filter.stack = this;
    filter.addPropertyChangeListener(this);
    support.firePropertyChange("image", null, null); //$NON-NLS-1$
    support.firePropertyChange("filter", null, filter); //$NON-NLS-1$
  }

  /**
   * Gets the index of the last removed filter, or -1 if none removed.
   *
   * @return the stack index.
   */
  public int lastIndexRemoved() {
    return indexRemoved;
  }

  /**
   * Sets the post filter. If non-null, the post filter is applied after all
   * other filters.
   *
   * @param filter a filter
   */
  public void setPostFilter(Filter filter) {
    if (postFilter != null) {
      postFilter.removePropertyChangeListener(this);
    }
    postFilter = filter;
    if (filter != null) {
      filter.addPropertyChangeListener(this);
      support.firePropertyChange("image", null, null); //$NON-NLS-1$
      support.firePropertyChange("filter", null, filter); //$NON-NLS-1$
    }
  }

  /**
   * Gets the post filter.
   *
   * @return the post filter
   */
  public Filter getPostFilter() {
    return postFilter;
  }

  /**
   * Gets the first instance of the specified filter class. May return null.
   *
   * @param filterClass the filter class
   * @return the first filter of the specified class, if any
   */
  public Filter getFilter(Class filterClass) {
    Iterator it = filters.iterator();
    while (it.hasNext()) {
      Filter filter = (Filter)it.next();
      if (filter.getClass() == filterClass)
        return filter;
    }
    return null;
  }

  /**
   * Removes the specified filter from the stack.
   *
   * @param filter the filter
   */
  public void removeFilter(Filter filter) {
  	indexRemoved = filters.indexOf(filter);
  	if (indexRemoved > -1) {
  		filters.remove(filter);
      filter.stack = null;
  		filter.removePropertyChangeListener(this);
      support.firePropertyChange("image", null, null); //$NON-NLS-1$
      support.firePropertyChange("filter", filter, null); //$NON-NLS-1$
    }
  }

  /**
   * Clears the filter stack.
   */
  public void clear() {
    Iterator it = filters.iterator();
    while (it.hasNext()) {
      Filter filter = (Filter)it.next();
      filter.stack = null;
      filter.removePropertyChangeListener(this);
    }
    filters.clear();
    support.firePropertyChange("image", null, null); //$NON-NLS-1$
    support.firePropertyChange("filter", null, null); //$NON-NLS-1$
  }

  /**
   * Returns true if this contains no filters.
   *
   * @return <code>true</code> if this is empty
   */
  public boolean isEmpty() {
    return filters.isEmpty() && postFilter == null;
  }

  /**
   * Returns a copy of the filters in this filter stack.
   *
   * @return a collection of filters
   */
  public Collection getFilters() {
    return (Collection)filters.clone();
  }

  /**
   * Returns the current filtered image.
   *
   * @param image the image to filter
   * @return the filtered image
   */
  public BufferedImage getFilteredImage(BufferedImage image) {
    if (!isEnabled()) return image;
    Iterator it = filters.iterator();
    while (it.hasNext()) {
      Filter filter = (Filter)it.next();
      image = filter.getFilteredImage(image);
    }
    if (postFilter != null) {
      image = postFilter.getFilteredImage(image);
    }
    return image;
  }

  /**
   * Implements abstract Filter method.
   *
   * @return the inspector
   */
  public JDialog getInspector() {
    return null;
  }

  /**
   * Shows/hides all inspectors.
   *
   * @param vis true to show inspectors
   */
  public void setInspectorsVisible(boolean vis) {
    Collection filters = getFilters();
    Iterator it = filters.iterator();
    while (it.hasNext()) {
      Filter filter = (Filter)it.next();
      JDialog inspector = filter.getInspector();
      if (inspector != null) {
      	if (!vis) { // hide and mark visible inspectors
        	filter.inspectorVisible = inspector.isVisible();
        	inspector.setVisible(false);
      	}
      	else if (!inspector.isModal()){ // show marked non-modal inspectors, then unmark them
        	inspector.setVisible(filter.inspectorVisible);
        	filter.inspectorVisible = false;
      	}
      }
    }
  }

	/**
	 * Refreshes this filter's GUI
	 */
	public void refresh() {
		Iterator it = getFilters().iterator();
		while (it.hasNext()) {
			((Filter)it.next()).refresh();
		}
	}

  /**
   * Responds to property change events. FilterStack listens for the
   * following events: all events from its filters.
   *
   * @param e the property change event
   */
  public void propertyChange(PropertyChangeEvent e) {
    support.firePropertyChange("image", null, null); //$NON-NLS-1$
  }
}
