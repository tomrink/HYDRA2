package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.hydra.Hydra;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import java.io.File;
import edu.wisc.ssec.adapter.MultiSpectralDataSource;
import edu.wisc.ssec.adapter.MultiDimensionDataSource;
import ucar.unidata.util.ColorTable;
import visad.VisADException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.sql.Timestamp;
import java.util.List;

import visad.*;

public class DataSource {

   public static DataSourceImpl createDataSource(File[] files) {
      String[] fileNames = new String[files.length];
      for (int k=0; k<fileNames.length; k++) {
        File f = files[k];
        String fileNameAbsolute = f.getParent() + File.separatorChar + f.getName();
        fileNames[k] = fileNameAbsolute;
      }
      String name = files[0].getName();
      try {
        if (name.startsWith("NPR-MIRS")) {
           GenericDataSource dataSource = new GenericDataSource(files);
           return dataSource;
        }
        if (name.contains("ABI-L2-CMIP")) {
           GEOSDataSource dataSource = new GEOSDataSource(fileNames[0]);
           return dataSource;
        }
        if (files[0].getName().startsWith("SV") || files[0].getName().startsWith("GM") ||
            files[0].getName().startsWith("SV") || files[0].getName().startsWith("GI") ||
            files[0].getName().startsWith("SV") || files[0].getName().startsWith("GD")) {
          VIIRSDataSource dataSource = new VIIRSDataSource(files);
          return dataSource;
        }
        if (files[0].getName().startsWith("SCRIS") || files[0].getName().startsWith("GCRSO")) {
          ArrayList<File> dataList = new <File>ArrayList();
          for (int i=0; i<files.length; i++) {
             String fname = files[i].getName();
             if (fname.startsWith("SCRIS")) {
                dataList.add(files[i]);
             }
          }
          ArrayList<String> sortedList = null;
          try {
             sortedList = DataSource.getTimeSortedFilenameList(dataList, "Suomi");
             
          } catch (Exception e) {
              e.printStackTrace();
          }
          for (int i=0; i<sortedList.size(); i++) {
             files[i] = new File(sortedList.get(i));
          }
          try {
             NOAA_SNPP_DataSource dataSource = new NOAA_SNPP_DataSource(files);
             return dataSource;
          } catch (Exception exc) {
             exc.printStackTrace();
          }          
        }
        if (files[0].getName().startsWith("SATMS_npp") || files[0].getName().startsWith("GATMO")) {
          ArrayList<File> dataList = new <File>ArrayList();
          for (int i=0; i<files.length; i++) {
             String fname = files[i].getName();
             if (fname.startsWith("SATMS_npp") || fname.startsWith("GATMO-SATMS_npp")) {
                dataList.add(files[i]);
             }
          }
          ArrayList<String> sortedList = null;
          try {
              sortedList = DataSource.getTimeSortedFilenameList(dataList, "Suomi");
          } catch (Exception e) {
              e.printStackTrace();
          }
          for (int i=0; i<sortedList.size(); i++) {
             files[i] = new File(sortedList.get(i));
          }
          try {
             NOAA_SNPP_DataSource dataSource = new NOAA_SNPP_DataSource(files);
             return dataSource;
          } catch (Exception exc) {
             exc.printStackTrace();
          }
        }
        if (files[0].getName().startsWith("AIRS") && files[0].getName().contains("atm_prof_rtv") && files[0].getName().endsWith(".hdf")) {
          AtmSoundingDataSource dataSource = new AIRSv1_SoundingDataSource(files);
          return dataSource;
        }
        if (files[0].getName().startsWith("AIRS") && files[0].getName().contains("atm_prof_rtv") && files[0].getName().endsWith(".h5")) {
          AtmSoundingDataSource dataSource = new AIRSv2_SoundingDataSource(files);
          return dataSource;
        }
        if (files[0].getName().startsWith("IASI") && files[0].getName().contains("atm_prof_rtv") && files[0].getName().endsWith(".h5")) {
          AtmSoundingDataSource dataSource = new IASI_SoundingDataSource(files);
          return dataSource;
        }
        if (files[0].getName().startsWith("CrIS") && files[0].getName().contains("atm_prof_rtv")) {
          CrIS_SoundingDataSource dataSource = new CrIS_SoundingDataSource(files);
          return dataSource;
        }
        if (name.startsWith("MOD06") || name.startsWith("MYD06") || name.contains("mod06") ||
            name.startsWith("MOD04") || name.startsWith("MYD04") || name.contains("mod04") ||
            name.startsWith("MOD35") || name.startsWith("MYD35") || name.contains("mod35") ||
            name.contains("mod14") || name.startsWith("MOD14") || name.startsWith("MYD14") ||
            name.contains("mod28") || name.startsWith("MOD28") || name.startsWith("MYD28") ||
            name.startsWith("geocatL2_OT") || name.contains("seadas") || name.contains("SEADAS_npp") || name.contains("SEADAS_modis")) 
        {
            MultiDimensionDataSource dataSource = new MultiDimensionDataSource(new DataSourceDescriptor(), Misc.newList(fileNames), null);
            dataSource.doMakeDataChoices();
            return dataSource;
        }
        if (files[0].getName().startsWith("viirs_l1b-m") || files[0].getName().startsWith("viirs_geo-m")) {
           SIPS_VIIRS_SVM dataSource = new SIPS_VIIRS_SVM(files);
           return dataSource;
        }
        if (files[0].getName().startsWith("VL1BM")) {
           SIPS_VIIRS_SVM dataSource = new SIPS_VIIRS_SVM(files);
           return dataSource;
        }        
        if (files[0].getName().startsWith("viirs_l1b-i") || files[0].getName().startsWith("viirs_geo-i")) {
           SIPS_VIIRS_SVI dataSource = new SIPS_VIIRS_SVI(files);
           return dataSource;
        } 
        if (files[0].getName().startsWith("VL1BI")) {
           SIPS_VIIRS_SVI dataSource = new SIPS_VIIRS_SVI(files);
           return dataSource;
        }  
        if (files[0].getName().startsWith("VL1BD")) {
           SIPS_VIIRS_DNB dataSource = new SIPS_VIIRS_DNB(files);
           return dataSource;
        }                
        else {
          ArrayList<String> sortedList = null;
          try {
              sortedList = DataSource.getTimeSortedFilenameList(Misc.newList(files), null);
          } catch (Exception e) {
              e.printStackTrace();
          }
          MultiSpectralDataSource dataSource = new MultiSpectralDataSource(new DataSourceDescriptor(), sortedList, null);
          dataSource.doMakeDataChoices();
          return dataSource;
        }
      } catch (VisADException e) {
          e.printStackTrace();
      }
      return null;
   }


