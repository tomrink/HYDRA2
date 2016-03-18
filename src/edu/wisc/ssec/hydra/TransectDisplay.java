package edu.wisc.ssec.hydra;


import visad.*;

import visad.util.Util;
import visad.util.HersheyFont;
import java.rmi.RemoteException;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Component;
import java.awt.Color;
import java.awt.event.WindowEvent;

import javax.swing.JFrame;

import java.util.ArrayList;

    public class TransectDisplay extends HydraDisplay {

      DataReference transectDataRef = null;
      RealType rangeType = null;

      LocalDisplay display = null;

      ScalarMap ymap = null;

      ArrayList<ScalarMap> yAxisMaps = new ArrayList<ScalarMap>();
      
      DragLine drgline;
      
      JFrame frame;
      
      double markerDist = Double.NaN;
    
      public TransectDisplay(Transect transect, Color color, Point loc) throws VisADException, RemoteException {

        transectDataRef = transect.getTransectDataRef();
        FlatField data = (FlatField) transectDataRef.getData();
        FunctionType fncType = (FunctionType) data.getType();

        RealType domainType = Transect.DistAlongTransect;
        rangeType = (RealType) fncType.getRange();

        display = new visad.java3d.DisplayImplJ3D("2D disp", new visad.java3d.TwoDDisplayRendererJ3D());
        ((DisplayImpl)display).disableAction();
        display.getDisplayRenderer().setBackgroundColor(new Color(0.92f, 0.92f, 0.92f)); // off-white
        display.getDisplayRenderer().setForegroundColor(Color.black);
        ScalarMap xMapA = new ScalarMap(RealType.XAxis, Display.XAxis);
        ScalarMap yMapA = new ScalarMap(RealType.YAxis, Display.YAxis);
        xMapA.setRange(-2.5,2.5);
        yMapA.setRange(-0.75,0.75);
        xMapA.setScaleEnable(false);
        yMapA.setScaleEnable(false);
        display.addMap(xMapA);
        display.addMap(yMapA);

        ProjectionControl pCntrl = display.getProjectionControl();
        double[] proj = pCntrl.getMatrix();
        proj[0] = 0.286;
        proj[5] = 0.286;
        proj[10] = 0.286;
        pCntrl.setMatrix(proj);
        
        ScalarMap xmap = new ScalarMap(domainType, Display.XAxis);

        /** Use addTransect in the initialization? */

        ymap = new ScalarMap(rangeType, Display.YAxis);
        yAxisMaps.add(ymap);

        ScalarMap txtMap = new ScalarMap(TextType.Generic, Display.Text);

        pCntrl = display.getProjectionControl();
        pCntrl.setAspectCartesian(new double[] {2.50, 0.75, 1.0});
        ((DisplayImpl)display).setAlwaysAutoScale(true);
        //display.getGraphicsModeControl().setLineWidth(1.5f);
        //((visad.java3d.GraphicsModeControlJ3D)display.getGraphicsModeControl()).setSceneAntialiasingEnable(true);
        display.addMap(xmap);
        display.addMap(ymap);
        display.addMap(txtMap);

        HersheyFont font = new HersheyFont("timesr");

        AxisScale xAxis = xmap.getAxisScale();
        xAxis.setFont(font);
        xAxis.setColor(Color.black);
        xAxis.setSnapToBox(true);
        xAxis.setLabelSize(2*xAxis.getLabelSize());
        xmap.setScaleEnable(true);

        AxisScale yAxis = ymap.getAxisScale();
        yAxis.setFont(font);
        yAxis.setColor(Color.black);
        yAxis.setSnapToBox(true);
        yAxis.setLabelSize(2*yAxis.getLabelSize());
        ymap.setScaleEnable(true);
        
        display.getGraphicsModeControl().setScaleEnable(true);

        ConstantMap lineWidth = new ConstantMap(1.5, Display.LineWidth);
        ConstantMap[] constantMaps = new ConstantMap[] {lineWidth};

        if (color != null) {
           color = getGraphColor(color);
           ConstantMap[] clrs = Util.getColorMaps(color);
           constantMaps = Hydra.makeConstantMapArray(clrs, lineWidth);
           display.addReference(transectDataRef, constantMaps);
        } else {
           display.addReference(transectDataRef);
        }
        ((DisplayImpl)display).enableAction();
        
        /* not yet
        //- text readout for index selector
        final DataReference txtRef = new DataReferenceImpl("text");
        //display.addReference(txtRef, new ConstantMap[] {new ConstantMap(0.9, Display.YAxis)});
        float[][] vals = data.getFloats(false);
        float[] minmax = Hydra.minmax(vals[0]);
        try {
          drgline = new DragLine(display, (Gridded1DSet)data.getDomainSet(), domainType, rangeType, Hydra.makeColorMap(Color.ORANGE), minmax, 0f, xmap);
          drgline.addListener(transect);
        } catch (Exception e) {
           e.printStackTrace();
        }
        */        

        frame = Hydra.createAndShowFrameFromEDT("Transect Display", doMakeComponent(), new Dimension(400,160), loc);
        frame.toFront();
        frame.addWindowListener(this);
     }

     public void addTransect(Transect transect, Color color) throws VisADException, RemoteException {
       FlatField dataTransect = (FlatField) transect.getTransectDataRef().getData();
       RealType rangeType = (RealType) ((FunctionType)dataTransect.getType()).getRange();

       addScalarMapForRangeType(rangeType);

       color = getGraphColor(color);
       ConstantMap lineWidth = new ConstantMap(1.5, Display.LineWidth);
       ConstantMap[] constantMaps = Hydra.makeConstantMapArray(Util.getColorMaps(color), lineWidth);
       
       if (drgline != null) {
          drgline.addListener(transect);
       }

       this.display.addReference(transect.getTransectDataRef(), constantMaps);
     }

     public void transectRangeChanged(RealType rangeType) throws VisADException, RemoteException {
         addScalarMapForRangeType(rangeType);
     }

     private void addScalarMapForRangeType(RealType rangeType) throws VisADException, RemoteException {
         boolean hasMap = false;
         for (int k=0; k<yAxisMaps.size(); k++) {
            ScalarMap map = yAxisMaps.get(k);
            if (rangeType.equals(map.getScalar())) {
               hasMap = true;
               break;
            }
         }

         if (!hasMap) {
            ScalarMap map = new ScalarMap(rangeType, Display.YAxis);

            AxisScale yAxis = map.getAxisScale();
            yAxis.setColor(Color.black);
            yAxis.setLabelSize(24);
            yAxis.setSide(AxisScale.SECONDARY);
            if (yAxisMaps.size() < 2) {
            yAxis.setSnapToBox(true);
            }

            this.display.addMap(map);
            yAxisMaps.add(map);
         }
     }

     public void removeTransect(Transect transect) throws VisADException, RemoteException {
       this.display.removeReference(transect.getTransectDataRef());
       //- rescale the display to account for removed data
       ((DisplayImpl)this.display).reDisplayAll();
     }

     private Color getGraphColor(Color color) {
        if (color.equals(Color.green)) {
          color = new Color(34, 190, 24);
        }
        return color;
     }

     public Component doMakeComponent() {
       return display.getComponent();
     }

     public void windowClosing(WindowEvent e) {
        Transect.removeAll();
     }
     
     public void setMarkerDist(double dist, Gridded1DSet dset) throws VisADException, RemoteException {
         if (markerDist != dist && drgline != null) {
            drgline.updateSelector(dist, dset);
            markerDist = dist;
         }
     }
     
   }
