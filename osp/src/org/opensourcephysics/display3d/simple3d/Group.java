/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */

package org.opensourcephysics.display3d.simple3d;
import org.opensourcephysics.controls.*;
import java.util.*;

/**
 * <p>Title: Group</p>
 * <p>Description: A Group is an element that is made of other elements.</p>
 * The group's position, size, visibility and transformation do affect the
 * elements in the group. The group's style doesn't, though.
 * @author Francisco Esquembre
 * @version March 2005
 * @see Style
 */
public class Group extends Element implements org.opensourcephysics.display3d.core.Group {
  // Implementation variables
  private ArrayList elementList = new ArrayList();
  private ArrayList list3D = new ArrayList();          // The list of Objects3D
  private Object3D[] minimalObjects = new Object3D[1]; // The array of Objects3D

  // ----------------------------------------------------
  // Implementation of core.Group
  // ----------------------------------------------------
  public void addElement(org.opensourcephysics.display3d.core.Element element) {
    if(!(element instanceof Element)) {
      throw new UnsupportedOperationException("Can't add element to group (incorrect implementation)");
    }
    if(!elementList.contains(element)) {
      elementList.add(element);
    }
    ((Element) element).setGroup(this);
  }

  public void addElements(java.util.Collection elements) {
    if(elements!=null) {
      Iterator it = elements.iterator();
      while(it.hasNext()) {
        Object obj = it.next();
        if(obj instanceof Element) {
          addElement((Element) obj);
        }
      }
    }
  }

  public void removeElement(org.opensourcephysics.display3d.core.Element element) {
    elementList.remove(element);
  }

  public void removeAllElements() {
    elementList.clear();
  }

  public synchronized ArrayList getElements() {
    return(ArrayList) elementList.clone();
  }

  public org.opensourcephysics.display3d.core.Element getElement(int index) {
    try { return (org.opensourcephysics.display3d.core.Element) elementList.get(index); }
    catch (IndexOutOfBoundsException exc) { return null; }
  }

  // ----------------------------------------------------
  // Abstract part of Element
  // ----------------------------------------------------
  Object3D[] getObjects3D() {
    if(!isReallyVisible()) {
      return null;
    }
    list3D.clear();
    for(Iterator it = elementList.iterator();it.hasNext();) {
      Object3D[] objects = ((Element) it.next()).getObjects3D();
      if(objects!=null) {
        for(int i = 0, n = objects.length;i<n;i++) {
          list3D.add(objects[i]);
        }
      }
    }
    setElementChanged(false);
    if(list3D.size()==0) {
      return null;
    }
    return(Object3D[]) list3D.toArray(minimalObjects);
  }

  void draw(java.awt.Graphics2D _g2, int _index) {
    System.out.println("Group draw (i): I should not be called!");
  }

  void drawQuickly(java.awt.Graphics2D _g2) {
    for(Iterator it = elementList.iterator();it.hasNext();) {
      ((Element) it.next()).drawQuickly(_g2);
    }
    setElementChanged(false);
  }

  // Overwrites its parent
  void setNeedToProject(boolean _need) {
    for(Iterator it = elementList.iterator();it.hasNext();) {
      ((Element) it.next()).setNeedToProject(_need);
    }
  }

  public void getExtrema(double[] min, double[] max) {
    double minX = Double.POSITIVE_INFINITY, maxX = Double.NEGATIVE_INFINITY;
    double minY = Double.POSITIVE_INFINITY, maxY = Double.NEGATIVE_INFINITY;
    double minZ = Double.POSITIVE_INFINITY, maxZ = Double.NEGATIVE_INFINITY;
    double[] firstPoint = new double[3], secondPoint = new double[3];
    Iterator it = getElements().iterator();
    while(it.hasNext()) {
      ((Element) it.next()).getExtrema(firstPoint, secondPoint);
      minX = Math.min(Math.min(minX, firstPoint[0]), secondPoint[0]);
      maxX = Math.max(Math.max(maxX, firstPoint[0]), secondPoint[0]);
      minY = Math.min(Math.min(minY, firstPoint[1]), secondPoint[1]);
      maxY = Math.max(Math.max(maxY, firstPoint[1]), secondPoint[1]);
      minZ = Math.min(Math.min(minZ, firstPoint[2]), secondPoint[2]);
      maxZ = Math.max(Math.max(maxZ, firstPoint[2]), secondPoint[2]);
    }
    min[0] = minX;
    max[0] = maxX;
    min[1] = minY;
    max[1] = maxY;
    min[2] = minZ;
    max[2] = maxZ;
  }

  public InteractionTarget getTargetHit(int x, int y) {
    if(!isReallyVisible()) {
      return null;
    }
    Iterator it = getElements().iterator();
    while(it.hasNext()) {
      InteractionTarget target = ((Element) it.next()).getTargetHit(x, y);
      if(target!=null) {
        return target;
      }
    }
    return null;
  }

  boolean getElementChanged() {
    for(Iterator it = elementList.iterator();it.hasNext();) {
      if(((Element) it.next()).getElementChanged()) {
        return true;
      }
    }
    return super.getElementChanged();
  }

  // ----------------------------------------------------
  // XML loader
  // ----------------------------------------------------
  public static XML.ObjectLoader getLoader() {
    return new Loader();
  }

  protected static class Loader extends org.opensourcephysics.display3d.core.Group.Loader {
    public Object createObject(XMLControl control) {
      return new Group();
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