   public static DataSourceImpl createDataSource(File dir, Class ds) {
     DataSourceImpl dataSource = null;
     try {
        dataSource = (DataSourceImpl) ds.getConstructor(new Class[] {File.class}).newInstance(dir);
     }
     catch (Exception e) {
        e.printStackTrace();
     }
     return dataSource;
   }


   public static float getNadirResolution(DataSourceImpl datasource, DataChoice choice) throws Exception {
     if (datasource instanceof VIIRSDataSource) {
       return ((VIIRSDataSource)datasource).getNadirResolution(choice);
     }
     else if (datasource instanceof SIPS_VIIRS_DataSource) {
        return ((SIPS_VIIRS_DataSource)datasource).getNadirResolution();
     }
     else if (datasource instanceof AHIDirectory) {
       return ((AHIDirectory)datasource).getNadirResolution(choice);        
     }
     else if (datasource instanceof MultiSpectralDataSource) {
       //datasource.getNadirResolution() TODO support this in MultiSpectralDataSource
       File file = new File(((MultiSpectralDataSource)datasource).getDatasetName());
       return getNadirResolution(file);
     }
     else if (datasource instanceof MultiDimensionDataSource) {
       File file = new File(((MultiDimensionDataSource)datasource).getDatasetName());
       String name = file.getName();
       if (name.startsWith("MOD06") || name.startsWith("MYD06") || name.contains("mod06")) {
          String pname = choice.getName();
          if (pname.equals("Cloud_Optical_Thickness") || pname.equals("Cloud_Effective_Radius") || pname.equals("Cloud_Water_Path")) {
             return 1020f;
          }
       }
       else if (name.contains("mod14") || name.contains("mod28") || name.contains("mod35")) {
           return 1020f;
       }
       else if (name.contains("seadas") || name.startsWith("SEADAS_modis")) {
          return 1020f;
       }
       else if(name.contains("SEADAS_npp")) {
          return 770f;
       }
       else if (name.contains("mod04")) {
          return 10100f;
       }
       else if (name.contains("mod04_3k")) {
          return 3060f;
       }
       else if (name.startsWith("geocatL2_OT")) {
          return 1020f;
       }
           
       return 5000f;
     }
     else {
       throw new Exception("Datasource not identified");
     }
   }

