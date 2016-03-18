package edu.wisc.ssec.hydra.data;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataChoice;
import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.GranuleAggregation;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.RangeProcessor;
import edu.wisc.ssec.adapter.AggregationRangeProcessor;
import visad.VisADException;
import visad.Data;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ucar.unidata.data.DirectDataChoice;


public class NOAA_VIIRS_DataSource extends DataSourceImpl {

   String dateTimeStamp = null;

   String description = null;

   File[] files = null;

   ArrayList<DataChoice> myDataChoices = new ArrayList<DataChoice>();
   ArrayList<SwathAdapter> swathAdapters = new ArrayList<SwathAdapter>();
   
   String[] bandNames = null;
   float[] centerWavelength = null;


   public NOAA_VIIRS_DataSource(File directory) throws Exception {
     this(directory.listFiles());
   }

   public NOAA_VIIRS_DataSource(File[] files) throws Exception {
      super(new DataSourceDescriptor(), "VIIRS", "VIIRS", new Hashtable());
      
      ArrayList<File> geoFileList = new ArrayList<File>();

      File file = files[0];
      String name = file.getName();
      File geoFile;
     
      File[] geoFiles = new File[files.length];
      
      String prefix = null;
      if (name.startsWith("SVM")) {
         prefix = "GMTCO_npp";
      }
      else if (name.startsWith("SVI")) {
         prefix = "GITCO_npp";
      }
      else if (name.startsWith("SVDNB")) {
         prefix = "GDNBO_npp";
      }
      else {
         throw new Exception("unknown VIIRS filename prefix: "+name);
      }
      
      File dir = new File(file.getParent());
      File[] list = dir.listFiles();
      for (int k=0; k<list.length; k++) {
         if (list[k].getName().startsWith(prefix)) {
            geoFileList.add(list[k]); 
         }
      }
      
      for (int i=0; i<files.length; i++) {
         name = files[i].getName();
         String[] strs = name.split("_");
         String regex = prefix+"_"+strs[2]+"_"+strs[3]+"_"+strs[4]+"_"+strs[5]+".*";
         Pattern pattern = Pattern.compile(regex);
         for (int k=0; k<geoFileList.size(); k++) {
            geoFile = geoFileList.get(k);
            Matcher matcher = pattern.matcher(geoFile.getName());
            if (matcher.find()) {
              geoFiles[i] = geoFile;
              break;
            }      
         }
      }
      
      dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName(), "Suomi");
      
