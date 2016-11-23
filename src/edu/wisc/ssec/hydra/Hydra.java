package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.hydra.data.DataSourceFactory;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.JMenuBar;

import java.awt.Component;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;

import java.io.File;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.net.URL;
import java.text.DecimalFormat;

import java.lang.Float;


import ucar.unidata.util.Range;
import ucar.unidata.util.ColorTable;
import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;
//import ucar.unidata.ui.colortable.ColorTableDefaults;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.hydra.data.MultiSpectralDataSource;
import edu.wisc.ssec.adapter.LongitudeLatitudeCoordinateSystem;
import edu.wisc.ssec.adapter.ReprojectSwath;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.MapLines;

import ucar.unidata.view.geoloc.MapProjectionDisplay;
import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;


import visad.data.mcidas.BaseMapAdapter;
import visad.georef.MapProjection;
import visad.georef.TrivialMapProjection;
import visad.georef.LatLonPoint;
import visad.georef.EarthLocationTuple;


import visad.*;
import java.rmi.RemoteException;

import org.jdom2.input.SAXBuilder;
import org.jdom2.output.XMLOutputter;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

import java.util.StringTokenizer;

import java.io.BufferedReader;
import java.io.InputStreamReader;



public class Hydra {

   Selection selection = null;

   DataSource dataSource = null;

   public static ColorTable grayTable = new ColorTable("gray", "gray", grayTable(256, false));

   public static ColorTable invGrayTable = new ColorTable("invGray", "invGray", grayTable(256, true));

   public static ColorTable rainbow = null;

   public static ColorTable invRainbow = null;
   
   public static ColorTable heat = null;
   
   public static RealType reflectance = RealType.getRealType("Reflectance");

   public static RealType brightnessTemp = RealType.getRealType("BrightnessTemp");

   public static RealType radiance = RealType.getRealType("Radiance");

   float nadirResolution = 1000.0f; // default

   float wavenumber = 0;

   int channelIndex = 0;

   boolean doReproject = true;

   String dateTimeStamp = null;
   String sourceDescription = null;
   String fldName = null;

   JFrame selectFrame = null;

   JComponent actionComponent = null;

   boolean selectorIconified = false;

   static int numImageDisplays = 0;

   JComponent selectComponent = null;

   DataBrowser dataBrowser = null;

   private static boolean regionMatch = true;

   private static int reprojectMode = 0;
   
   private static boolean doParallel = true;

   private static MapProjection sharedMapProj = null;

   public static HashMap<HydraRGBDisplayable, FlatField> displayableToImage = 
         new HashMap<HydraRGBDisplayable, FlatField>(); 

   public static GSHHG GSHHGadapter = null;
   
   public static WorldDataBank WDBIIadapter = null;

   static int numDataSourcesOpened = 0;

   public boolean multiDisplay = false;
   public boolean singleChannelDisplay = true;
   public boolean multiChannelDisplay = false;
   public boolean atmRetrievalDisplay = false;
   
   private static int uniqueID = 0;
   
   private int dataSourceId;
   
   private DataSourceFactory dataSourceFactory = new DataSourceFactory();
   
   public static HashMap<Integer, DataSource> dataSourceMap = new HashMap<>();

   public Hydra() {
   }

   public Hydra(DataBrowser dataBrowser) {
      this.dataBrowser = dataBrowser;
   }

   public void dataSourceSelected(File dir, Class ds) {
     try {
        dataSource = dataSourceFactory.createDataSource(dir, ds);
     } 
     catch (Exception e) {
        e.printStackTrace();
     }

     sourceDescription = dataSource.getDescription();
     dateTimeStamp = dataSource.getDateTimeStamp();
     numDataSourcesOpened++;
     
     dataSourceId = numDataSourcesOpened;
     dataSourceMap.put(dataSourceId, dataSource);

     selection = new BasicSelection(dataSource, this, numDataSourcesOpened);
   }

   public DataBrowser getDataBrowser() {
      return dataBrowser;
   }

