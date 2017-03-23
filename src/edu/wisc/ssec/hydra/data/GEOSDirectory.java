package edu.wisc.ssec.hydra.data;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import visad.VisADException;
import visad.Data;
import java.rmi.RemoteException;


public class GEOSDirectory extends DataSource {

   public float[] nadirResolution;

   public String[] bandNames;

   public float[] centerWavelength;
   
   ArrayList<String> targetList = new ArrayList();
   
   HashMap<String, ArrayList> bandIDtoFileList = new HashMap();

   HashMap<String, GEOSDataSource> bandIDtoDataSource = new HashMap();

   HashMap<String, String> bandIDtoBandName = new HashMap();
   HashMap<String, String> bandNameToBandID = new HashMap();

   String dateTimeStamp = null;
   
   DataGroup catHKM = new DataGroup("HKM");
   DataGroup cat1KM = new DataGroup("1KM");
   DataGroup cat2KM = new DataGroup("2KM");
   
   DataGroup[] category;
   
   public int[] default_stride;
   
   boolean unpack = false;

   public GEOSDirectory(File directory) {
     this(directory.listFiles());
   }

   public GEOSDirectory(File[] files) {
   }

   void init(File[] files) {
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
              if (fileBelongsToThis(name) && name.contains(bandNames[k])) {
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
               dateTimeStamp = DataSource.getDateTimeStampFromFilename(filename);
            }
            catch (Exception e) {
               e.printStackTrace();
            }
          }
          
          GEOSDataSource datasource = null;
          try {
            datasource = new GEOSDataSource(fileList.get(0), default_stride[k], unpack);
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
            if (targetList.contains(name)) {
              targetDataChoice = choice;
            }
          }

          doMakeDataChoice(bandNames[k], k, num, targetDataChoice, category[k]);

          num++;
        }
      }
      
   }

   void doMakeDataChoice(String name, int bandIdx, int idx, DataChoice targetDataChoice, DataGroup category) {
     DataChoice dataChoice = new DataChoice(this, name, category);
     dataChoice.setDataSelection(targetDataChoice.getDataSelection());
     myDataChoices.add(dataChoice);      
   }

   @Override
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

   @Override
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

   @Override
   public String getDateTimeStamp() {
     return dateTimeStamp;
   }
   
   @Override
   public boolean getDoReproject(DataChoice choice) {
     return false;
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
          if (targetList.contains(name)) {
             targetDataChoice = choice;
          }
       }
       
       targetDataChoice.setDataSelection(dataChoice.getDataSelection());      
       Data data = datasource.getData(targetDataChoice);
       data = postProcess(targetDataChoice, data);
       return data;
    }
    
    boolean fileBelongsToThis(String filename) {
       return true;
    }
    
    Data postProcess(DataChoice choice, Data data) throws VisADException, RemoteException {
       return data;
    }

}
