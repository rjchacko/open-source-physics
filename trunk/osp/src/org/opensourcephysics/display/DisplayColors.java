package org.opensourcephysics.display;
import java.awt.Color;

/**
 * Defines  color pallettes used by OSP components.
 *
 * @author Wolfgang Christian
 * @version 1.0
 */
public class DisplayColors {

   static Color[] phaseColors = null;
   static Color[] lineColors = {Color.red, Color.green, Color.blue, Color.yellow.darker(), Color.cyan, Color.magenta};
   static Color[] markerColors = {Color.black, Color.blue, Color.red, Color.green, Color.darkGray, Color.lightGray};
   private DisplayColors() {}

   /**
    * Gets an array of colors.
    *
    * @return the color array
    */
   public static Color[] getPhaseToColorArray() {
      if(phaseColors==null) {
         phaseColors = new Color[256];
         for(int i = 0; i<256; i++) {
            double val = Math.abs(Math.sin(Math.PI*i/255));
            int b = (int) (255*val*val);
            val = Math.abs(Math.sin(Math.PI*i/255+Math.PI/3));
            int g = (int) (255*val*val*Math.sqrt(val));
            val = Math.abs(Math.sin(Math.PI*i/255+2*Math.PI/3));
            int r = (int) (255*val*val);
            phaseColors[i] = new Color(r, g, b);
         }
      }
      return phaseColors;
   }

   /**
    * Converts a phase angle in the range [-Pi,Pi] to a color.
    *
    * @param phi phase angle
    * @return the color
    */
   public static Color phaseToColor(double phi) {
      int index = (int) (127.5*(1+phi/Math.PI));
      index = index%255;
      if(phaseColors==null) {
         return getPhaseToColorArray()[index];
      } else {
         return phaseColors[index];
      }
   }

   /**
    * Gets a random color.
    *
    * @return random color
    */
   public static Color randomColor() {
      return new Color((int) (Math.random()*255), (int) (Math.random()*255), (int) (Math.random()*255));
   }

   /**
    * Gets a line color that matches the index.
    * @param index int
    * @return Color
    */
   static public Color getLineColor(int index) {
      if(index<lineColors.length-1) {
         return lineColors[index]; // use specified colors
      } else {
         return randomColor();
      }
   }

   /**
    * Gets a marker color that matches the index.
    * @param index int
    * @return Color
    */
   static public Color getMarkerColor(int index) {
      if(index<markerColors.length-1) {
         return markerColors[index]; // use specified colors
      } else {
         return randomColor();
      }
   }
}