   public void dataSourceSelected(File[] files) {
      String filename = files[0].getName();
      
      try {
         dataSource = dataSourceFactory.createDataSource(files);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
      
      dateTimeStamp = dataSource.getDateTimeStamp();
      sourceDescription = dataSource.getDescription();
      numDataSourcesOpened++;
      
      dataSourceId = numDataSourcesOpened;
      dataSourceMap.put(dataSourceId, dataSource);

      if (dataSource.isSounder()) {
         multiChannelDisplay = true;
         multiDisplay = true;
      }
      else if (dataSource.isAtmRetrieval()) {
         atmRetrievalDisplay = true;
         multiDisplay = true;
      }
      else if (dataSource.isImager()) {
         multiChannelDisplay = false;
         multiDisplay = false;
      }

      selection = new BasicSelection(dataSource, this, numDataSourcesOpened);
   }
   
   public int getDataSourceId() {
      return dataSourceId;
   }
   
   public DataSource getDataSource() {
      return dataSource;
   }
   
   public static DataSource getDataSource(int dataSourceId) {
      return dataSourceMap.get(dataSourceId);
   }
   
   public static void removeDataSource(DataSource dataSource) {
      DataSourceFactory.removeDataSource(dataSource);
   }

   public String toString() {
     return numDataSourcesOpened+": "+sourceDescription+" "+dateTimeStamp;
   }

   public void setCursorToWait() {
      selectFrame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
      selectComponent.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
   }

   public void setCursorToDefault() {
      selectFrame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      selectComponent.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   }

   public String getSourceDescription() {
      return sourceDescription;
   }
   
   public Selection getSelection() {
      return selection;
   }

//--------------------- DISPLAY -------------------------------------

   public boolean createImageDisplay() {
      return createImageDisplay(0);
   }

   public boolean createImageDisplay(int mode) {
      return createImageDisplay(mode, 0);
   }

   public boolean createImageDisplay(int mode, int windowNumber) {
     DataChoice choice;
     
     if (multiChannelDisplay) {
        DataSelection dataSelection = new MultiDimensionSubset();
        selection.applyToDataSelection(dataSelection);
        choice = selection.getSelectedDataChoice();
        try {
           MultiChannelViewer mcv = new MultiChannelViewer(this, choice, sourceDescription, dateTimeStamp, windowNumber, numDataSourcesOpened);
           selectFrame = mcv.getFrame();
        } catch (Exception e) {
           e.printStackTrace();
        }
        return true;
     }
     else if (atmRetrievalDisplay) {
        DataSelection dataSelection = new MultiDimensionSubset();
        selection.applyToDataSelection(dataSelection);
        choice = selection.getSelectedDataChoice();
        try {
           AtmSoundingViewer asv = new AtmSoundingViewer(choice, sourceDescription, dateTimeStamp, windowNumber, numDataSourcesOpened);
           asv.setDataChoices(dataSource.getDataChoices());
           selectFrame = asv.getFrame();
        } catch (Exception e) {
           e.printStackTrace();
        }
        return true;
     }


     boolean imageCreated = false;
     DataSelection dataSelection = new MultiDimensionSubset();
        

     selection.applyToDataSelection(dataSelection);
     choice = selection.getSelectedDataChoice();
     fldName = selection.getSelectedName();

     doReproject = dataSource.getDoReproject(choice);

     try {
        if (doReproject) {
           nadirResolution = dataSource.getNadirResolution(choice);
        }
     } 
     catch (Exception e) { 
        System.out.println("could not determine nadir resolution, using default: 1000m");
     }

     try {
        //- get the data
        FlatField image = (FlatField) dataSource.getData(choice, dataSelection);
        FlatField swathImage = image;

        if (doReproject) {
           if (dataSource.getReduceBowtie(choice)) {
              String sensorName = dataSource.getSensorName(choice);
              reduceSwathBowtie(image, sensorName);
           }
        }

        MapProjection mapProj = null;
        
        //- reproject
        if (doReproject) {
          float[][] corners = MultiSpectralData.getLonLatBoundingCorners(image.getDomainSet());
          mapProj = getSwathProjection(corners); //TODO: How do we know this is a swath?
          Linear2DSet grid = makeGrid(mapProj, corners, nadirResolution);
          int reprojectMode = getReprojectMode();
          boolean filter = true;
          if (reprojectMode == 0) {
              if (!dataSource.getDoFilter(choice)) {
                  filter = false;
              }
          }
          image = ReprojectSwath.swathToGrid(grid, image, reprojectMode, filter);
        }
        else {
          mapProj = getDataProjection(image);
        }

        if (regionMatch) {
           if (sharedMapProj == null) {
              sharedMapProj = mapProj;
           }
           else {
              mapProj = sharedMapProj;
           }
        }

        ColorTable clrTbl = dataSource.getDefaultColorTable(choice);
        Range range = dataSource.getDefaultColorRange(choice);
        
        DataSourceInfo datSrcInfo = new DataSourceInfo(sourceDescription, dateTimeStamp, dataSourceId);
        DatasetInfo dsInfo = new DatasetInfo(fldName, nadirResolution, datSrcInfo);
        
        if (mode == 0 || ImageDisplay.getTarget() == null) {
            //-- make the displayable
            HydraRGBDisplayable imageDsp = makeImageDisplayable(image, range, clrTbl, fldName);
            imageDsp.setUseFastRendering(true);
            displayableToImage.put(imageDsp, swathImage);
            ImageDisplay iDisplay = new ImageDisplay(imageDsp, mapProj, windowNumber, dsInfo);
        }
        else if (mode == 1) {
           displayableToImage.put((HydraRGBDisplayable)ImageDisplay.getTarget().getReplaceableImageDisplayable(), swathImage);           
           ImageDisplay.getTarget().updateImageData(image, clrTbl, mapProj, dsInfo);
        }
        else if (mode == 2) {
           // TODO: Need to understand why this is necessary when doing 'overlay'.  IDV or VisAD issue?
           image = makeFlatFieldWithUniqueRange(image);
           HydraRGBDisplayable imageDsp = makeImageDisplayable(image, range, clrTbl, fldName);
           imageDsp.setUseFastRendering(true);
           displayableToImage.put(imageDsp, swathImage);
           if (dataSource.getOverlayAsMask(choice)) {
              imageDsp.addConstantMap(new ConstantMap(1.0, Display.RenderOrderPriority));
              ImageDisplay.getTarget().addOverlayImage(imageDsp, dsInfo, true);
           }
           else {
              ImageDisplay.getTarget().addOverlayImage(imageDsp, dsInfo);             
           }
        }

        imageCreated = true;
     }
     catch (VisADException e) {
        e.printStackTrace();
     }
     catch (RemoteException e) {
        e.printStackTrace();
     }
     catch (Exception e) { 
        e.printStackTrace();
     }

     return imageCreated;
   }

   public static int[][][] getMouseFunctionMap() {

      boolean isMac = false;
      String os = java.lang.System.getProperty("os.name").toLowerCase();
      if (os.indexOf("mac") >= 0) isMac = true;
      
      int[][][] map = DisplayMaster.defaultMouseFunctions;
      int[][][] myMap = new int[3][2][2];
      myMap[0][0][0] = map[0][0][0];
      myMap[0][0][1] = map[0][0][1];
      myMap[0][1][0] = map[0][1][0];
      myMap[0][1][1] = map[0][1][1];
      myMap[1][0][0] = map[1][0][0];
      myMap[1][0][1] = map[1][0][1];
      myMap[1][1][0] = map[1][1][0];
      myMap[1][1][1] = map[1][1][1];
      myMap[2][0][0] = map[2][0][0];
      myMap[2][0][1] = map[2][0][1];
      myMap[2][1][0] = map[2][1][0];
      myMap[2][1][1] = map[2][1][1];

      if (isMac) {
        myMap[0][0][1] = MouseHelper.ZOOM;
      }

      return myMap;
   }

    public static float[] minmax(FlatField ffield, EarthLocationTuple[] earthLocs)
             throws VisADException, RemoteException {
      int[] indexes = new int[2];
      float[] minmax = Hydra.minmax(ffield.getFloats(false)[0], indexes);
      Gridded2DSet domSet = (Gridded2DSet)ffield.getDomainSet();
      float[][] grdVal = domSet.indexToValue(new int[] {indexes[0]});
      CoordinateSystem cs = domSet.getCoordinateSystem();
      float[][] lonlat = cs.toReference(grdVal);
      EarthLocationTuple lla0 = new EarthLocationTuple(lonlat[1][0], lonlat[0][0], 0.0);

      grdVal = domSet.indexToValue(new int[] {indexes[1]});
      lonlat = cs.toReference(grdVal);
      EarthLocationTuple lla1 = new EarthLocationTuple(lonlat[1][0], lonlat[0][0], 0.0);

      if (earthLocs != null) {
         earthLocs[0] = lla0;
         earthLocs[1] = lla1;
      }
      
      return minmax;
    }

    public static float[] minmax(float[] values, int length, int[] indexes) {
      float min =  Float.MAX_VALUE;
      float max = -Float.MAX_VALUE;
      int minIdx = 0;
      int maxIdx = 0;
      for (int k = 0; k < length; k++) {
        float val = values[k];
        if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
          if (val < min) {
             min = val;
             minIdx = k;
          }
          if (val > max) {
             max = val;
             maxIdx = k;
          }
        }
      }
      if (indexes != null) {
         indexes[0] = minIdx;
         indexes[1] = maxIdx;
      }
      return new float[] {min, max};
    }

