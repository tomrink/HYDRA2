package edu.wisc.ssec.hydra.data;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.GranuleAggregation;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.adapter.NetCDFFile;
import visad.VisADException;
import visad.Data;
import java.rmi.RemoteException;
import ucar.unidata.util.ColorTable;
import ucar.unidata.util.Misc;
import ucar.unidata.util.Range;
import visad.FlatField;
import visad.Linear2DSet;


public class CLAVRX_VIIRS_DataSource extends DataSource {

   String dateTimeStamp = null;

   String description = null;

   File[] files = null;

   ArrayList<SwathAdapter> swathAdapters;
   
   String[] bandNames = null;
   float[] centerWavelength = null;
   

   public CLAVRX_VIIRS_DataSource(File directory) throws Exception {
     this(directory.listFiles());
   }
   
   public CLAVRX_VIIRS_DataSource(File[] files) throws Exception {
      ArrayList<String> sortedList = DataSource.getTimeSortedFilenameList(Misc.newList(files));
      
      myDataChoices = new ArrayList();
      swathAdapters = new ArrayList();
      
      File file = files[0];
              
      String[] fileNames = new String[files.length];
      for (int k=0; k<sortedList.size(); k++) {
         fileNames[k] = sortedList.get(k);
      }
      String name = files[0].getName();
     
      dateTimeStamp = DataSource.getDateTimeStampFromFilename(name);
      
      try {
         init(fileNames);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   void init(String[] files) throws Exception {
      ArrayList<NetCDFFile> ncdfal = new ArrayList<NetCDFFile>();
      
      int numGrans = files.length;
      
      for (int k=0; k<files.length; k++) {
         ncdfal.add(new NetCDFFile(files[k]));
      }
      
      GranuleAggregation aggReader = new GranuleAggregation(ncdfal, "scan_lines_along_track_direction");
      
      String name = files[0];
      String[] strs = name.split("_");
      String prodStr = strs[0].substring(2,5);
      String sensorName = null;
      
      boolean unsigned = false;
      boolean unpack = true;
      boolean range_check_after_scaling = false;
      
      String[] arrayNames = new String[] {"cloud_type", "cloud_phase", "cloud_mask", "cld_press_acha", "cld_temp_acha", "cld_height_acha", "ndvi_sfc"};
      String[] rangeNames = new String[] {"Cloud_Type", "Cloud_Phase", "Cloud_Mask", "Cld_Top_Press", "Cld_Top_Temp", "Cld_Top_Hght", "NDVI"};
      
      for (int k=0; k<arrayNames.length; k++) {
      
         HashMap metadata = fillMetadataTable(
                 "pixel_elements_along_scan_direction",
                 "scan_lines_along_track_direction",
                 arrayNames[k],
                 rangeNames[k],
                 "pixel_elements_along_scan_direction",
                 "scan_lines_along_track_direction",
                 "longitude",
                 "latitude",
                 new String[] {"scan_lines_along_track_direction", "pixel_elements_along_scan_direction"},
                 new String[] {"scan_lines_along_track_direction", "pixel_elements_along_scan_direction"},
                 new String[] {"scan_lines_along_track_direction", "pixel_elements_along_scan_direction"},
                 unsigned, unpack, range_check_after_scaling
         );
      
         metadata.put(MultiDimensionAdapter.scale_name, "scale_factor");
         metadata.put(MultiDimensionAdapter.offset_name, "add_offset");
         metadata.put(MultiDimensionAdapter.fill_value_name, "_FillValue");
         metadata.put(SwathAdapter.geo_scale_name, "scale_factor");
         metadata.put(SwathAdapter.geo_fillValue_name, "_FillValue");
            
         SwathAdapter adapter = new SwathAdapter(aggReader, metadata);
         adapter.setDefaultStride(10);
      
         setDataChoice(adapter, k, rangeNames[k]);
      }
   }

   public HashMap fillMetadataTable(String xtrack, String track, 
                                    String array, String range, String geoXtrack, String geoTrack,
                                    String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                    boolean unsigned, boolean unpack, boolean range_check_after_scaling) {
      
      HashMap metadata = SwathAdapter.getEmptyMetadataTable();
      
      metadata.put(SwathAdapter.xtrack_name, xtrack);
      metadata.put(SwathAdapter.track_name, track);
      metadata.put(SwathAdapter.geo_xtrack_name, geoXtrack);
      metadata.put(SwathAdapter.geo_track_name, geoTrack);
      metadata.put(SwathAdapter.array_name, array);
      metadata.put(SwathAdapter.range_name, range);
      metadata.put(SwathAdapter.lon_array_name, lonArray);
      metadata.put(SwathAdapter.lat_array_name, latArray);
      if (unsigned) {
         metadata.put("unsigned", "true");
      }
      if (unpack) {
         metadata.put("unpack", "true");
      }
      if (range_check_after_scaling) {
         metadata.put("range_check_after_scaling", "true");
      }
      if (lonArrayDims != null) {
         metadata.put(SwathAdapter.lon_array_dimension_names, lonArrayDims);
      }
      if (latArrayDims != null) {
         metadata.put(SwathAdapter.lat_array_dimension_names, latArrayDims);
      }
      if (arrayDims != null) {
         metadata.put(SwathAdapter.array_dimension_names, arrayDims);     
      }
      
      return metadata;
   }
   
   
   public SwathAdapter getSwathAdapter(DataChoice dataChoice) {
      for (int k=0; k<myDataChoices.size(); k++) {
          if (myDataChoices.get(k).equals(dataChoice)) {
              return swathAdapters.get(k);
          }
      }
      return null;
   }
   
    void setDataChoice(SwathAdapter adapter, int idx, String name) {
       HashMap subset = adapter.getDefaultSubset();
       DataSelection dataSel = new MultiDimensionSubset(subset);
       DataChoice dataChoice = new DataChoice(this, name, null);
       dataChoice.setDataSelection(dataSel);
       myDataChoices.add(dataChoice);
       swathAdapters.add(adapter);    
    }  


   public Data getData(DataChoice dataChoice, DataSelection dataSelection)
       throws VisADException, RemoteException 
    {
      try {
         SwathAdapter adapter = getSwathAdapter(dataChoice);
         
         MultiDimensionSubset select = null;
         select = (MultiDimensionSubset) dataChoice.getDataSelection();
         
         HashMap subset = select.getSubset();
         
         Data data = adapter.getData(subset);
         Linear2DSet domSet = (Linear2DSet) ((FlatField)data).getDomainSet();
         for (int k=0; k<swathAdapters.size(); k++) {
            SwathAdapter sa = swathAdapters.get(k);
            if (adapter != sa) {
               sa.setDomainSet(domSet);
            }
         }
         return data;
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
    }
   
    public String getDateTimeStamp() {
       return dateTimeStamp;
    }
    
    public String getDescription() {
       return "CLAVRx";
    }
    
    public float getNadirResolution(DataChoice choice) throws Exception {
       return 780f;
    }    
    
    public Range getDefaultColorRange(DataChoice choice) {
       Range range;
       if (choice.getName().equals("Cloud_Mask")) {
          range = new Range(3f+0.5f, 0f-0.5f);
       }
       else if (choice.getName().equals("Cloud_Type")) {
          range = new Range(0f-0.5f, 12f+0.5f);
       }
       else if (choice.getName().equals("Cloud_Phase")) {
          range = new Range(0f-0.5f, 5f+0.5f);
       }
       else {
          range = super.getDefaultColorRange(choice);
       }
       return range;
    }    
    
    public ColorTable getDefaultColorTable(DataChoice choice) {
       ColorTable clrTbl = null;
       
       if (choice.getName().equals("Cloud_Type")) {
       
          float[][] palette = 
             new float[][] {{192f, 128f, 15f,  11f, 45f, 254f, 252f, 254f, 253f, 160f, 252f, 250f, 126f},
                            {192f, 128f, 127f, 36f, 254f, 192f, 13f, 253f, 164f, 42f, 252f, 40f, 15f},
                            {192f, 128f, 18f,  250f, 254f, 203f, 27f, 56f, 40f, 45f, 252f, 250f, 125f},
                          {0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f}};

           for (int i=0; i<palette[0].length; i++) palette[0][i] /= 256;
           for (int i=0; i<palette[1].length; i++) palette[1][i] /= 256;
           for (int i=0; i<palette[2].length; i++) palette[2][i] /= 256;

           clrTbl = new ColorTable();
           clrTbl.setTable(palette);
       }
       else if (choice.getName().equals("Cloud_Phase")) {
          float[][] palette =
             new float[][] {{128f, 11f, 45f, 127f, 254f, 0f},
                            {128f, 36f, 254f, 15f, 192f, 0f},
                            {128f, 250f, 254f, 126f, 203f, 0f},
                            {0.98f, 0.98f, 0.98f, 0.98f, 0.98f, 0.98f}};
           for (int i=0; i<palette[0].length; i++) palette[0][i] /= 256;
           for (int i=0; i<palette[1].length; i++) palette[1][i] /= 256;
           for (int i=0; i<palette[2].length; i++) palette[2][i] /= 256;

           clrTbl = new ColorTable();
           clrTbl.setTable(palette);          
       }
       else {
          clrTbl = super.getDefaultColorTable(choice);
       }
       
       return clrTbl;
    }
}
