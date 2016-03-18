package edu.wisc.ssec.hydra;

import javax.swing.JFrame;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import java.awt.Component;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.geom.Rectangle2D;

import java.io.File;

import visad.VisADException;
import visad.RealTupleType;
import visad.georef.MapProjection;
import visad.georef.EarthLocation;
import visad.georef.TrivialMapProjection;
import java.rmi.RemoteException;

import ucar.unidata.view.geoloc.MapProjectionDisplay;



public class DisplayCapture {

    JFileChooser fc = new JFileChooser();
    MapProjectionDisplay mapProjDsp;
    double[] lastProjMatrix;
    MapProjection lastMapProj;
    ImageDisplay imageDisplay;
    JDialog dialog;

    public DisplayCapture(final JFrame parent, final ImageDisplay imageDisplay) throws VisADException, RemoteException {
       this.imageDisplay = imageDisplay;
       this.mapProjDsp = (MapProjectionDisplay) imageDisplay.getDisplayMaster();
       this.imageDisplay.setProbeVisible(false);

       dialog = new JDialog(parent, "KML capture");
       dialog.setLocationRelativeTo(parent);
       dialog.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                 resetAfterKMLcaptureDone();
            }
          }
       );

       lastMapProj = imageDisplay.getMapProjection();
       imageDisplay.setDisplayLinking(false);
       lastProjMatrix = imageDisplay.getProjectionMatrix();

       Rectangle2D bounds = mapProjDsp.getLatLonBox();
       mapProjDsp.setMapProjection(new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple, bounds));

       JPanel optPanel = new JPanel(new FlowLayout());
       JCheckBox boundaryToggle = new JCheckBox("Map Boundaries", true);
       boundaryToggle.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
               if (e.getStateChange() == ItemEvent.DESELECTED) {
                  imageDisplay.toggleMapBoundaries(false);
               } else {
                  imageDisplay.toggleMapBoundaries(true);
               }
            }
          }
       );
       optPanel.add(boundaryToggle);

       JPanel actPanel = new JPanel(new FlowLayout());

       final JDialog fcParent = dialog;
       JButton saveButton = new JButton("Save");
       saveButton.setActionCommand("Save");
       saveButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             if (e.getActionCommand().equals("Save")) {
                int retVal = fc.showSaveDialog(fcParent);
                if (retVal == JFileChooser.APPROVE_OPTION) {
                    try {
                        saveKML(fc.getSelectedFile());
                    } catch (VisADException exc) {
                    } catch (RemoteException exc) {
                    }
                }
             }
          }
       });

       JButton closeButton = new JButton("Close");
       closeButton.setActionCommand("Close");
       closeButton.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             if (e.getActionCommand().equals("Close")) {
                    resetAfterKMLcaptureDone();
             }
          }
       });

       actPanel.add(closeButton);
       actPanel.add(saveButton);

       JPanel panel = new JPanel(new GridLayout(2,1));
       panel.add(optPanel);
       panel.add(actPanel);

       dialog.setContentPane(panel);
       dialog.getRootPane().setDefaultButton(saveButton);
       dialog.validate();
       dialog.setVisible(true);
       dialog.setSize(dialog.getPreferredSize());
    }

    private void resetAfterKMLcaptureDone() {
        imageDisplay.setMapProjection(lastMapProj);
        imageDisplay.setProjectionMatrix(lastProjMatrix);
        imageDisplay.setDisplayLinking(true);
        imageDisplay.setProbeVisible(true);
        imageDisplay.toggleMapBoundaries(true);
        dialog.setVisible(false);
    }

    void saveKML(File file) throws VisADException, RemoteException {
                String path = file.getAbsolutePath();
                String kmlPath = path+".kml";
                if (!path.endsWith(".jpeg")) {
                   path = path+".jpeg";
                }
                final String imagePath = path;
                java.io.File tmpF = new java.io.File(imagePath);
                String imageName = tmpF.getName();

                Rectangle2D screenBounds = mapProjDsp.getScreenBounds();
                EarthLocation ul = mapProjDsp.screenToEarthLocation(0,0);
                EarthLocation lr = mapProjDsp.screenToEarthLocation((int)screenBounds.getWidth(),(int)screenBounds.getHeight());
                double north = ul.getLatLonPoint().getLatitude().getValue();
                double west = ul.getLatLonPoint().getLongitude().getValue();
                double south = lr.getLatLonPoint().getLatitude().getValue();
                double east = lr.getLatLonPoint().getLongitude().getValue();
                Hydra.makeKML(south, north, west, east, kmlPath, imageName);

                captureDisplay(imagePath);
    }


    void captureDisplay(final String imagePath)  {
                final MapProjectionDisplay dsp = mapProjDsp;
                    Runnable captureImage = new Runnable() {
                      public void run() {
                          dsp.saveCurrentDisplay(new java.io.File(imagePath), true, true);
                      }
                 };
                 Thread t = new Thread(captureImage);
                 t.start();
    }

    public void captureJPEG(JFrame frame) {
            int retVal = fc.showSaveDialog(frame);
            if (retVal == JFileChooser.APPROVE_OPTION) {
               java.io.File file = fc.getSelectedFile();
               String path = file.getAbsolutePath();
               if (!path.endsWith(".jpeg")) {
                  path = path+".jpeg";
               }
               String imagePath = path;

               captureDisplay(imagePath);
            }
    }

    public static void captureJPEG(JFrame frame, final MapProjectionDisplay dsp) {
          JFileChooser fc = new JFileChooser();
          int retVal = fc.showSaveDialog(frame);
          if (retVal == JFileChooser.APPROVE_OPTION) {
              java.io.File file = fc.getSelectedFile();
              String path = file.getAbsolutePath();
              if (!path.endsWith(".jpeg")) {
                  path = path+".jpeg";
              }
              String imagePath = path;

              captureDisplay(imagePath, dsp);
          }
    }

    public static void captureDisplay(final String imagePath, final MapProjectionDisplay dsp) {
                    Runnable captureImage = new Runnable() {
                      public void run() {
                          dsp.saveCurrentDisplay(new java.io.File(imagePath), true, true);
                      }
                 };
                 Thread t = new Thread(captureImage);
                 t.start();
    }
}