    public static float[] minmax(float[] values, int length) {
       return minmax(values, length, null);
    }

    public static float[] minmax(float[] values, int[] indexes) {
       return minmax(values, values.length, indexes);
    }

    public static float[] minmax(float[] values) {
       return minmax(values, values.length);
    }

    public static double[] minmax(double[] values, int length, int[] indexes) {
      double min =  Double.MAX_VALUE;
      double max = -Double.MAX_VALUE;
      int minIdx = 0;
      int maxIdx = 0;
      for (int k = 0; k < length; k++) {
        double val = values[k];
        if ((val == val) && (val < Float.POSITIVE_INFINITY) && (val > Float.NEGATIVE_INFINITY)) {
          if (val < min) {
             min = val;
             minIdx = k;
          }
          if (val > max) {
             max = val;
             maxIdx = k;
          }
        }
      }
      if (indexes != null) {
         indexes[0] = minIdx;
         indexes[1] = maxIdx;
      }
      return new double[] {min, max};
    }

    public static double[] minmax(double[] values, int length) {
       return minmax(values, length, null);
    }

    public static double[] minmax(double[] values) {
       return minmax(values, values.length);
    }
    
    public static FieldImpl infiniteToNaN(FieldImpl fltFld) throws VisADException, RemoteException {
       float[][] fvals = fltFld.getFloats(false);
       for (int t=0; t<fvals.length; t++) {
          for (int i=0; i<fvals[0].length; i++) {
             float flt = fvals[0][i];
             if  (Float.isInfinite(flt)) {
                fvals[0][i] = Float.NaN;
             }
          }  
       }
       return fltFld;
    }

    public static FlatField cloneButRangeType(RealType newRange, FlatField ffield, boolean copy) throws VisADException, RemoteException {
        FunctionType ftype = (FunctionType) ffield.getType();
        Set domainSet = ffield.getDomainSet();
        ftype = new FunctionType(ftype.getDomain(), newRange);
        float[][] rangeValues = ffield.getFloats(false);

        ffield = new FlatField(ftype, domainSet);
        ffield.setSamples(rangeValues, copy);
        return ffield;
    }

    public static RealType makeRealType(String name, String bandName) {
         String newName = null;
         bandName = bandName.replace('.', ',');

         if (name.contains("Reflectance")) {
             newName = "Reflectance_"+bandName;
         }
         else if  (name.contains("BrightnessTemperature") || name.contains("BrightnessTemp")) {
             newName = "BrightnessTemp_"+bandName;
         }
         else if (name.contains("Radiance")) {
             newName = "Radiance_"+bandName;
         }
         else {
             return null;
         }

         return RealType.getRealType(newName);
    }

    public static FlatField appendToRangeType(FlatField image, String wavenum) {
        try {
           FunctionType ftype = (FunctionType)image.getType();
           String name = ((RealType)ftype.getRange()).getName();
           RealType rtype = Hydra.makeRealType(name, wavenum);
           if (rtype != null) {
              image = Hydra.cloneButRangeType(rtype, image, false);
           }
        }
        catch (Exception e) {
          e.printStackTrace();
        }

        return image;
    }



    public static DecimalFormat getDecimalFormat(double number) {
        double OofM = java.lang.Math.log10(Math.abs(number));
        DecimalFormat numFmt;
        if (OofM <= -4 || OofM >= 4) {
          numFmt = new DecimalFormat("0.00E00");
        }
        else {
          numFmt = new DecimalFormat();
          numFmt.setMaximumFractionDigits(3);
        }

        return numFmt;

    }

