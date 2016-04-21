package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.MultiDimensionSubset;
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
import visad.QuickSort;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class AHIDirectory extends DataSourceImpl {


   public static float[] nadirResolution = new float[] {
     1000f, 1000f, 500f, 1000f,
     2000f, 2000f, 2000f, 2000f,
     2000f, 2000f, 2000f, 2000f,
     2000f, 2000f, 2000f, 2000f
   };

   public static String[] bandNames =  new String[] {
     "B01", "B02", "B03", "B04", "B05",
     "B06", "B07", "B08", "B09", "B10",
     "B11", "B12", "B13", "B14", "B15", "B16"
   };

   public static float[] centerWavelength = new float[] {
     0.47f, 0.51f, 0.64f, 0.86f, 1.6f, 2.3f, 3.9f, 6.2f, 6.9f, 7.3f,
     8.6f, 9.6f, 10.4f, 11.2f, 12.4f, 13.3f
   };
   
   HashMap<String, ArrayList> bandIDtoFileList = new HashMap<String, ArrayList>();

   HashMap<String, GEOSDataSource> bandIDtoDataSource = new HashMap<String, GEOSDataSource>();

   HashMap<String, String> bandIDtoBandName = new HashMap<String, String>();
   HashMap<String, String> bandNameToBandID = new HashMap<String, String>();

   String dateTimeStamp = null;
   
   List catHKM = DataCategory.parseCategories("HKM;IMAGE");
   List cat1KM = DataCategory.parseCategories("1KM;IMAGE");
   List cat2KM = DataCategory.parseCategories("2KM;IMAGE");
   
   double default_stride = 10;

   public AHIDirectory(File directory) {
     this(directory.listFiles());
   }

   public AHIDirectory(File[] files) {
      super(new DataSourceDescriptor(), "AHI", "AHI", new Hashtable());

      int numFiles = files.length;
      int numBands = bandNames.length;

      boolean[] used = new boolean[numFiles];

      for (int k=0; k<numBands; k++) {
         ArrayList<File> fileList = new ArrayList<File>();
         bandIDtoFileList.put(bandNames[k], fileList);
         bandIDtoBandName.put(bandNames[k], bandNames[k]);
         bandNameToBandID.put(bandNames[k], bandNames[k]);

         for (int t=0; t<numFiles; t++) {
            if (!used[t]) {
              File file = files[t];
              String name = file.getName();
              if (name.startsWith("HS_H08") && name.contains(bandNames[k]) && name.endsWith(".nc")) {
                fileList.add(file);
                used[t] = true;
              }
            }
         }
      }

      int num = 0;
      for (int k=0; k<numBands; k++) {
        ArrayList<File> fileList = bandIDtoFileList.get(bandNames[k]);
        if (!fileList.isEmpty()) {

          if (dateTimeStamp == null) {
            try {
               String filename = (String)fileList.get(0).getName();
               int idx = 7;
               String yyyyMMdd = filename.substring(idx, idx+=8);
               SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
               idx += 1;
               String HHmm = filename.substring(idx, idx+4);
               Date datetime = sdf.parse(yyyyMMdd.concat(HHmm));

               dateTimeStamp = DataSource.makeDateTimeStamp(datetime);
            }
            catch (Exception e) {
               e.printStackTrace();
            }
          }
          
          List cat = null;
          if (k==0 || k==1 || k==3) {
             cat = cat1KM;
             default_stride = 20;
          }
          else if (k==2) {
             cat = catHKM;
             default_stride = 40;
          }
          else {
             cat = cat2KM;
             default_stride = 10;
          }

          GEOSDataSource datasource = null;
          try {
            datasource = new GEOSDataSource(fileList.get(0).getPath(), default_stride);
            bandIDtoDataSource.put(bandNames[k], datasource);
          }
          catch (Exception e) {
             e.printStackTrace();
          }

          
          List dataChoices = datasource.getDataChoices();

          DataChoice targetDataChoice = null;

          for (int t=0; t< dataChoices.size(); t++) {
            DataChoice choice = (DataChoice) dataChoices.get(t);
            String name = choice.getName(); 
            if (name.contains("albedo") || name.contains("brightness_temp")) {
              targetDataChoice = choice;
            }
          }

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

      for (int k=0; k<bandNames.length; k++) {
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
      for (int k=0; k<bandNames.length; k++) {
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
     return "H08 AHI";
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
       GEOSDataSource datasource = bandIDtoDataSource.get(name);
       List dataChoices = datasource.getDataChoices();

       DataChoice targetDataChoice = null;
 
       for (int t=0; t< dataChoices.size(); t++) {
          DataChoice choice = (DataChoice) dataChoices.get(t);
          name = choice.getName();
          if (name.contains("albedo") || name.contains("brightness_temp")) {
             targetDataChoice = choice;
          }
       }
      
       targetDataChoice.setProperties(dataChoice.getProperties());
       Data data = datasource.getData(targetDataChoice, null, null, null);
       if (targetDataChoice.getName().equals("brightness_temp")) {
          float[][] rngVals = ((FlatField)data).getFloats(false);
          for (int k=0; k<rngVals[0].length; k++) {
             float fval = rngVals[0][k];
             if (fval < 170f || fval > 310f) {
                rngVals[0][k] = Float.NaN;
             }
          }
       }
       return data;
    }

}