      try {
         init(files, geoFiles);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   void init(File[] files, File[] geoFiles) throws Exception {
      ArrayList<NetCDFFile> ncdfal = new ArrayList<NetCDFFile>();
      ArrayList<NetCDFFile> ncdfalGeo = new ArrayList<NetCDFFile>();
      
      int numGrans = files.length;
      
      for (int k=0; k<files.length; k++) {
         ncdfal.add(new NetCDFFile(files[k].getAbsolutePath()));
         ncdfalGeo.add(new NetCDFFile(geoFiles[k].getAbsolutePath()));
      }
      
      GranuleAggregation aggReader = new GranuleAggregation(ncdfal, "Track");
      GranuleAggregation aggGeoReader = new GranuleAggregation(ncdfalGeo, "Track");
      
      String name = files[0].getName();
      String[] strs = name.split("_");
      String prodStr = strs[0].substring(2,5);
      
      String geoDatasetPath = null;
      if (prodStr.startsWith("M")) {
         geoDatasetPath = "All_Data/VIIRS-MOD-GEO-TC_All/";
      }
      else if (prodStr.startsWith("I")) {
         geoDatasetPath = "All_Data/VIIRS-IMG-GEO-TC_All/";
      }
      else if (prodStr.equals("DNB")) {
         geoDatasetPath = "All_Data/VIIRS-DNB-GEO_All/";
      }
      else {
         throw new Exception("unknown product: "+prodStr);
      }
      
      
      RangeProcessor[] rngProcessors = new RangeProcessor[numGrans];
      
      boolean unsigned = true;
      boolean unpack = true;
      boolean range_check_after_scaling = true;
      if (prodStr.equals("DNB") || prodStr.equals("M13")) {
         unsigned = false;
         unpack = false;
         range_check_after_scaling = false;
      }
      
      HashMap metadata = fillMetadataTable(
              "XTrack",
              "Track",
              getProductName(prodStr),
              prodStr,
              "XTrack",
              "Track",
              geoDatasetPath+"Longitude",
              geoDatasetPath+"Latitude",
              new String[] {"Track", "XTrack"},
              new String[] {"Track", "XTrack"},
              new String[] {"Track", "XTrack"},
              unsigned, unpack, range_check_after_scaling
      );
      
      float scale = 1f;
      float offset = 0f;
      for (int k=0; k<numGrans; k++) {
         NetCDFFile ncFile = ncdfal.get(k);
         String scaleFactorName = getScaleFactorName(prodStr);
         if (scaleFactorName != null) {
            int[] dimLens = ncFile.getDimensionLengths(scaleFactorName);
            float[] fltArray = ncFile.getFloatArray(scaleFactorName, new int[] {0}, new int[] {dimLens[0]}, new int[] {1});
            scale = fltArray[0];
            offset = fltArray[1];
         }
         float[] range = getValidRange(prodStr);
         double[] missing = getMissing(prodStr);
         RangeProcessor rngProcessor = new RangeProcessor(ncFile, metadata, scale, offset, range[0], range[1], missing);
         rngProcessors[k] = rngProcessor;
      }
      
      AggregationRangeProcessor aggRngProcessor = new AggregationRangeProcessor(aggReader, metadata, rngProcessors);
      SwathAdapter adapter = new SwathAdapter(aggReader, metadata, aggGeoReader);
      
      setDataChoice(adapter, 0, getProductName(prodStr));
   }

   public List getDataChoices() {
       return myDataChoices;
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
       Hashtable props = new Hashtable();
       props.put(MultiDimensionSubset.key, dataSel);
       DataChoice dataChoice = new DirectDataChoice(this, idx, name, name, null, props);
       dataChoice.setProperties(props);
       myDataChoices.add(dataChoice);
       swathAdapters.add(adapter);       
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
      try {
         SwathAdapter adapter = getSwathAdapter(dataChoice);
         
         MultiDimensionSubset select = null;
         Hashtable table = dataChoice.getProperties();
         Enumeration keys = table.keys();
         while (keys.hasMoreElements()) {
             Object key = keys.nextElement();
             if (key instanceof MultiDimensionSubset) {
                select = (MultiDimensionSubset) table.get(key);
             }
         }
         HashMap subset = select.getSubset();
         
         Data data = adapter.getData(subset);
         return data;
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
    }
    
    public static String getProductName(String prodStr) {
       String name = null;
       switch (prodStr) {
          case "I01":
             name = "All_Data/VIIRS-I1-SDR_All/Reflectance";
             break;
          case "I02":
             name = "All_Data/VIIRS-I2-SDR_All/Reflectance";
             break;
          case "I03":
             name = "All_Data/VIIRS-I3-SDR_All/Reflectance";
             break;
          case "M01":
             name = "All_Data/VIIRS-M1-SDR_All/Reflectance";
             break;
          case "M02":
             name = "All_Data/VIIRS-M2-SDR_All/Reflectance";
             break;
          case "M03":
             name = "All_Data/VIIRS-M3-SDR_All/Reflectance";
             break;
          case "M04":
             name = "All_Data/VIIRS-M4-SDR_All/Reflectance";
             break;
          case "M05":
             name = "All_Data/VIIRS-M5-SDR_All/Reflectance";
             break;
          case "M06":
             name = "All_Data/VIIRS-M6-SDR_All/Reflectance";
             break;
          case "M07":
             name = "All_Data/VIIRS-M7-SDR_All/Reflectance";
             break;
          case "M08":
             name = "All_Data/VIIRS-M8-SDR_All/Reflectance";
             break;
          case "M09":
             name = "All_Data/VIIRS-M9-SDR_All/Reflectance";
             break;
          case "M10":
             name = "All_Data/VIIRS-M10-SDR_All/Reflectance";
             break;
          case "M11":
             name = "All_Data/VIIRS-M11-SDR_All/Reflectance";
             break;  
          case "M12":
             name = "All_Data/VIIRS-M12-SDR_All/BrightnessTemperature";
             break;
          case "M13":
             name = "All_Data/VIIRS-M13-SDR_All/BrightnessTemperature";
             break;
          case "M14":
             name = "All_Data/VIIRS-M14-SDR_All/BrightnessTemperature";
             break;
          case "M15":
             name = "All_Data/VIIRS-M15-SDR_All/BrightnessTemperature";
             break;
          case "M16":
             name = "All_Data/VIIRS-M16-SDR_All/BrightnessTemperature";
             break;
          case "I04":
             name = "All_Data/VIIRS-I4-SDR_All/BrightnessTemperature";
             break;
          case "I05":
             name = "All_Data/VIIRS-I5-SDR_All/BrightnessTemperature";
             break;   
          case "DNB":
             name = "All_Data/VIIRS-DNB-SDR_All/Radiance";
       }
       return name;
    }    
    
    public static String getScaleFactorName(String prodStr) {
       String name = null;
       switch (prodStr) {
          case "I01":
             name = "All_Data/VIIRS-I1-SDR_All/ReflectanceFactors";
             break;
          case "I02":
             name = "All_Data/VIIRS-I2-SDR_All/ReflectanceFactors";
             break;
          case "I03":
             name = "All_Data/VIIRS-I3-SDR_All/ReflectanceFactors";
             break;
          case "M01":
             name = "All_Data/VIIRS-M1-SDR_All/ReflectanceFactors";
             break;
          case "M02":
             name = "All_Data/VIIRS-M2-SDR_All/ReflectanceFactors";
             break;
          case "M03":
             name = "All_Data/VIIRS-M3-SDR_All/ReflectanceFactors";
             break;
          case "M04":
             name = "All_Data/VIIRS-M4-SDR_All/ReflectanceFactors";
             break;
          case "M05":
             name = "All_Data/VIIRS-M5-SDR_All/ReflectanceFactors";
             break;
          case "M06":
             name = "All_Data/VIIRS-M6-SDR_All/ReflectanceFactors";
             break;
          case "M07":
             name = "All_Data/VIIRS-M7-SDR_All/ReflectanceFactors";
             break;
          case "M08":
             name = "All_Data/VIIRS-M8-SDR_All/ReflectanceFactors";
             break;
          case "M09":
             name = "All_Data/VIIRS-M9-SDR_All/ReflectanceFactors";
             break;
          case "M10":
             name = "All_Data/VIIRS-M10-SDR_All/ReflectanceFactors";
             break;
          case "M11":
             name = "All_Data/VIIRS-M11-SDR_All/ReflectanceFactors";
             break;  
          case "M12":
             name = "All_Data/VIIRS-M12-SDR_All/BrightnessTemperatureFactors";
             break;
          case "M14":
             name = "All_Data/VIIRS-M14-SDR_All/BrightnessTemperatureFactors";
             break;
          case "M15":
             name = "All_Data/VIIRS-M15-SDR_All/BrightnessTemperatureFactors";
             break;
          case "M16":
             name = "All_Data/VIIRS-M16-SDR_All/BrightnessTemperatureFactors";
             break;
          case "I04":
             name = "All_Data/VIIRS-I4-SDR_All/BrightnessTemperatureFactors";
             break;
          case "I05":
             name = "All_Data/VIIRS-I5-SDR_All/BrightnessTemperatureFactors";
             break;             
       }
       return name;
    }
    
    public static double[] getMissing(String prodStr) {
       float[] missing = null;
       switch (prodStr) {
          case "I01":
             missing = getMissingRefl();
             break;
          case "I02":
             missing = getMissingRefl();
             break;
          case "I03":
             missing = getMissingRefl();
             break;
          case "M01":
             missing = getMissingRefl();
             break;
          case "M02":
             missing = getMissingRefl();
             break;
          case "M03":
             missing = getMissingRefl();
             break;
          case "M04":
             missing = getMissingRefl();
             break;
          case "M05":
             missing = getMissingRefl();
             break;
          case "M06":
             missing = getMissingRefl();
             break;
          case "M07":
             missing = getMissingRefl();
             break;
          case "M08":
             missing = getMissingRefl();
             break;
          case "M09":
             missing = getMissingRefl();
             break;
          case "M10":
             missing = getMissingRefl();
             break;
          case "M11":
             missing = getMissingRefl();
             break;  
          case "M12":
             missing = getMissingEmis();
             break;
          case "M13":
             missing = getMissingM13();
             break;
          case "M14":
             missing = getMissingEmis();
             break;
          case "M15":
             missing = getMissingEmis();
             break;
          case "M16":
             missing = getMissingEmis();
             break;
          case "I04":
             missing = getMissingEmis();
             break;
          case "I05":
             missing = getMissingEmis();
             break;   
          case "DNB":
             missing = getMissingDNB();
             break;
       }
       double[] dblArray = new double[missing.length];
       for (int i=0; i<dblArray.length; i++) dblArray[i] = missing[i];
       return dblArray;
    }
    
    public static float[] getMissingRefl() {
       float[] missing = new float[] {
          65535 - 65536,
          65534 - 65536,
          65533 - 65536,
          65532 - 65536,
          65531 - 65536,
          65530 - 65536,
          65529 - 65536,
          65528 - 65536
       };
           
       return missing;
    }
    
    public static float[] getMissingEmis() {
       float[] missing = new float[] {
          65535 - 65536,
          65534 - 65536,
          65533 - 65536,
          65532 - 65536,
          65531 - 65536,
          65529 - 65536,
          65528 - 65536
       };
           
       return missing;
    }
    
    public static float[] getMissingM13() {
       float[] missing = new float[] {
          -999.9f,
          -999.8f,
          -999.7f,
          -999.6f,
          -999.5f,
          -999.3f
       };
       return missing;
    }
    
    public static float[] getMissingDNB() {
       float[] missing = new float[] {
          -999.9f,
          -999.8f,
          -999.5f,
          -999.3f
       };
       return missing;       
    }
    
    public static float[] getValidRange(String prodStr) {
       float[] validRange = new float[] {-Float.MAX_VALUE, Float.MAX_VALUE};
       switch(prodStr) {
          case "I01":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "I02":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "I03":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "I04":
             validRange[0] = 208f;
             validRange[1] = 367f;
             break;
          case "I05":
             validRange[0] = 150f;
             validRange[1] = 380f;
             break;
          case "M01":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M02":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M03":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M04":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M05":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M06":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "I06":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M07":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M08":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M09":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M10":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M11":
             validRange[0] = 0f;
             validRange[1] = 1.6f;
             break;
          case "M12":
             validRange[0] = 203f;
             validRange[1] = 368f;
             break;
          case "M13":
             validRange[0] = 192f;
             validRange[1] = 683f;
             break;
          case "M14":
             validRange[0] = 120f;
             validRange[1] = 365f;
             break;
          case "M15":
             validRange[0] = 111f;
             validRange[1] = 381f;
             break;  
          case "M16 ":
             validRange[0] = 103f;
             validRange[1] = 382f;
             break;
       }
       return validRange;
    }


}
