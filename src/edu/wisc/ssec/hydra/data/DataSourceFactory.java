package edu.wisc.ssec.hydra.data;

import java.io.File;
import java.util.ArrayList;
import ucar.unidata.util.Misc;

public class DataSourceFactory {
   
   public DataSourceFactory() {
      
   }
   
   public DataSource createDataSource(File[] files) throws Exception {
        DataSource dataSource = null;
      
        String[] fileNames = new String[files.length];
        for (int k=0; k<fileNames.length; k++) {
          File f = files[k];
          fileNames[k] = f.getPath();
        }
        String name = files[0].getName();
      
        if (name.startsWith("NPR-MIRS")) {
           dataSource = new GenericDataSource(files);
        }
        else if (name.contains("ABI-L2-CMIP") || name.startsWith("HS_H08") || name.contains("HIMAWARI8-AHI")) {
           dataSource = new GEOSDataSource(files[0]);
        }
        else if (files[0].getName().startsWith("SV") || files[0].getName().startsWith("GM") ||
            files[0].getName().startsWith("SV") || files[0].getName().startsWith("GI") ||
            files[0].getName().startsWith("SV") || files[0].getName().startsWith("GD")) {
          dataSource = new VIIRSDataSource(files);
        }
        else if (files[0].getName().startsWith("SCRIS") || files[0].getName().startsWith("GCRSO")) {
          ArrayList<File> dataList = new <File>ArrayList();
          for (int i=0; i<files.length; i++) {
             String fname = files[i].getName();
             if (fname.startsWith("SCRIS") || fname.startsWith("GCRSO-SCRIS_npp")) {
                dataList.add(files[i]);
             }
          }
          ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(dataList);
          for (int i=0; i<sortedList.size(); i++) {
             files[i] = new File(sortedList.get(i));
          }
          dataSource = new NOAA_SNPP_DataSource(files);
        }
        else if (files[0].getName().startsWith("SATMS_npp") || files[0].getName().startsWith("GATMO")) {
          ArrayList<File> dataList = new <File>ArrayList();
          for (int i=0; i<files.length; i++) {
             String fname = files[i].getName();
             if (fname.startsWith("SATMS_npp") || fname.startsWith("GATMO-SATMS_npp")) {
                dataList.add(files[i]);
             }
          }
          ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(dataList);
          for (int i=0; i<sortedList.size(); i++) {
             files[i] = new File(sortedList.get(i));
          }
          dataSource = new NOAA_SNPP_DataSource(files);
        }
        else if (files[0].getName().startsWith("AIRS") && files[0].getName().contains("atm_prof_rtv") && files[0].getName().endsWith(".hdf")) {
          dataSource = new AIRSv1_SoundingDataSource(files);
        }
        else if (files[0].getName().startsWith("AIRS") && files[0].getName().contains("atm_prof_rtv") && files[0].getName().endsWith(".h5")) {
          dataSource = new AIRSv2_SoundingDataSource(files);
        }
        else if (files[0].getName().startsWith("IASI") && files[0].getName().contains("atm_prof_rtv") && files[0].getName().endsWith(".h5")) {
          dataSource = new IASI_SoundingDataSource(files);
        }
        else if (files[0].getName().startsWith("CrIS") && files[0].getName().contains("atm_prof_rtv")) {
          dataSource = new CrIS_SoundingDataSource(files);
        }
        else if (name.startsWith("MOD06") || name.startsWith("MYD06") || name.contains("mod06") ||
            name.startsWith("MOD04") || name.startsWith("MYD04") || name.contains("mod04") ||
            name.startsWith("MOD35") || name.startsWith("MYD35") || name.contains("mod35") ||
            name.contains("mod14") || name.startsWith("MOD14") || name.startsWith("MYD14") ||
            name.contains("mod28") || name.startsWith("MOD28") || name.startsWith("MYD28") ||
            name.startsWith("geocatL2_OT") || name.contains("seadas") || name.contains("SEADAS_npp") || name.contains("SEADAS_modis")) 
        {
            dataSource = new MultiDimensionDataSource(Misc.newList(fileNames));
        }
        else if (files[0].getName().startsWith("viirs_l1b-m") || files[0].getName().startsWith("viirs_geo-m")) {
           dataSource = new SIPS_VIIRS_SVM(files);
        }
        else if (files[0].getName().startsWith("VL1BM")) {
           dataSource = new SIPS_VIIRS_SVM(files);
        }        
        else if (files[0].getName().startsWith("viirs_l1b-i") || files[0].getName().startsWith("viirs_geo-i")) {
           dataSource = new SIPS_VIIRS_SVI(files);
        } 
        else if (files[0].getName().startsWith("VL1BI")) {
           dataSource = new SIPS_VIIRS_SVI(files);
        }  
        else if (files[0].getName().startsWith("VL1BD")) {
           dataSource = new SIPS_VIIRS_DNB(files);
        }                
        else {
          ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(Misc.newList(files));
          dataSource = new MultiSpectralDataSource(sortedList);
        }
      
        return dataSource;      
   }
   
   public DataSource createDataSource(File dir, Class ds) throws Exception {
      DataSource dataSource = (DataSource) ds.getConstructor(new Class[] {File.class}).newInstance(dir);
      return dataSource;
   }
   
}
