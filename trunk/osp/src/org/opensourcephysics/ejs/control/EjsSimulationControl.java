
/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */
package org.opensourcephysics.ejs.control;
import java.beans.PropertyChangeListener;
import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;
import org.opensourcephysics.controls.*;
import org.opensourcephysics.display.*;
import org.opensourcephysics.ejs.control.EjsControlFrame;

public class EjsSimulationControl extends EjsControlFrame {
  protected Simulation model;
  protected DrawingPanel drawingPanel;
  protected JPanel controlPanel;
  public EjsSimulationControl(Simulation model, DrawingFrame frame, String[] args) {
    super(model, "name=controlFrame;title=OSP Simulation;location=400,0;layout=border;exit=true; visible=false");
    this.model = model;
    addTarget("control", this);
    addTarget("model", model);
    if(frame!=null) {
      getMainFrame().setAnimated(frame.isAnimated());
      getMainFrame().setAutoclear(frame.isAutoclear());
      getMainFrame().setBackground(frame.getBackground());
      getMainFrame().setTitle(frame.getTitle());
      drawingPanel = frame.getDrawingPanel();
      addObject(drawingPanel, "Panel", "name=drawingPanel; parent=controlFrame; position=center");
      frame.setDrawingPanel(null);
      frame.dispose();
    }
    add("Panel", "name=controlPanel; parent=controlFrame; layout=border; position=south");
    add("Panel", "name=buttonPanel;position=west;parent=controlPanel;layout=flow");
    //add("Button", "parent=buttonPanel; text=Start; action=control.runSimulation();name=runButton");
    //add("Button", "parent=buttonPanel; text=Step; action=control.stepAnimation()");
    //add("Button", "parent=buttonPanel; text=Reset; action=control.resetAnimation()");
    add("Button", "parent=buttonPanel;tooltip=Start and stop simulation;image=/org/opensourcephysics/resources/ejs/images/play.gif; action=control.runSimulation();name=runButton");
    add("Button", "parent=buttonPanel;tooltip=Step simulation;image=/org/opensourcephysics/resources/ejs/images/step.gif; action=control.stepSimulation()");
    add("Button", "parent=buttonPanel; tooltip=Reset simulation;image=/org/opensourcephysics/resources/ejs/images/reset.gif; action=control.resetSimulation()");
    controlPanel = ((JPanel) getElement("controlPanel").getComponent());
    controlPanel.setBorder(new EtchedBorder());
    customize();
    model.setControl(this);
    loadXML(args);
    model.initializeAnimation();
    java.awt.Container cont = (java.awt.Container) getElement("controlFrame").getComponent();
    if(!org.opensourcephysics.display.OSPRuntime.appletMode) {
      cont.setVisible(true);
    }
    if(model instanceof PropertyChangeListener) {
      addPropertyChangeListener((PropertyChangeListener) model);
    }
    getMainFrame().pack(); // make sure everything is showing
    getMainFrame().doLayout();
    GUIUtils.showDrawingAndTableFrames();
  }

  /**
   * Override this method to customize the EjsSimulationControl.
   */
  protected void customize() {}

  /**
   * Renders (draws) the panel immediately.
   *
   * Unlike repaint, the render method is draws the panel within the calling method's thread.
   * This method is called automatically if the frame is animated.
   */
  public void render() {
    if(drawingPanel!=null) {
      drawingPanel.render(); // simulations should render their panels at every time step
    }
  }

  /**
   * Resets the model and switches the text on the run button.
   */
  public void resetSimulation() {
	model.stopAnimation();
	messageArea.setText("");
	GUIUtils.clearDrawingFrameData(true);
    model.resetAnimation();
    if(xmlDefault!=null) {
        xmlDefault.loadObject(getOSPApp());
    }
    model.initializeAnimation();
    //getControl("runButton").setProperty("text", "Start");
    getControl("runButton").setProperty("image", "/org/opensourcephysics/resources/ejs/images/play.gif");
    GUIUtils.showDrawingAndTableFrames();
 
  }

  public void stepSimulation() {
    model.stopAnimation();
    //getControl("runButton").setProperty("text", "Start");
    getControl("runButton").setProperty("image", "/org/opensourcephysics/resources/ejs/images/play.gif");
    model.stepAnimation();
    GUIUtils.repaintAnimatedFrames();
  }

  /**
   * Runs the Simulation switches the text on the run button
   */
  public void runSimulation() {
    if(model.isRunning()) {
      model.stopSimulation();
      //getControl("runButton").setProperty("text", "Start");
      getControl("runButton").setProperty("image", "/org/opensourcephysics/resources/ejs/images/play.gif");
    } else {
      //getControl("runButton").setProperty("text", "Stop");
      getControl("runButton").setProperty("image", "/org/opensourcephysics/resources/ejs/images/pause.gif");
      model.startSimulation();
    }
  }

}

