package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.hydra.Hydra;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import visad.VisADException;
import visad.Data;
import visad.FlatField;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;
import ucar.unidata.util.ColorTable;


public class AHIDirectory extends DataSource {


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
   
   DataGroup catHKM = new DataGroup("HKM");
   DataGroup cat1KM = new DataGroup("1KM");
   DataGroup cat2KM = new DataGroup("2KM");
   
   
   double default_stride = 10;
   
   private ArrayList<DataChoice> myDataChoices = new ArrayList<DataChoice>();

   public AHIDirectory(File directory) {
     this(directory.listFiles());
   }

   public AHIDirectory(File[] files) {

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
          
          DataGroup cat = null;
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
            datasource = new GEOSDataSource(fileList.get(0), default_stride);
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


   public void doMakeDataChoice(String name, int bandIdx, int idx, DataChoice targetDataChoice, DataGroup category) {
     DataChoice dataChoice = new DataChoice(this, name, category);
     dataChoice.setDataSelection(targetDataChoice.getDataSelection());
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
   
   public boolean getDoReproject(DataChoice choice) {
     return false;
   }
   
   public ColorTable getDefaultColorTable(DataChoice choice) {
      ColorTable clrTbl = Hydra.grayTable;
      String name = choice.getName();
      if ( name.equals("B07") || name.equals("B08") || name.equals("B09") || name.equals("B10") || name.equals("B11") || name.equals("B12") || name.equals("B13") || name.equals("B14") || name.equals("B15") || name.equals("B16") ) {
        clrTbl = Hydra.invGrayTable;
      }
      
      return clrTbl;
   }
   
    public void addDataChoice(DataChoice dataChoice) {
      myDataChoices.add(dataChoice); 
    }
  
    public List getDataChoices() {
      return myDataChoices; 
    }

    public Data getData(DataChoice dataChoice, DataSelection dataSelection)
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
       
       targetDataChoice.setDataSelection(dataChoice.getDataSelection());      
       Data data = datasource.getData(targetDataChoice);
       if (targetDataChoice.getName().equals("brightness_temp")) {
          float[][] rngVals = ((FlatField)data).getFloats(false);
          for (int k=0; k<rngVals[0].length; k++) {
             float fval = rngVals[0][k];
             if (fval < 150f || fval > 340f) {
                rngVals[0][k] = Float.NaN;
             }
          }
       }
       return data;
    }

}
