package edu.wisc.ssec.hydra;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.Color;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;
import ucar.visad.display.MapLines;
import ucar.visad.display.RGBDisplayable;
import ucar.visad.display.RubberBandBox;
import ucar.visad.display.XYDisplay;
import ucar.visad.display.CrossSectionSelector;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;


import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;

import visad.*;
import visad.MouseHelper;
import java.rmi.RemoteException;


public class RGBImageDisplay implements ActionListener {

   JFrame frame;

   MapProjectionDisplayJ3D mapProjDsp;
   DisplayMaster dspMaster;

   MapProjection mapProjection;

   CrossSectionSelector transect = null;

   ImageRGBDisplayable imageDsp = null;

   String name = null;

   public RGBImageDisplay(MapProjection mapProj) throws VisADException, RemoteException {
     this(null, mapProj, null);
   }

   // TODO: description should be folded into Displayable
   public RGBImageDisplay(ImageRGBDisplayable imageDsp, MapProjection mapProj, String description)
       throws VisADException, RemoteException {

      this.mapProjection = mapProj;
      this.imageDsp = imageDsp;
      this.name = description;

      mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
      mapProjDsp.enableRubberBanding(false);
      dspMaster = mapProjDsp;

      //- make simple color range control.  FIXME: should wait on computeRange so we have to do this before next step.
      RGBCompositeControl rgbCntrl = new RGBCompositeControl(dspMaster, imageDsp);


      dspMaster.setMouseFunctions(Hydra.getMouseFunctionMap());

      dspMaster.draw(); //- once at the beginning? semms ok

      mapProjDsp.setMapProjection(mapProj);

      dspMaster.addDisplayable(imageDsp);
      Hydra.addBaseMap(mapProjDsp);


      ImageIcon imgIc = new ImageIcon(getClass().getResource("/resources/icons/house.png"));
      JButton resetButton = new JButton(imgIc);
      resetButton.setToolTipText("Reset Display scale/translation");
      resetButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
                try {
                    mapProjDsp.resetScaleTranslate();
                } catch (VisADException ve) {
                    ve.printStackTrace();
                } catch (RemoteException re) {
                    re.printStackTrace();
                }
         }
      });

      JPanel outerPanel = new JPanel();
      outerPanel.setLayout(new BorderLayout());

      JPanel southPanel = new JPanel();
      JPanel panel = new JPanel(new FlowLayout());
      panel.add(resetButton);
      panel.add(rgbCntrl.doMakeContents());
      southPanel.add(panel);
      

      outerPanel.add(dspMaster.getDisplayComponent(), BorderLayout.CENTER);
      outerPanel.add(southPanel, BorderLayout.SOUTH);

      JMenuBar menuBar = new JMenuBar();

      JMenu newMenu = new JMenu("New");
      newMenu.getPopupMenu().setLightWeightPopupEnabled(false);
      menuBar.add(newMenu);

      JMenu toolsMenu = new JMenu("Tools");
      toolsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
      menuBar.add(toolsMenu);

      JMenuItem transectMenu = new JMenuItem("Transect");
      transectMenu.setActionCommand("doTransect");
      transectMenu.addActionListener(this);
      //toolsMenu.add(transectMenu);

      JMenuItem scatterMenu = new JMenuItem("Scatter");
      scatterMenu.setActionCommand("doScatter");
      scatterMenu.addActionListener(this);
      scatterMenu.setToolTipText("first X, then Y creates scatter");
      //toolsMenu.add(scatterMenu);

      JMenu settingsMenu = new JMenu("Settings");
      settingsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
      settingsMenu.addActionListener(this);
      menuBar.add(settingsMenu);
    
      String title = (description != null) ? description : "Display";
      Hydra.createAndShowFrame(title, outerPanel, menuBar, new Dimension(500,500));
   }

   public void setMapProjection(MapProjection mapProj) throws VisADException, RemoteException {
     this.mapProjection = mapProj;
     this.mapProjDsp.setMapProjection(mapProj);
   }

   public MapProjection getMapProjection() {
     return mapProjection;
   }

   public ImageRGBDisplayable getImageDisplayable() {
     return imageDsp;
   }

   public void actionPerformed(ActionEvent e) {
   }

}