   public static int getReprojectionStrategy(DataSourceImpl datasource, DataChoice choice) {
     if (datasource instanceof VIIRSDataSource) {
       return 0;
     }
     else {
       return 0;
     }
   }

   public static boolean getDoReproject(DataSourceImpl datasource, DataChoice choice) {
      if (datasource instanceof GEOSDataSource || 
          datasource instanceof AHIDirectory)
      {
         return false;
      }
      String name = choice.getName();

      if (name.equals("Sea_Surface_Temperature")) {
         return true;
      }
      if (name.equals("Cloud_Mask")) {
         return true;   
      }
      if (name.equals("Cloud_Phase_Infrared")) {
         return true;
      }
      if (name.equals("fire_mask")) {
         return true;
      }
      if (name.equals("Cloud_Top_Temperature")) {
         return true;
      }
      if (name.equals("Cloud_Top_Pressure")) {
         return true;
      }
      if (name.equals("Cloud_Fraction")) {
         return true;
      }
      if (name.equals("Optical_Depth_Land_And_Ocean")) {
         return true;
      }

      return true;
   }
   
   public static boolean getDoFilter(DataChoice choice) {
       String name = choice.getName();
       
      if (name.equals("Cloud_Mask")) {
         return false;   
      }
      if (name.equals("Cloud_Phase_Infrared")) {
         return false;
      }
      if (name.equals("fire_mask")) {
         return false;
      }
       
      return true;
   }
   
   public static boolean getOverlayAsMask(DataChoice choice) {
      String name = choice.getName();
      
      if (name.equals("fire_mask")) {
         return true;
      }
      else if (name.equals("OT_grid_mag")) {
         return true;
      }
      return false;
   }

   public static boolean getReduceBowtie(DataSourceImpl datasource, DataChoice choice) {
      if (datasource instanceof VIIRSDataSource || datasource instanceof SIPS_VIIRS_DataSource) {
         return false;
      }
      else if (datasource instanceof MultiDimensionDataSource) {
         String str = ((MultiDimensionDataSource)datasource).getDatasetName();
         if (str.contains("_npp_")) {
            return false;
         }
         return true;
      }
      else {
         return true;
      }
   }

   public static String getSensorName(DataSourceImpl datasource, DataChoice choice) {
       if (datasource instanceof MultiSpectralDataSource) {
          File file = new File(((MultiSpectralDataSource)datasource).getDatasetName());
          return getSensorNameFromFilename(file);
       }
       else if (datasource instanceof MultiDimensionDataSource) {
          File file = new File(((MultiDimensionDataSource)datasource).getDatasetName());
          return getSensorNameFromFilename(file);
       }
       return null;
   }

