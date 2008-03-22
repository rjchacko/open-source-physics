/*
 * Open Source Physics software is free software as described near the bottom of this code file.
 *
 * For additional information and documentation on Open Source Physics please see:
 * <http://www.opensourcephysics.org/>
 */



package org.opensourcephysics.ejs.control;

//~--- non-JDK imports --------------------------------------------------------

import org.opensourcephysics.controls.Calculation;
import org.opensourcephysics.display.*;

//~--- JDK imports ------------------------------------------------------------

import java.beans.PropertyChangeListener;

import javax.swing.JPanel;
import javax.swing.border.EtchedBorder;

/**
 * An EJS control object for Calculations.
 * @author Wolfgang Christian
 * @version 1.0
 */
public class EjsCalculationControl extends EjsControlFrame {
    protected JPanel       controlPanel;
    protected DrawingPanel drawingPanel;
    protected Calculation  model;

    public EjsCalculationControl(Calculation model, DrawingFrame frame, String[] args) {
        super(model, "name=controlFrame;title=QM Superposition;location=400,0;layout=border;exit=true; visible=false");
        this.model = model;
        addTarget("control", this);

        if (frame != null) {
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
        //add("Button", "parent=buttonPanel; text=Calculate; action=control.calculate()");
        //add("Button", "parent=buttonPanel; text=Reset; action=control.resetCalculation()");
        add("Button", "parent=buttonPanel;tooltip=Calculate;image=/org/opensourcephysics/resources/ejs/images/play.gif; action=control.calculate();name=calculateButton");
        add("Button", "parent=buttonPanel; tooltip=Reset calculation;image=/org/opensourcephysics/resources/ejs/images/reset.gif; action=control.resetCalculation()");
        controlPanel = ((JPanel) getElement("controlPanel").getComponent());
        controlPanel.setBorder(new EtchedBorder());
        customize();
        model.setControl(this);
        loadXML(args);
        model.resetCalculation();

        java.awt.Container cont = (java.awt.Container) getElement("controlFrame").getComponent();

        if (!org.opensourcephysics.display.OSPRuntime.appletMode) {
            cont.setVisible(true);
        }

        if (model instanceof PropertyChangeListener) {
            addPropertyChangeListener((PropertyChangeListener) model);
        }

        getMainFrame().pack();
        getMainFrame().doLayout();
        GUIUtils.showDrawingAndTableFrames();
    }

    /**
     * Override this method to customize this EjsSimulationControl.
     */
    protected void customize() {}

    /**
     * Resets the calculation.
     */
    public void resetCalculation() {
    	messageArea.setText("");
    	GUIUtils.clearDrawingFrameData(true);
        model.resetCalculation();
        if(xmlDefault!=null) {
            xmlDefault.loadObject(getOSPApp());
        }
        GUIUtils.showDrawingAndTableFrames();
    }

    /**
     * Does the calculation.
     */
    public void calculate() {
        model.calculate();
        GUIUtils.showDrawingAndTableFrames();
    }
}


//~ Formatted by Jindent --- http://www.jindent.com
