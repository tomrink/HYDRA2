package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.hydra.Hydra;
import ucar.unidata.util.Range;
import java.io.File;
import java.rmi.RemoteException;
import ucar.unidata.util.ColorTable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;

import visad.*;

public class DataSource {
   
   public DataSource() {
   }


   public boolean getDoFilter(DataChoice choice) {
       String name = choice.getName();
       
      if (name.equals("Cloud_Mask")) {
         return false;   
      }
      if (name.equals("Cloud_Phase_Infrared") || name.equals("Cloud_Phase")) {
         return false;
      }
      if (name.equals("fire_mask")) {
         return false;
      }
      if (name.equals("Cloud_Type")) {
         return false;
      }
       
      return true;
   }
   
   public boolean getOverlayAsMask(DataChoice choice) {
      String name = choice.getName();
      
      if (name.equals("fire_mask")) {
         return true;
      }
      else if (name.equals("OT_grid_mag")) {
         return true;
      }
      return false;
   }


   public Range getDefaultColorRange(DataChoice choice) {
      String name = choice.getName();

      Range rng = null;

      if (name.equals("Cloud_Mask")) {
         rng = new Range(0f-0.5f, 3f+0.5f);
      }
      if (name.equals("Cloud_Phase_Infrared") || name.equals("Cloud_Phase")) {
         rng = new Range(0f-0.5f, 6f+0.5f);
      }
      if (name.equals("fire_mask")) {
         rng = new Range(0f-0.5f, 9f+0.5f);
      }
      if (name.equals("Cloud_Type")) {
         rng = new Range(0f-0.5f, 9+0.5f);
      }

      return rng; 
   }


   public static String getDescriptionFromFilename(String filename) {
     String desc = null;
     
     if (filename.contains("SEADAS") || filename.contains("seadas")) {
       desc = "SEADAS";
     }
     else if (filename.startsWith("MOD") || filename.startsWith("t1.")) {
       desc = "MODIS T";
     }
     else if (filename.startsWith("MYD") || filename.startsWith("a1.")) {
       desc = "MODIS A";
     }
     else if (filename.startsWith("SV") && filename.contains("_npp_")) {
       desc = "VIIRS";
     }
     else if (filename.startsWith("viirs_l1b")) {
       desc = "VIIRS";
     }
     else if (filename.startsWith("SCRIS_npp") || filename.startsWith("GCRSO-SCRIS_npp")) {
       desc = "CrIS SDR";
     }
     else if (filename.startsWith("SATMS_npp") || filename.startsWith("GATMO-SATMS_npp")) {
       desc = "ATMS SDR";
     }
     else if (filename.contains("L1B.AIRS_Rad")) {
       desc = "AIRS L1B";
     }
     else if (filename.startsWith("IASI_xxx_1C")) {
       desc = "IASI L1C";
     }
     else if (filename.contains("IASI_C") && filename.endsWith(".nc")) {
       desc = "IASI L1C";
     }
     else if (filename.startsWith("iasil1c") && filename.endsWith(".h5")) {
       desc = "IASI L1C";
     } 
     else if (filename.contains("AVHR_C") && filename.endsWith(".nc")) {
       desc = "AVHRR L1B";
     }
     else if (filename.startsWith("HIRS_xxx_1B")) {
       desc = "HIRS L1B";
     }
     else if (filename.contains("HIRS_C") && filename.endsWith(".nc")) {
       desc = "HIRS L1B";
     }
     else if (filename.startsWith("hirsl1c") && filename.endsWith(".h5")) {
       desc = "HIRS L1C";
     }     
     else if (filename.startsWith("MHSx_xxx_1B")) {
       desc = "MHS L1B";
     }
     else if (filename.contains("MHS_C") && filename.endsWith(".nc")) {
       desc = "MHS L1B";
     }
     else if (filename.startsWith("AMSA_xxx_1B")) {
       desc = "AMSU-A L1B";
     }
     else if (filename.contains("AMSUA_C") && filename.endsWith(".nc")) {
       desc = "AMSU-A L1B";
     }
     else if (filename.startsWith("amsual1c") && filename.endsWith(".h5")) {
       desc = "AMSU-A L1C";
     }  
     else if (filename.startsWith("mhsl1c") && filename.endsWith(".h5")) {
       desc = "MHS L1C";
     }     
     else if (filename.startsWith("AVHR_xxx_1B")) {
       desc = "AVHRR L1B";
     }
     else if (filename.contains("MERSI")) {
       desc = "MERSI";
     }
     else if (filename.startsWith("NPR-MIRS")) {
       desc = "MIRS";
     }
     else if (filename.startsWith("geocatL2_OT")) {
       desc = "OT";
     }
     else if (filename.startsWith("geocatL1.HIMAWARI-8") || filename.startsWith("geocatL2.HIMAWARI-8")) {
       desc = "AHI";
     }
     else if (filename.contains("SST") && filename.contains("VIIRS_NPP-ACSPO")) {
       desc = "SST ACSPO";
     }  
     
     return desc;
   }