   public static String getSensorNameFromFilename(File file) {
     String name = file.getName();

     if (name.startsWith("MYD021KM")) return new String("MODIS_1KM");
     if (name.startsWith("MOD021KM")) return new String("MODIS_1KM");
     if (name.startsWith("MYD02HKM")) return new String("MODIS_HKM");
     if (name.startsWith("MOD02HKM")) return new String("MODIS_HKM");
     if (name.startsWith("MYD02QKM")) return new String("MODIS_QKM");
     if (name.startsWith("MOD02QKM")) return new String("MODIS_QKM");
     if (name.startsWith("a1.") || name.startsWith("t1.")) {
        if (name.contains("1000m")) return new String("MODIS_1KM");
        if (name.contains("500m")) return new String("MODIS_HKM");
        if (name.contains("250m")) return new String("MODIS_QKM");
     }
     if (name.contains("MERSI_0250M_L1B")) return new String("MERSI_QKM");
     if (name.contains("MERSI_1000M_L1B")) return new String("MERSI_1KM");
     if (name.startsWith("FY3C_MERSI") && name.contains("1000M")) return new String("MERSI_1KM");
     return null;
   }

   public static float getNadirResolution(File file) {
     String name = file.getName();

     if (name.startsWith("SVM")) return 770.0f;
     if (name.startsWith("SVDNB")) return 770.0f;
     if (name.startsWith("SVI")) return 390.0f;
     if (name.startsWith("MYD021KM")) return 1020.0f;
     if (name.startsWith("MOD021KM")) return 1020.0f;
     if (name.startsWith("MYD02HKM")) return 510.0f;
     if (name.startsWith("MOD02HKM")) return 510.0f;
     if (name.startsWith("MYD02QKM")) return 260.0f;
     if (name.startsWith("MOD02QKM")) return 260.0f;
     if (name.startsWith("a1.") || name.startsWith("t1.")) {
        if (name.contains("1000m")) return 1020.0f;
        if (name.contains("500m")) return 510.0f;
        if (name.contains("250m")) return 260.0f;
     }
     if (name.contains("MERSI_0250M_L1B")) return 260.0f;
     //return 1000.0f;
     return 770.0f;
   }

