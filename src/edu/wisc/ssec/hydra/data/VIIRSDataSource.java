package edu.wisc.ssec.hydra.data;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataChoice;
import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.lang.Math;
import visad.VisADException;
import visad.Data;
import visad.FlatField;
import java.rmi.RemoteException;


public class VIIRSDataSource extends DataSourceImpl {


   public static String[] bands =  new String[] {
     "SVI01", "SVI02", "SVI03", "SVI04", "SVI05", 
     "SVM01", "SVM02", "SVM03", "SVM04", "SVM05",
     "SVM06", "SVM07", "SVM08", "SVM09", "SVM10",
     "SVM11", "SVM12", "SVM13", "SVM14", "SVM15", 
     "SVM16", "SVDNB"
   };

   public static float[] nadirResolution = new float[] {
     380f, 380f, 380f, 380f, 380f,
     770f, 770f, 770f, 770f, 770f,
     770f, 770f, 770f, 770f, 770f,
     770f, 770f, 770f, 770f, 770f,
     770f, 770f
   };

   public static String[] bandNames =  new String[] {
     "I1", "I2", "I3", "I4", "I5",
     "M1", "M2", "M3", "M4", "M5",
     "M6", "M7", "M8", "M9", "M10",
     "M11", "M12", "M13", "M14", "M15",
     "M16", "DNB"
   };

   public static float[] centerWavelength = new float[] {
     0.640f, 0.856f, 1.610f, 3.740f, 11.450f, 0.412f, 0.445f, 0.488f, 0.555f, 0.672f,
     0.746f, 0.865f, 1.240f, 1.378f, 1.61f, 2.250f, 3.700f, 4.050f, 8.550f, 10.763f, 12.013f, 0.70f 
   };
   
   HashMap<String, ArrayList> bandIDtoFileList = new HashMap<String, ArrayList>();

   HashMap<String, NOAA_VIIRS_DataSource> bandIDtoDataSource = new HashMap<String, NOAA_VIIRS_DataSource>();

   HashMap<String, String> bandIDtoBandName = new HashMap<String, String>();
   HashMap<String, String> bandNameToBandID = new HashMap<String, String>();

   List catI = DataCategory.parseCategories("I-Band;IMAGE");
   List catM = DataCategory.parseCategories("M-Band;IMAGE");
   List catDNB = DataCategory.parseCategories("DNB-Band;IMAGE");

   String dateTimeStamp = null;

   public VIIRSDataSource(File directory) {
     this(directory.listFiles());
   }

   public VIIRSDataSource(File[] files) {
      super(new DataSourceDescriptor(), "VIIRS", "VIIRS", new Hashtable());

      int numFiles = files.length;
      int numBands = bands.length;

      boolean[] used = new boolean[numFiles];

      for (int k=0; k<numBands; k++) {
         ArrayList<File> fileList = new ArrayList<File>();
         bandIDtoFileList.put(bands[k], fileList);
         bandIDtoBandName.put(bands[k], bandNames[k]);
         bandNameToBandID.put(bandNames[k], bands[k]);

         for (int t=0; t<numFiles; t++) {
            if (!used[t]) {
              File file = files[t];
              String name = file.getName();
              String filename = file.getAbsolutePath();
              if (name.startsWith(bands[k]) && name.endsWith(".h5")) {
                fileList.add(file);
                used[t] = true;
              }
            }
         }
      }

      // create DataSource for each band
      int num = 0;
      for (int k=0; k<numBands; k++) {
        ArrayList<File> fileList = bandIDtoFileList.get(bands[k]);
        if (!fileList.isEmpty()) {

          if (dateTimeStamp == null) {
            dateTimeStamp = DataSource.getDateTimeStampFromFilename((String)fileList.get(0).getName(), "Suomi");
          }

          NOAA_VIIRS_DataSource datasource = null;
          try {
            ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(fileList, "Suomi");
            File[] tmpFiles = new File[sortedList.size()];
            for (int i=0; i<tmpFiles.length; i++) {
               tmpFiles[i] = new File(sortedList.get(i));
            }
            datasource = new NOAA_VIIRS_DataSource(tmpFiles);
            bandIDtoDataSource.put(bands[k], datasource);
          }
          catch (Exception e) {
             e.printStackTrace();
          }

          
          List dataChoices = datasource.getDataChoices();

          DataChoice targetDataChoice = null;

          for (int t=0; t< dataChoices.size(); t++) {
            DataChoice choice = (DataChoice) dataChoices.get(t);
            String name = choice.getName(); 
            if (name.contains("Reflectance") || name.contains("BrightnessTemperature")) {
              targetDataChoice = choice;
            }
            if (bands[k].equals("SVDNB") && name.contains("Radiance")) {
              targetDataChoice = choice;
            }
          }

          List cat = (k <= 4) ? catI : catM;
          if (k == 21) cat = catDNB;

          doMakeDataChoice(bandNames[k], k, num, targetDataChoice, cat);

          num++;
        }
      }

   }

   public void doMakeDataChoice(String name, int bandIdx, int idx, DataChoice targetDataChoice, List category) {
     Hashtable subset = targetDataChoice.getProperties();
     DataChoice dataChoice = new DirectDataChoice(this, idx, name, name, category, subset);
     dataChoice.setProperties(subset);
     addDataChoice(dataChoice);
   }

   public float getNadirResolution(DataChoice choice) throws Exception {
      String name = choice.getName();
      float res = 0;

      for (int k=0; k<bands.length; k++) {
        if (name.equals(bandNames[k])) {
          res = nadirResolution[k];
          break;
        }
      }

      if (res == 0) {
        throw new Exception("Item not found so can't get resolution");
      }
      else {
        return res;
      }
   }

   public String getDescription(DataChoice choice) {
      String name = choice.getName();
      
      float cntrWvln = 0;
      for (int k=0; k<bands.length; k++) {
         if (name.equals(bandNames[k])) {
            cntrWvln = centerWavelength[k];
            break;
         }
      }

      if (cntrWvln == 0) {
        return null;
      }
      else {
        return "("+cntrWvln+")";
      }

   }

   public String getDescription() {
     return "NPP VIIRS";
   }

   public String getDateTimeStamp() {
     return dateTimeStamp;
   }

    public synchronized Data getData(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
       return this.getDataInner(dataChoice, category, dataSelection, requestProperties);
    }


    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException 
    {
       String name = dataChoice.getName();
       name = bandNameToBandID.get(name);
       NOAA_VIIRS_DataSource datasource = bandIDtoDataSource.get(name);
       List dataChoices = datasource.getDataChoices();

       DataChoice targetDataChoice = null;
 
       for (int t=0; t< dataChoices.size(); t++) {
          DataChoice choice = (DataChoice) dataChoices.get(t);
          name = choice.getName();
          if (name.contains("Reflectance") || name.contains("BrightnessTemperature")) {
             targetDataChoice = choice;
          }
          if (dataChoice.getName().equals("DNB") && name.contains("Radiance")) {
             targetDataChoice = choice;
          } 
       }
      
       targetDataChoice.setProperties(dataChoice.getProperties());
       Data data = datasource.getData(targetDataChoice, null, null, null);
       //-  post process for DNB: replace radiance with log10(radiance)
       if (dataChoice.getName().equals("DNB")) {
          float[][] rngVals = ((FlatField)data).getFloats(false);
          for (int k=0; k<rngVals[0].length; k++) {
             float fval = rngVals[0][k];
             if (fval <= 0f) {
                rngVals[0][k] = Float.NaN;
             }
             else {
                rngVals[0][k] = (float) Math.log10((double)fval);
             }
          }
       }
       return data;
    }

}