   public static String getDateTimeStampFromFilename(String filename) {
      long millis = getSecondsSinceEpoch(filename);
      return makeDateTimeStamp(millis);
   }
   
   public static long getSecondsSinceEpoch(String filename) {
     long millis = 0;
     Date datetime = null;
     
     try {
        if (filename.contains("_npp_d")) {
           int idx = filename.indexOf("_npp_d");
           idx += 6;
           String str = filename.substring(idx, idx+16);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmmss");
           datetime = sdf.parse(str);
        }
        else if (filename.startsWith("MOD") || filename.startsWith("MYD")) {
           String yyyyddd = filename.substring(10,17);
           String hhmm = filename.substring(18,22);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDDHHmm");
           String yyyyDDDHHmm = yyyyddd.concat(hhmm);
           datetime = sdf.parse(yyyyDDDHHmm);
        }
        else if (filename.startsWith("a1") || filename.startsWith("t1")) {
           SimpleDateFormat sdf = new SimpleDateFormat("yyDDDHHmm");
           String yyddd = filename.substring(3,8);
           String hhmm = filename.substring(9,13);
           String yyyyDDDHHmm = yyddd.concat(hhmm);
           datetime = sdf.parse(yyyyDDDHHmm);
        }
        else if (filename.startsWith("HIRS_xxx_1B") || filename.startsWith("MHSx_xxx_1B") ||
                 filename.startsWith("AVHR_xxx_1B") || filename.startsWith("IASI_xxx_1C") || 
                 filename.startsWith("AMSA_xxx_1B")) {
           String yyyymmdd = filename.substring(16,24);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");

           String hhmm = filename.substring(24,28);
           String yyyyMMddHHmm = yyyymmdd.concat(hhmm);
           datetime = sdf.parse(yyyyMMddHHmm);
        }
        else if (filename.startsWith("FY3C_MERSI")) {
           String yyyymmdd = filename.substring(19, 27);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
           String hhmm = filename.substring(28,32);
           datetime = sdf.parse(yyyymmdd.concat(hhmm));
        }
        else if (filename.contains("MERSI")) {
           String yyyyddd = filename.substring(2,9);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDDHHmm");
           String HHmm = filename.substring(9,13);
           datetime = sdf.parse(yyyyddd.concat(HHmm));
        }
        else if (filename.startsWith("NPR-MIRS")) {
           String yyyyddd = filename.substring(23,31);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDDHHmm");
           String HHmm = filename.substring(31,35);
           datetime = sdf.parse(yyyyddd.concat(HHmm));
        }
        else if (filename.contains("IASI_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("IASI_C_EUMP");
            idx += 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("iasil1c") && filename.endsWith(".h5")) {
            int idx = 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            idx += 8;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }        
        else if (filename.contains("AVHR_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("AVHR_C_EUMP");
            idx += 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.contains("HIRS_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("HIRS_C_EUMP");
            idx += 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("hirsl1c") && filename.endsWith(".h5")) {
            int idx = 12;
            String yyyyMMdd = filename.substring(idx, idx+8);
            idx += 8;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }      
        else if (filename.startsWith("amsual1c") && filename.endsWith(".h5")) {
            int idx = 13;
            String yyyyMMdd = filename.substring(idx, idx+8);
            idx += 8;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("mhsl1c") && filename.endsWith(".h5")) {
            int idx = 11;
            String yyyyMMdd = filename.substring(idx, idx+8);
            idx += 8;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.contains("AMSUA_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("AMSUA_C_EUMP");
            idx += 13;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.contains("MHS_C") && filename.endsWith(".nc")) {
            int idx = filename.lastIndexOf("MHS_C_EUMP");
            idx += 11;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 8;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.contains("atm_prof_rtv") && filename.endsWith(".h5")) {
            int idx = 6;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 10;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("viirs_l1b") || filename.startsWith("VL1B")) {
            int idx = filename.indexOf("_snpp_d");
            idx += 7;
            String str = filename.substring(idx, idx+14);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmm");
            datetime = sdf.parse(str);
        }
        else if (filename.startsWith("geocatL2_OT.Aqua.")) {
            int idx = 17;
            String str = filename.substring(idx,idx+12);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD'.'HHmm");
            datetime = sdf.parse(str);
        }
        else if (filename.startsWith("geocatL2_OT.Terra.")) {
            int idx = 18;
            String str = filename.substring(idx,idx+12);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD'.'HHmm");
            datetime = sdf.parse(str);           
        }
        else if (filename.startsWith("geocatL1.HIMAWARI-8") || filename.startsWith("geocatL2.HIMAWARI-8")) {
            int idx = 20;
            String str = filename.substring(idx, idx+12);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD'.'HHmm");
            datetime = sdf.parse(str);                       
        }
        else if (filename.startsWith("SEADAS_modis")) {
            int idx = 14;
            String str = filename.substring(idx, idx+14);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmm");
            datetime = sdf.parse(str);
        }
        else if (filename.contains("SST") && filename.contains("VIIRS_NPP-ACSPO")) {
           String str = filename.substring(0, 12);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
           datetime = sdf.parse(str);
        }
     }
     catch (Exception e) {
        return -1;
     }
     
     long time = 0;
     if (datetime != null) {
        time = datetime.getTime();
     }
     return time;
  }
  
  public static String makeDateTimeStamp(Date date) {
     return makeDateTimeStamp(date.getTime());
  }
  
  public static String makeDateTimeStamp(long time) {
     String timeStr = (new Timestamp(time)).toString();
     timeStr = timeStr.substring(0,16);
     return timeStr;
  }
  
  public static ArrayList<File> getTimeSortedFileList(List<File> fileList) throws Exception {
     return getTimeSortedFileList((File[]) fileList.toArray(new File[0]));
  }
    
  public static ArrayList<File> getTimeSortedFileList(File[] fileList) throws Exception {
     int numFiles = fileList.length;
     double[] times = new double[numFiles];
     for (int k=0; k<numFiles; k++) {
        File file = fileList[k];
        String name = file.getName();
        times[k] = (double) DataSource.getSecondsSinceEpoch(name);
     }

     int[] indexes = QuickSort.sort(times);

     ArrayList<File> sortedList = new ArrayList<File>();
     for (int k=0; k<numFiles; k++) {
        sortedList.add(fileList[indexes[k]]);
     } 
      
     return sortedList;
  }
  
  public static ArrayList<String> getTimeSortedFilenameList(List<File> fileList) throws Exception {
     return getTimeSortedFilenameList((File[]) fileList.toArray(new File[0]));
  }
  
  public static ArrayList<String> getTimeSortedFilenameList(File[] fileList) throws Exception {

      int numFiles = fileList.length;
      String[] filenames = new String[numFiles];
      double[] times = new double[numFiles];
      for (int k=0; k<numFiles; k++) {
         File file = fileList[k];
         String name = file.getName();
         times[k] = (double) DataSource.getSecondsSinceEpoch(name);
         filenames[k] = file.getAbsolutePath();
      }

      int[] indexes = QuickSort.sort(times);

      ArrayList<String> sortedList = new ArrayList<String>();
      for (int k=0; k<numFiles; k++) {
         sortedList.add(filenames[indexes[k]]);
      }

      return sortedList;
  }
  
  public static DataChoice getDataChoiceByName(ArrayList<DataChoice> choices, String name) {
     for (int i=0; i<choices.size(); i++) {
        DataChoice choice = choices.get(i);
        if (choice.getName().equals(name)) {
           return choice;
        }
     }
     return null;
  }
  
  public float getNadirResolution(DataChoice choice) throws Exception {
     return 5000f;
  }

  public int getDefaultChoice() {
     return 0;
  }
  
  public List getDataChoices() {
     return null;
  }
  
  public String getDateTimeStamp() {
     return null;
  }
  
  public String getDescription() {
     return null;
  }
  
  public String getDescription(DataChoice choice) {
     return null;
  }
  
  public String getSensorName(DataChoice choice) {
     return null;
  }
  
  public boolean getDoReproject(DataChoice choice) {
     return true;
  }
  
  public boolean getReduceBowtie(DataChoice choice) {
     return false;
  }
  
  public ColorTable getDefaultColorTable(DataChoice choice) {
     ColorTable clrTbl = Hydra.grayTable;
     String name = choice.getName();
     
     if (name.contains("Emissive")) {
        clrTbl = Hydra.invGrayTable;
     }
     else if (name.contains("Cloud_Mask")) {
        float[][] palette = new float[][] {{0.9f,0.9f,0.0f,0.0f},
                                           {0.9f,0.0f,0.9f,0.9f},
                                           {0.9f,0.0f,0.9f,0.0f},
                                           {0.97f,0.97f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("Cloud_Phase_Infrared") || name.contains("Cloud_Phase")) {
        float[][] palette = new float[][] {{0.0f,0.0f,1.0f,0.8f,0.0f,0.0f,0.0f},
                                           {0.0f,0.0f,0.5f,0.8f,0.8f,0.8f,0.8f},
                                           {0.0f,0.8f,0.5f,0.0f,0.0f,0.0f,0.0f},
                                           {0.00f,0.97f,0.98f,0.98f,0.98f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("fire_mask")) {
        float[][] palette = new float[][] {{0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f,1.0f,1.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f,0.58f,0.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.98f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("Cloud_Type")) {
        float[][] palette = new float[][] {{0f, 10f, 33f,   41f, 10f,  250f, 246f, 252f, 143f, 244f},
                                           {0f, 35f, 189f, 249f, 102f, 247f, 13f,  136f, 148f, 40f},
                                           {0f, 241f, 249f, 46f, 13f,   54f,  27f, 37f, 144f, 250f},
                       {0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f}};
        
        for (int i=0; i<palette[0].length; i++) palette[0][i] /= 256;
        for (int i=0; i<palette[1].length; i++) palette[1][i] /= 256;
        for (int i=0; i<palette[2].length; i++) palette[2][i] /= 256;
        
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     else if (name.contains("Cloud_Top_Temperature") || name.contains("Cld_Top_Temp")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("Cloud_Top_Pressure") || name.contains("Cld_Top_Pres")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("Cld_Top_Hght")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("Sea_Surface_Temperature") || name.contains("SST")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("radiances")) {
        clrTbl = Hydra.invGrayTable;
     }
     else if (name.contains("RainRate")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("TPW")) {
        clrTbl = Hydra.rainbow;
     }
     else if (name.contains("brightness_temp")) {
        clrTbl = Hydra.invGrayTable;
     }
     else if (name.contains("albedo")) {
        clrTbl = Hydra.grayTable;
     }
     
     return clrTbl;
  }
  
  public boolean isAtmRetrieval() {
     return false;
  }
  
  public boolean isSounder() {
     return false;
  }
  
  public boolean isImager() {
     return true;
  }
  
  public Data getData(DataChoice dataChoice, DataSelection dataSelection) throws VisADException, RemoteException {
     return null;
  }
  
  public Data getData(DataChoice dataChoice) throws VisADException, RemoteException {
     return getData(dataChoice, null);
  }
  
  public MultiSpectralData getMultiSpectralData(DataChoice dataChoice) {
     return null;
  }
  
  public MultiSpectralData[] getMultiSpectralData() {
     return null;
  }
  
  /* ???
  public Data getData(DataView view, DataChoice dataChoice, DataSelection dataSelection) {
     
  }
  */
}