    public static void reduceSwathBowtie(FlatField image, String sensorName) throws VisADException, RemoteException {
        if (sensorName == null) {
           return;
        }
        Linear2DSet domSet = (Linear2DSet) image.getDomainSet();
        float start = (float) domSet.getY().getFirst();
        int fovStart = (int) domSet.getX().getFirst();
        float[][] newRngVals = image.getFloats(true);
        int[] lens = domSet.getLengths();
        int XTrkLen = lens[0];
        int TrkLen = lens[1];

        int numDetectors = 0;

        if (sensorName.equals("MODIS_1KM")) {
           numDetectors = 10;
        } else if (sensorName.equals("MODIS_HKM")) {
           numDetectors = 20;
        } else if (sensorName.equals("MODIS_QKM")) {
           numDetectors = 40;
        } else if (sensorName.equals("MERSI_QKM")) {
           numDetectors = 40;
        } else if (sensorName.equals("MERSI_1KM")) {
           numDetectors = 10;
        } else {
           return;
        }

        switch (sensorName) {
           case "MODIS_1KM":
              for (int j=0; j<TrkLen; j++) {
                 int detIdx = (j+((int)start)) % numDetectors;
                 for (int i=0; i<XTrkLen; i++) {
                    int idx = j*XTrkLen + i;
                    int fov = fovStart + i;
                    if ((fov >= 0 && fov < 250) || (fov < 1354 && fov >= 1104)) {
                       if ((detIdx == 9) || (detIdx == 0)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 250 && fov < 340) || (fov >= 1014 && fov <= 1104)) {
                       if ((detIdx == 9)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                 }
              }
           break;

           case "MODIS_HKM":
              for (int j=0; j<TrkLen; j++) {
                 int detIdx = (j+((int)start)) % numDetectors;
                 for (int i=0; i<XTrkLen; i++) {
                    int idx = j*XTrkLen + i;
                    int fov = fovStart + i;

                    if ((fov >= 0 && fov < 500) || (fov < 2708 && fov >= 2208)) {
                       if ((detIdx >= 0 && detIdx < 2) || (detIdx >= 18)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 500 && fov < 800) || (fov >= 1908 && fov <= 2208)) {
                       if ((detIdx == 0) || (detIdx == 19)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                 }
              }
           break;

           case "MODIS_QKM":
              for (int j=0; j<TrkLen; j++) {
                 int detIdx = (j+((int)start)) % numDetectors;
                 for (int i=0; i<XTrkLen; i++) {
                    int idx = j*XTrkLen + i;
                    int fov = fovStart + i;

                    if ((fov >= 0 && fov < 1000) || (fov < 5416 && fov >= 4416)) {
                       if ((detIdx < 3) || (detIdx >= 37)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 1000 && fov < 1300) || (fov >= 4116 && fov < 4416)) {
                       if ((detIdx < 2) || (detIdx >= 38)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 1300 && fov < 1600) || (fov >= 3816 && fov < 4116)) {
                       if ((detIdx <= 1) || (detIdx >= 38)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 1600 && fov < 2600) || (fov >= 2860 && fov < 3816)) {
                       if ((detIdx == 0 ) || (detIdx == 39)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                 }
              }
           break;

          case "MERSI_QKM":
              for (int j=0; j<TrkLen; j++) {
                 int detIdx = (j+((int)start)) % numDetectors;
                 for (int i=0; i<XTrkLen; i++) {
                    int idx = j*XTrkLen + i;
                    int fov = fovStart + i;

                    if ((fov >= 0 && fov < 1024) || (fov < 8192 && fov >= 7168)) {
                       if ((detIdx < 6) || (detIdx > 33)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 1024 && fov < 1576) || (fov >= 6516 && fov <= 7168)) {
                       if ((detIdx < 5) || (detIdx > 35)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 1576 && fov < 2176) || (fov >= 6016 && fov <= 6516)) {
                       if ((detIdx < 2) || (detIdx > 37)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 2176 && fov < 3176) || (fov >= 5016 && fov <= 6016)) {
                       if ((detIdx < 1) || (detIdx > 38)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                    if ((fov >= 3176 && fov < 3600) || (fov >= 4416 && fov <= 5016)) {
                       if ((detIdx < 1) || (detIdx >= 40)) {
                          newRngVals[0][idx] = Float.NaN;
                       }
                    }
                 }
              }
          break;
        }

        image.setSamples(newRngVals, false);
    }


    /**
     * Create the GUI and show it.  For thread safety,
     * this method should be invoked from the
     * event dispatch thread.
     */
    public static JFrame createAndShowFrame(final String title, final Component component, final JMenuBar menuBar, final Dimension size, final Point loc, final boolean exitOnClose) {

        //Schedule a job for the event dispatch thread:
        //creating and showing this application's GUI.
        final JFrame frame = new JFrame(title);
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                //Turn off metal's use of bold fonts
                UIManager.put("swing.boldMetal", Boolean.FALSE);

                //JFrame frame = new JFrame(title);

                if (menuBar != null) {
                  frame.setJMenuBar(menuBar);
                }

                if (exitOnClose) {
                  frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                }

                //Add content to the window.
                frame.getContentPane().add(component);

                if (size != null) {
                  frame.setPreferredSize(size);
                }

                if (loc != null) {
                  frame.setLocation(loc.x, loc.y);
                }

                //Display the window.
                frame.pack();
                frame.setVisible(true);
            }
        });
    
      return frame;
    }

    public static JFrame createAndShowFrame(final String title, final Component component, final Dimension size) {
       return createAndShowFrame(title, component, null, size, null, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component, final Dimension size, final Point loc) {
       return createAndShowFrame(title, component, null, size, loc, false);
    }
    
    /** Use this if already on the EDT */
    public static JFrame createAndShowFrameFromEDT(final String title, final Component component, final Dimension size, final Point loc) {
        //Turn off metal's use of bold fonts
        UIManager.put("swing.boldMetal", Boolean.FALSE);

        JFrame frame = new JFrame(title);

        //Add content to the window.
        frame.getContentPane().add(component);

        if (size != null) {
          frame.setPreferredSize(size);
        }

        if (loc != null) {
          frame.setLocation(loc.x, loc.y);
        }

        //Display the window.
        frame.pack();
        frame.setVisible(true);
        return frame;
    }

    public static JFrame createAndShowFrame(final String title, final Component component, JMenuBar menuBar, final Dimension size) {
       return createAndShowFrame(title, component, menuBar, size, null, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component, JMenuBar menuBar, final Dimension size, final Point loc) {
       return createAndShowFrame(title, component, menuBar, size, loc, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component) {
       return createAndShowFrame(title, component, null, null, null, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component, JMenuBar menuBar) {
       return createAndShowFrame(title, component, menuBar, null, null, false);
    }

    public static JFrame createAndShowFrame(final String title, final Component component, final boolean exitOnClose) {
       return createAndShowFrame(title, component, null, null, null, exitOnClose);
    }
    

    public static HydraRGBDisplayable makeImageDisplayable(FlatField image, Range range, ColorTable colorTable, String name) throws VisADException, RemoteException {

       RealType imageRangeType =
          (((FunctionType)image.getType()).getFlatRange().getRealComponents())[0];

       boolean alphaflag=true;
       HydraRGBDisplayable imageDsp = new HydraRGBDisplayable(name, imageRangeType, null, ColorTable.addAlpha(colorTable), colorTable.getName(), alphaflag, range);
       imageDsp.addConstantMap(new ConstantMap(0.0, Display.RenderOrderPriority));
       imageDsp.setData(image);
    
       return imageDsp;
    }

    public static ImageRGBDisplayable makeRGBImageDisplayable(FlatField rgbImage) throws VisADException, RemoteException {
        return makeRGBImageDisplayable(rgbImage, null);
    }
    
    public static ImageRGBDisplayable makeRGBImageDisplayable(FlatField rgbImage, String name) throws VisADException, RemoteException {
       ImageRGBDisplayable rgbDisplayable = new ImageRGBDisplayable(name, grayTable.getTable(), false, rgbImage);
       rgbDisplayable.addConstantMap(new ConstantMap(0.0, Display.RenderOrderPriority));
       rgbDisplayable.setData(rgbImage);
       
       return rgbDisplayable;
    }

   /**
    * Takes a FlatField, returns one with a unique RangeType name.
    *
    */

    public static FlatField makeFlatFieldWithUniqueRange(FlatField ffield) throws VisADException, RemoteException {
       FunctionType fncType = (FunctionType) ffield.getType();
       RealTupleType imageDomType = fncType.getDomain();
       RealType[] comps = fncType.getFlatRange().getRealComponents();
       int numRangeComps = comps.length;

       FlatField new_image = null;
       float[][] rngValues = ffield.getFloats(false);

       if (numRangeComps == 1) {
          RealType imageRangeType = ((fncType).getFlatRange().getRealComponents())[0];
  
          String str = imageRangeType.getName();
          String new_name = str+"_"+numImageDisplays;

          imageRangeType = RealType.getRealType(new_name);
          new_image = new FlatField(new FunctionType(imageDomType, imageRangeType), ffield.getDomainSet());
       }
       else {
          RealType[] rtypes = new RealType[numRangeComps];
          for (int k=0; k<rtypes.length; k++) {
             String str = comps[k].getName();
             String new_name = str+"_"+numImageDisplays;
             rtypes[k] =  RealType.getRealType(new_name);
          }
          new_image = new FlatField(new FunctionType(imageDomType, new RealTupleType(rtypes)), ffield.getDomainSet());
       }
       new_image.setSamples(rngValues);

       numImageDisplays++;
       return new_image;
    }


   //- TODO:  break out these map routines

   private static UnionSet mapVHRES = null;
   private static UnionSet mapSUPU = null;
   private static UnionSet mapSUPW = null;
   private static UnionSet mapHPOL = null;

   public static void initializeMapBoundaries() {
      try {
         URL  mapSource = Hydra.class.getResource("/auxdata/maps/OUTLSUPU");
         BaseMapAdapter mapAdapter = new BaseMapAdapter(mapSource);
         mapSUPU = mapAdapter.getData();

         mapSource = Hydra.class.getResource("/auxdata/maps/OUTLSUPW");
         mapAdapter = new BaseMapAdapter(mapSource);
         mapSUPW = mapAdapter.getData();

         mapSource = Hydra.class.getResource("/auxdata/maps/OUTLHPOL");
         mapAdapter = new BaseMapAdapter(mapSource);
         mapHPOL = mapAdapter.getData();

         mapSource = Hydra.class.getResource("/resources/geographic/gshhs_h.b");
         GSHHGadapter = new GSHHG(mapSource);
         
         mapSource = Hydra.class.getResource("/resources/geographic/cia_wdb2-bdy-pby.txt");
         WDBIIadapter = new WorldDataBank(mapSource);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public static void initializeColorTables() {
      float[][] table = null;
      float[][] inv_table = null;
      ArrayList<float[]> colors = new ArrayList<float[]>();

      try {
         URL url = Hydra.class.getResource("/resources/color/rainbow.txt");
         BufferedReader reader =  new BufferedReader(new InputStreamReader(url.openStream()));
  
         int clrDim = 3;
         int cnt = 0;
         while (true) {
            String line = reader.readLine();
            if (line == null) break;
            StringTokenizer tokens = new StringTokenizer(line);
            if (cnt == 0) {
              clrDim = tokens.countTokens();
            }
            String red = tokens.nextToken();
            String grn = tokens.nextToken();
            String blu = tokens.nextToken();
            float[] tmp = new float[clrDim];
            tmp[0] = Float.parseFloat(red);
            tmp[1] = Float.parseFloat(grn);
            tmp[2] = Float.parseFloat(blu);
            colors.add(tmp);
            cnt++;
         }
         int numClrs = colors.size();
         table = new float[clrDim][numClrs];
         inv_table = new float[clrDim][numClrs];
         for (int k=0; k<numClrs; k++) {
            float[] tmp = colors.get(k);
            table[0][k] = tmp[0]/numClrs;
            table[1][k] = tmp[1]/numClrs;
            table[2][k] = tmp[2]/numClrs;
            inv_table[0][(numClrs-1)-k] = tmp[0]/numClrs;
            inv_table[1][(numClrs-1)-k] = tmp[1]/numClrs;
            inv_table[2][(numClrs-1)-k] = tmp[2]/numClrs;
         }

         reader.close();
      }
      catch (Exception e) {
         e.printStackTrace();
      }

      rainbow = new ColorTable("rainbow", "rainbow", table);
      invRainbow = new ColorTable("invRainbow", "invRainbow", inv_table);
      
      table = new float[3][256];
      table = BaseColorControl.initTableVis5D(table);
      heat = new ColorTable("heat", "heat", table);
   }

   public static MapLines addBaseMapToDisplay(MapProjectionDisplay mapProjDsp, UnionSet set) throws VisADException, RemoteException {
       return addBaseMapToDisplay(mapProjDsp, set, Color.cyan);
   }

   public static MapLines addBaseMapToDisplay(MapProjectionDisplay mapProjDsp, UnionSet set, Color color) throws VisADException, RemoteException {
        MapLines mapLines  = new MapLines("maplines");
        //mapLines.setUseFastRendering(true);
        mapLines.addConstantMap(new ConstantMap(5.0, Display.RenderOrderPriority));
        try {
            mapLines.setMapLines(set);
            mapLines.setColor(color);
            mapProjDsp.addDisplayable(mapLines);
        } catch (Exception excp) {
            System.out.println(excp);
        }

        return mapLines;
   }

   public static void addBaseMap(MapProjectionDisplayJ3D mapProjDsp) throws VisADException, RemoteException {
      ArrayList<MapLines> baseMap = new ArrayList<MapLines>();

      UnionSet local = new UnionSet(mapSUPU.getSets());
      baseMap.add(addBaseMapToDisplay(mapProjDsp, local));

      local = new UnionSet(mapHPOL.getSets());
      baseMap.add(addBaseMapToDisplay(mapProjDsp, local));

      local = new UnionSet(mapSUPW.getSets());
      baseMap.add(addBaseMapToDisplay(mapProjDsp, local));
   }

   public static ArrayList addBaseMapVHRES(MapProjectionDisplayJ3D mapProjDsp) throws VisADException, RemoteException {
      ArrayList<MapLines> baseMap = new ArrayList<MapLines>();

      WDBIIadapter.setRegion(-90f, 90f, -180f, 180f);
      baseMap.add(addBaseMapToDisplay(mapProjDsp, WDBIIadapter.getData()));

      setRegion(mapProjDsp.getMapProjection());
      UnionSet mapVHRES = GSHHGadapter.getData();
      baseMap.add(addBaseMapToDisplay(mapProjDsp, mapVHRES));

      return baseMap;
   }

   public static void updateBaseMapVHRES(ArrayList<MapLines> mapList, MapProjection mapProj) throws VisADException, RemoteException {
      setRegion(mapProj);
      mapList.get(1).setData(GSHHGadapter.getData());
   }

   public static void setRegion(MapProjection mapProj) throws VisADException, RemoteException {
      Rectangle2D rect = mapProj.getDefaultMapArea();
      double x = rect.getX();
      double w = rect.getWidth();
      double y = rect.getY();
      double h = rect.getHeight();
      // TODO: make this better
      double f = 0.58;
      if (mapProj instanceof GEOSProjection) {
         f = 0.0;
      }
      
      double[] leftUp = new double[] {x-f*w, y+f*h+h};
      double[] leftDn = new double[] {x-f*w, y-f*h};
      double[] rghtDn = new double[] {x+f*w+w, y-f*h};
      double[] rghtUp = new double[] {x+f*w+w, y+f*h+h};
      double[] cntrUp = new double[] {leftUp[0]+w/2, leftUp[1]};
      
      LatLonPoint llp_leftUp = mapProj.getLatLon(new double[][] {{leftUp[0]}, {leftUp[1]}});
      LatLonPoint llp_leftDn = mapProj.getLatLon(new double[][] {{leftDn[0]}, {leftDn[1]}});
      LatLonPoint llp_rghtUp = mapProj.getLatLon(new double[][] {{rghtUp[0]}, {rghtUp[1]}});
      LatLonPoint llp_rghtDn = mapProj.getLatLon(new double[][] {{rghtDn[0]}, {rghtDn[1]}});
      LatLonPoint llp_cntrUp = mapProj.getLatLon(new double[][] {{cntrUp[0]}, {cntrUp[1]}});
      
      float latA = (float) (llp_leftUp.getLatitude()).getValue();
      float latB = (float) (llp_leftDn.getLatitude()).getValue();
      float latC = (float) (llp_rghtDn.getLatitude()).getValue();
      float latD = (float) (llp_rghtUp.getLatitude()).getValue();
      float latCntr = (float) (llp_cntrUp.getLatitude()).getValue();

      float latMin = (float) (llp_leftDn.getLatitude()).getValue();
      float latMax = (float) (llp_leftUp.getLatitude()).getValue();
      float lonWest = (float) (llp_leftUp.getLongitude()).getValue();
      float lonEast = (float) (llp_rghtUp.getLongitude()).getValue();

      if (latA> 0 && latB>0 && latC>0 && latD>0) {
         if (latCntr > latMax) latMax = 90;
      }

      float lonA = (float) (llp_leftUp.getLongitude()).getValue();
      float lonB = (float) (llp_leftDn.getLongitude()).getValue();
      float lonC = (float) (llp_rghtDn.getLongitude()).getValue();
      float lonD = (float) (llp_rghtUp.getLongitude()).getValue();

      int numCrossed = 0;

      if (crossesGreenwich(leftUp, leftDn, rghtDn, rghtUp, lonA, lonB, lonC, lonD, 0, mapProj)) numCrossed++;
      if (crossesGreenwich(leftUp, leftDn, rghtDn, rghtUp, lonA, lonB, lonC, lonD, 1, mapProj)) numCrossed++;
      if (crossesGreenwich(leftUp, leftDn, rghtDn, rghtUp, lonA, lonB, lonC, lonD, 2, mapProj)) numCrossed++;
      if (crossesGreenwich(leftUp, leftDn, rghtDn, rghtUp, lonA, lonB, lonC, lonD, 3, mapProj)) numCrossed++;

      if (numCrossed != 1 || numCrossed == 0) {
         GSHHGadapter.setRegion(latMin, latMax, lonWest, lonEast);
      } 
      else {
         if (latMin < 0) latMin = -90f;
         if (latMin > 0) latMax = 90f;
         GSHHGadapter.setRegion(latMin, latMax);
      }
   }

   public static boolean crossesGreenwich(double[] leftUp, double[] leftDn, double[] rghtDn, double[] rghtUp, 
                                         double lonA, double lonB, double lonC, double lonD, 
                                         int which, MapProjection mapProj) throws VisADException {
      double itrvl = 0;
      double[] pt = null;
      double[] testPt = new double[2];
      LatLonPoint llpt;
      double lon;
      double delLon = 0;

      if (lonA < 0) lonA += 360;
      if (lonB < 0) lonB += 360;
      if (lonC < 0) lonC += 360;
      if (lonD < 0) lonD += 360;

      if (which == 0) {
         itrvl = (leftDn[1] - leftUp[1])/20;
         pt = leftUp;
         testPt[0] = leftUp[0];
         testPt[1] = leftUp[1] + itrvl;
         delLon = lonB - lonA;
      }
      else if (which == 1) {
         itrvl = (rghtDn[0] - leftDn[0])/20;
         pt = leftDn;
         testPt[0] = leftDn[0] + itrvl;
         testPt[1] = leftDn[1];
         delLon = lonC - lonB;
      }
      else if (which == 2) {
         itrvl = (rghtUp[1] - rghtDn[1])/20;
         pt = rghtDn;
         testPt[0] = rghtDn[0];
         testPt[1] = rghtDn[1] + itrvl;
         delLon = lonD - lonC;
      }
      else if (which == 3) {
         itrvl = (leftUp[0] - rghtUp[0])/20;
         pt = rghtUp;
         testPt[0] = rghtUp[0] + itrvl;
         testPt[1] = rghtUp[1];
         delLon = lonA - lonD;
      }

      llpt = mapProj.getLatLon(new double[][] {{pt[0]}, {pt[1]}});
      lon = llpt.getLongitude().getValue();

      llpt = mapProj.getLatLon(new double[][] {{testPt[0]}, {testPt[1]}});
      double testLon = llpt.getLongitude().getValue();

      if (lon < 0) lon += 360;
      if (testLon < 0 ) testLon += 360;
      
      if (delLon > 0) {
        if (testLon < lon) return true;
      } 
      else {
        if (testLon > lon) return true;
      }
     
      return false;
   }
   
   public static MapProjection getSwathProjection(float[][] corners) throws VisADException {
      MapProjection mp = new LambertAEA(corners);
      return mp;
   }

  public static Linear2DSet makeGrid(MapProjection mp, float[][] corners, float res) throws Exception {
     float[][] xy = mp.fromReference(corners);

     float min_x = Float.MAX_VALUE;
     float min_y = Float.MAX_VALUE;
     float max_x = -Float.MAX_VALUE;
     float max_y = -Float.MAX_VALUE;

     for (int k=0; k<xy[0].length;k++) {
       if (xy[0][k] < min_x) min_x = xy[0][k];
       if (xy[1][k] < min_y) min_y = xy[1][k];
       if (xy[0][k] > max_x) max_x = xy[0][k];
       if (xy[1][k] > max_y) max_y = xy[1][k];
     }

     RealType xmap = RealType.getRealType("xmap", CommonUnit.meter);
     RealType ymap = RealType.getRealType("ymap", CommonUnit.meter);

     RealTupleType rtt = new visad.RealTupleType(xmap, ymap, mp, null);

     min_x = ((int) (min_x/res)) * res;
     max_x = ((int) (max_x/res)) * res;
     min_y = ((int) (min_y/res)) * res;
     max_y = ((int) (max_y/res)) * res;

     float del_x = max_x - min_x;
     float del_y = max_y - min_y;

     int xLen = (int) (del_x/res);
     int yLen = (int) (del_y/res);

     Linear2DSet grid = new Linear2DSet(rtt, min_x, min_x + (xLen-1)*res, xLen,
                                 min_y, min_y + (yLen-1)*res, yLen);

     return grid;
  }
  
  public static FlatField makeGEOSRadiansDomainField(FlatField fltFld, GEOSProjection coordSys) throws VisADException, RemoteException  {
       Linear2DSet domainSet = (Linear2DSet) fltFld.getDomainSet();
       MathType rangeType = ((FunctionType)fltFld.getType()).getRange();
       float[][] rangeVals = fltFld.getFloats(false);
       Linear1DSet setX = domainSet.getX();
       Linear1DSet setY = domainSet.getY();

       int lenX = setX.getLength();
       int lenY = setY.getLength();

       double scaleX = ((GEOSProjection)coordSys).getScaleX();
       double scaleY = ((GEOSProjection)coordSys).getScaleY();
       double offsetX = ((GEOSProjection)coordSys).getOffsetX();
       double offsetY = ((GEOSProjection)coordSys).getOffsetY();

       double firstX = setX.getFirst()*scaleX + offsetX;
       double firstY = setY.getFirst()*scaleY + offsetY;

       double lastX = setX.getLast()*scaleX + offsetX;
       double lastY = setY.getLast()*scaleY + offsetY;   

       Linear2DSet dSetRadians = new Linear2DSet(firstX, lastX, lenX, firstY, lastY, lenY);
       fltFld = new FlatField(new FunctionType(RealTupleType.Generic2D, rangeType), dSetRadians);
       fltFld.setSamples(rangeVals, false);  
       return fltFld;
  }


   public static MapProjection getDataProjection(FlatField image) throws VisADException, RemoteException {
      MapProjection mp = null;
      //- get MapProjection from incoming image.  If none, use default method
      FunctionType fnc_type = (FunctionType) image.getType();
      RealTupleType rtt = fnc_type.getDomain();
      CoordinateSystem cs = rtt.getCoordinateSystem();
      Set domainSet = image.getDomainSet();

      if (cs instanceof visad.CachingCoordinateSystem) {
         cs = ((visad.CachingCoordinateSystem)cs).getCachedCoordinateSystem();
      }

      if (cs instanceof MapProjection) {
         return (MapProjection) cs;
      }
      else if (cs instanceof LongitudeLatitudeCoordinateSystem) {
         //- get approximate center lon,lat
         int[] lens = ((GriddedSet)domainSet).getLengths();
         float[][] center = ((GriddedSet)domainSet).gridToValue(new float[][] {{lens[0]/2}, {lens[1]/2}});
         center = cs.toReference(center);
         float[][] corners = MultiSpectralData.getLonLatBoundingCorners(image.getDomainSet());
         try {
           mp = new LambertAEA(corners, center[0][0], center[1][0]);
         } catch (Exception e) {
           System.out.println(" getDataProjection"+e);
         }
         return mp;
      }

      float minLon = Float.NaN;
      float minLat = Float.NaN;
      float delLon = Float.NaN;
      float delLat = Float.NaN;

      if (domainSet instanceof LinearLatLonSet) {
         MathType type0 = ((SetType)domainSet.getType()).getDomain().getComponent(0);
         int latI = RealType.Latitude.equals(type0) ? 0 : 1;
         int lonI = (latI == 1) ? 0 : 1;

         float[] min = ((LinearLatLonSet)domainSet).getLow();
         float[] max = ((LinearLatLonSet)domainSet).getHi();
         minLon = min[lonI];
         minLat = min[latI];
         delLon = max[lonI] - min[lonI];
         delLat = max[latI] - min[latI];

         try {
            mp = new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple,
                    new Rectangle2D.Float(minLon, minLat, delLon, delLat));
         } catch (Exception e) {
            e.printStackTrace();
         }

         return mp;
      }
      else if (domainSet instanceof Gridded2DSet) {
        rtt = ((SetType)domainSet.getType()).getDomain();
        rtt = RealTupleType.SpatialEarth2DTuple;
        if (!(rtt.equals(RealTupleType.SpatialEarth2DTuple) || rtt.equals(RealTupleType.LatitudeLongitudeTuple))) {
          minLon = -180f;
          minLat = -90f;
          delLon = 360f;
          delLat = 180f;
        }
        else {
          int latI = rtt.equals(RealTupleType.SpatialEarth2DTuple) ? 1 : 0;
          int lonI = (latI == 1) ? 0 : 1;

          float[] min = ((Gridded2DSet)domainSet).getLow();
          float[] max = ((Gridded2DSet)domainSet).getHi();
          minLon = min[lonI];
          minLat = min[latI];
          delLon = max[lonI] - min[lonI];
          delLat = max[latI] - min[latI];
        }
      }

      try {
         mp = new TrivialMapProjection(RealTupleType.SpatialEarth2DTuple,
                 new Rectangle2D.Float(minLon, minLat, delLon, delLat));
      } catch (Exception e) {
          e.printStackTrace();
      }

      return mp;
   }

   public static String makeKML(double south, double north, double west, double east, String kmlPath, String imagePath) {
     Object obj = new Object();
     URL url = obj.getClass().getResource("/resources/hydra.kml");
     SAXBuilder builder = new SAXBuilder();
     Document doc = null;

     try {
       doc = builder.build(url);
     } catch (Exception e) {
       e.printStackTrace();
     }
     Element root = doc.getRootElement();

     List list = root.getChildren();

     List parms = ((Element)list.get(0)).getChildren();
     
     Element icon = (Element) parms.get(2);
     Element href = (Element) (icon.getChildren()).get(0);
     href.setText(imagePath);

     Element latlonbox = (Element) parms.get(3);
     List vals = latlonbox.getChildren();
    
     org.jdom2.Element elemN = (Element)vals.get(0);
     org.jdom2.Element elemW = (Element)vals.get(1);
     org.jdom2.Element elemS = (Element)vals.get(2);
     org.jdom2.Element elemE = (Element)vals.get(3);

     elemN.setText((new Float(north)).toString());
     elemW.setText((new Float(west)).toString());
     elemS.setText((new Float(south)).toString());
     elemE.setText((new Float(east)).toString());
     
     XMLOutputter xmlOut = new XMLOutputter();
     String newStr = xmlOut.outputString(doc);

     try {
       File file = new File(kmlPath);
       FileOutputStream fos = new FileOutputStream(file);
       fos.write(newStr.getBytes());
       fos.close();
     }
     catch (FileNotFoundException e) {
        System.out.println(e);
     }
     catch (IOException e) {
        System.out.println(e);
     }

     return newStr;
   }

    public static ConstantMap[] makeConstantMapArray(ConstantMap[] cmaps, ConstantMap cmap) {
       ConstantMap[] constantMaps = new ConstantMap[cmaps.length+1];
       for (int k=0; k<cmaps.length; k++) {
          constantMaps[k] = cmaps[k];
       }
       constantMaps[cmaps.length] = cmap;
       return constantMaps;
    }
    
    public static ConstantMap[] makeColorMap(final Color color)
        throws VisADException, RemoteException 
    {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;
        return new ConstantMap[] { new ConstantMap(r, Display.Red),
                                   new ConstantMap(g, Display.Green),
                                   new ConstantMap(b, Display.Blue),
                                   new ConstantMap(a, Display.Alpha) };
    }
    
    public static final float[][] grayTable(int numColors, boolean inverse)
            throws IllegalArgumentException {

        float[][] table = new float[3][numColors];

        float     scale = (float) (1.0f / (float) (numColors - 1));
        for (int i = 0; i < numColors; i++) {
            float a = (inverse)
                      ? ((float) numColors - i)
                      : ((float) i);
            table[0][i] = a * scale;  // Red amount
            table[1][i] = a * scale;  // Green
            table[2][i] = a * scale;  // Blue
        }
        return table;
    }


    public static void setRegionMatching(boolean on) {
       regionMatch = on;
    }

    public static boolean getRegionMatching() {
       return regionMatch;
    }

    public static void resetSharedProjection() {
       sharedMapProj = null;
    }

    public static void setReprojectMode(int mode) {
       reprojectMode = mode;
    }

    public static int getReprojectMode() {
       return reprojectMode;
    }
    
    public static void setDoParallel(boolean enable) {
       doParallel = enable;
       ReprojectSwath.setDoParallel(enable);
    }

    public static boolean getDoParallel() {
       return doParallel;
    }
    
    public synchronized static int getUniqueID() {
        uniqueID++;
        return uniqueID;
    }
}