   public static ColorTable getDefaultColorTable(DataSourceImpl datasource, DataChoice choice) {

     ColorTable clrTbl = Hydra.grayTable;
     String name = choice.getName();

     if (datasource instanceof MultiSpectralDataSource) {
       if (name.contains("Emissive")) {
         clrTbl = Hydra.invGrayTable;
       }
     }
     else if (datasource instanceof VIIRSDataSource || datasource instanceof SIPS_VIIRS_DataSource) {
       name = choice.getName();
       if ( name.equals("I4") || name.equals("I04") || name.equals("M12") || name.equals("M13") || name.equals("M14") || name.equals("M15") || name.equals("I5") || name.equals("I05") || name.equals("M16") ) {
         clrTbl = Hydra.invGrayTable;
       }
     }
     else if (datasource instanceof AHIDirectory) {
       name = choice.getName();
       if ( name.equals("B07") || name.equals("B08") || name.equals("B09") || name.equals("B10") || name.equals("B11") || name.equals("B12") || name.equals("B13") || name.equals("B14") || name.equals("B15") || name.equals("B16") ) {
         clrTbl = Hydra.invGrayTable;
       }
     }
     if (name.equals("Cloud_Mask")) {
        float[][] palette = new float[][] {{0.9f,0.9f,0.0f,0.0f},
                                           {0.9f,0.0f,0.9f,0.9f},
                                           {0.9f,0.0f,0.9f,0.0f},
                                           {0.97f,0.97f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     if (name.equals("Cloud_Phase_Infrared")) {
        float[][] palette = new float[][] {{0.0f,0.0f,1.0f,0.8f,0.0f,0.0f,0.0f},
                                           {0.0f,0.0f,0.5f,0.8f,0.8f,0.8f,0.8f},
                                           {0.0f,0.8f,0.5f,0.0f,0.0f,0.0f,0.0f},
                                           {0.00f,0.97f,0.98f,0.98f,0.98f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     if (name.equals("fire_mask")) {
        float[][] palette = new float[][] {{0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f,1.0f,1.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,1.0f,0.58f,0.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f},
                                           {0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.0f,0.98f,0.98f,0.98f}};
        clrTbl = new ColorTable();
        clrTbl.setTable(palette);
     }
     if (name.equals("Cloud_Top_Temperature")) {
        clrTbl = Hydra.rainbow;
     }
     if (name.equals("Cloud_Top_Pressure")) {
        clrTbl = Hydra.rainbow;
     }
     if (name.equals("Sea_Surface_Temperature")) {
        clrTbl = Hydra.rainbow;
     }
     if (name.equals("radiances")) {
        clrTbl = Hydra.invGrayTable;
     }
     if (name.equals("RainRate")) {
        clrTbl = Hydra.rainbow;
     }
     if (name.equals("TPW")) {
        clrTbl = Hydra.rainbow;
     }
     if (name.equals("brightness_temp")) {
        clrTbl = Hydra.invGrayTable;
     }
     if (name.equals("albedo")) {
        clrTbl = Hydra.grayTable;
     }


     return clrTbl;
   }

   public static Range getDefaultColorRange(DataSourceImpl datasource, DataChoice choice) {
      String name = choice.getName();

      Range rng = null;

      if (name.equals("Cloud_Mask")) {
         rng = new Range(0f-0.5f, 3f+0.5f);
      }
      if (name.equals("Cloud_Phase_Infrared")) {
         rng = new Range(0f-0.5f, 6f+0.5f);
      }
      if (name.equals("fire_mask")) {
         rng = new Range(0f-0.5f, 9f+0.5f);
      }

      return rng; 
   }


   // TODO: Replace these at some point

   public static String getDescription(DataSourceImpl dataSource) {
     if (dataSource instanceof VIIRSDataSource) {
       return ((VIIRSDataSource)dataSource).getDescription();
     }
     else if (dataSource instanceof SIPS_VIIRS_DataSource) {
        return ((SIPS_VIIRS_DataSource)dataSource).getDescription();
     }
     else if (dataSource instanceof AHIDirectory) {
       return ((AHIDirectory)dataSource).getDescription();
     }
     return null;
   }

   public static String getDescription(DataSourceImpl dataSource, DataChoice dataChoice) {
     if (dataSource instanceof VIIRSDataSource) {
       return ((VIIRSDataSource)dataSource).getDescription(dataChoice);
     }
     else if (dataSource instanceof SIPS_VIIRS_DataSource) {
        return ((SIPS_VIIRS_DataSource)dataSource).getDescription(dataChoice);
     }
     return null;
   }


   public static String getDateTimeStamp(DataSourceImpl dataSource) {
     if (dataSource instanceof VIIRSDataSource) {
       return ((VIIRSDataSource)dataSource).getDateTimeStamp();
     }
     else if (dataSource instanceof SIPS_VIIRS_DataSource) {
        return ((SIPS_VIIRS_DataSource)dataSource).getDateTimeStamp();
     }
     else if (dataSource instanceof AHIDirectory) {
       return ((AHIDirectory)dataSource).getDateTimeStamp();       
     }
     return null;
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
     else if (filename.startsWith("SCRIS")) {
       desc = "CrIS SDR";
     }
     else if (filename.startsWith("SATMS_npp")) {
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
     return desc;
   }

   public static long getMillisecondsSinceTheEpoch(String filename, String platform) throws Exception {
      return 0;
   }

   public static String getDateTimeStampFromFilename(String filename, String platform) {
      long millis = getSecondsSinceEpoch(filename, platform);
      return makeDateTimeStamp(millis);
   }
   
   public static long getSecondsSinceEpoch(String filename, String platform) {
     long millis = 0;
     Date datetime = null;
     
     try {
        if (platform != null) { 
           if (platform.equals("Suomi") || filename.contains("_npp_d")) {
              int idx = filename.indexOf("_npp_d");
              idx += 6;
              String str = filename.substring(idx, idx+16);
              SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmmss");
              datetime = sdf.parse(str);
           }
        }
        if (filename.startsWith("MOD") || filename.startsWith("MYD")) {
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            Date date = sdf.parse(yyyyMMdd);
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
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }      
        else if (filename.startsWith("amsual1c") && filename.endsWith(".h5")) {
            int idx = 13;
            String yyyyMMdd = filename.substring(idx, idx+8);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
            idx += 1;
            String HHmm = filename.substring(idx, idx+4);
            datetime = sdf.parse(yyyyMMdd.concat(HHmm));
        }
        else if (filename.startsWith("mhsl1c") && filename.endsWith(".h5")) {
            int idx = 11;
            String yyyyMMdd = filename.substring(idx, idx+8);
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
        else if (filename.startsWith("SEADAS_modis")) {
            int idx = 14;
            String str = filename.substring(idx, idx+14);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd'_t'HHmm");
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
  
  public static String getDateTimeStampFromDataSource(DataSourceImpl dataSource) {
     String dateTime = null;

     if (dataSource instanceof MultiSpectralDataSource) {
        dateTime = ((MultiSpectralDataSource)dataSource).getDateTime();
     }
     
     return dateTime;
  }

  public static String makeDateTimeStamp(Date date) {
     return makeDateTimeStamp(date.getTime());
  }
  
  public static String makeDateTimeStamp(long time) {
     String timeStr = (new Timestamp(time)).toString();
     timeStr = timeStr.substring(0,16);
     return timeStr;
  }
  
  public static ArrayList<File> getTimeSortedFileList(List<File> fileList, String platform) throws Exception {
     return getTimeSortedFileList((File[]) fileList.toArray(new File[0]), platform);
  }
    
  public static ArrayList<File> getTimeSortedFileList(File[] fileList, String platform) throws Exception {
     int numFiles = fileList.length;
     double[] times = new double[numFiles];
     for (int k=0; k<numFiles; k++) {
        File file = fileList[k];
        String name = file.getName();
        times[k] = (double) DataSource.getSecondsSinceEpoch(name, platform);
     }

     int[] indexes = QuickSort.sort(times);

     ArrayList<File> sortedList = new ArrayList<File>();
     for (int k=0; k<numFiles; k++) {
        sortedList.add(fileList[indexes[k]]);
     } 
      
     return sortedList;
  }
  
  public static ArrayList<String> getTimeSortedFilenameList(List<File> fileList, String platform) throws Exception {
     return getTimeSortedFilenameList((File[]) fileList.toArray(new File[0]), platform);
  }
  
  public static ArrayList<String> getTimeSortedFilenameList(File[] fileList, String platform) throws Exception {

      int numFiles = fileList.length;
      String[] filenames = new String[numFiles];
      double[] times = new double[numFiles];
      for (int k=0; k<numFiles; k++) {
         File file = fileList[k];
         String name = file.getName();
         times[k] = (double) DataSource.getSecondsSinceEpoch(name, platform);
         filenames[k] = file.getAbsolutePath();
      }

      int[] indexes = QuickSort.sort(times);

      ArrayList<String> sortedList = new ArrayList<String>();
      for (int k=0; k<numFiles; k++) {
         sortedList.add(filenames[indexes[k]]);
      }

      return sortedList;
   }

  public static int getDefaultChoice(DataSourceImpl dataSource, java.util.List list) {
     int idx = 0;
     Object[] choices = list.toArray();
     if (dataSource instanceof VIIRSDataSource) {
        for (int k=0; k < choices.length; k++) {
           if (((DataChoice)choices[k]).getName().contains("M15")) {
              idx = k;
              break;
           }
        }
     }
     return idx;
  }

}
