package edu.wisc.ssec.hydra.data;

import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.GranuleAggregation;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.RangeProcessor;
import edu.wisc.ssec.adapter.AggregationRangeProcessor;
import edu.wisc.ssec.adapter.CrIS_SDR_MultiSpectralData;
import edu.wisc.ssec.adapter.CrIS_SDR_Spectrum;
import edu.wisc.ssec.adapter.CrIS_SDR_SwathAdapter;
import edu.wisc.ssec.adapter.MultiSpectralAggr;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.hydra.Hydra;
import visad.VisADException;
import visad.Data;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ucar.unidata.util.ColorTable;


public class NOAA_SNPP_DataSource extends DataSource {

   String dateTimeStamp = null;

   String description = null;

   File[] files = null;

   ArrayList<DataChoice> myDataChoices = new ArrayList<DataChoice>();
   //ArrayList<SwathAdapter> swathAdapters = new ArrayList<SwathAdapter>();
   
   String[] bandNames = null;
   float[] centerWavelength = null;
   
   private ArrayList<MultiSpectralData> msd_CrIS = new ArrayList<MultiSpectralData>();
   private ArrayList<MultiSpectralData> multiSpectralData = new ArrayList<MultiSpectralData>();
   private HashMap<String, MultiSpectralData> msdMap = new HashMap<String, MultiSpectralData>();
   
   public static float[] ATMSChannelCenterFrequencies = {
                23.8f,
                31.4f,
                50.3f,
                51.76f,
                52.8f,
                53.596f,
                54.40f,
                54.94f,
                55.50f,
                57.29032f,
                57.29033f,
                57.29034f,
                57.29035f,
                57.29036f,
                57.29037f,
                88.20f,
                165.5f,
                183.3101f,
                183.3102f,
                183.3103f,
                183.3104f,
                183.3105f
   };



   public NOAA_SNPP_DataSource(File directory) throws Exception {
     this(directory.listFiles());
   }

   public NOAA_SNPP_DataSource(File[] files) throws Exception {
      
      File file = files[0];
      String name = file.getName();
     
      File[] geoFiles = null;
      
      String prefix = null;
      if (name.startsWith("SATMS")) {
         prefix = "GATMO_npp";
      }
      else if (name.startsWith("SCRIS")) {
         prefix = "GCRSO_npp";
      }
      else if (!(name.startsWith("GATMO-SATMS") || name.startsWith("GCRSO-SCRIS"))) {
         throw new Exception("unknown SNPP filename prefix: "+name);
      }
      
      if (prefix != null) {
         geoFiles = new File[files.length];
         ArrayList<File> geoFileList = new ArrayList<File>();
        
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
               File geoFile = geoFileList.get(k);
               Matcher matcher = pattern.matcher(geoFile.getName());
               if (matcher.find()) {
                 geoFiles[i] = geoFile;
                 break;
               }      
            }
         }
      }
      
      dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
      description = DataSource.getDescriptionFromFilename(file.getName());
      
      try {
         init(files, geoFiles);
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }
   
   void init(File[] files, File[] geoFiles) throws Exception {
      int numGrans = files.length;
      
      ArrayList<NetCDFFile> ncdfal = new ArrayList<NetCDFFile>();
      for (int k=0; k<files.length; k++) {
         ncdfal.add(new NetCDFFile(files[k].getAbsolutePath()));
      }
      GranuleAggregation aggReader = new GranuleAggregation(ncdfal, "Track");
      
      GranuleAggregation aggGeoReader = null;
      if (geoFiles != null) {
         ArrayList<NetCDFFile> ncdfalGeo = new ArrayList<NetCDFFile>();
         for (int k=0; k<files.length; k++) {
            ncdfal.add(new NetCDFFile(files[k].getAbsolutePath()));
            ncdfalGeo.add(new NetCDFFile(geoFiles[k].getAbsolutePath()));
         }  
         aggGeoReader = new GranuleAggregation(ncdfalGeo, "Track");     
      }
      
      String name = files[0].getName();
      String[] strs = name.split("_");
      String prodStr = strs[0];
      
      MultiSpectralData msd = null;
      String[] productPaths = null;
      String geoDatasetPath = null;
      if (prodStr.contains("SATMS")) {
         prodStr = "SATMS";
         geoDatasetPath = "All_Data/ATMS-SDR-GEO_All/";
         msd = buildATMS(aggReader, aggGeoReader, ncdfal, prodStr, geoDatasetPath);
      }
      else if (prodStr.contains("SCRIS")) {
         prodStr = "SCRIS";
         geoDatasetPath = "All_Data/CrIS-SDR-GEO_All/";
         String path = getProductName(prodStr);
         productPaths = new String[] {path+"ES_RealLW", path+"ES_RealMW", path+"ES_RealSW"};
         msd = buildCrIS(aggReader, aggGeoReader, productPaths, geoDatasetPath);
      }
      else {
         throw new Exception("unknown product: "+prodStr);
      }
      
      multiSpectralData.add(msd);      
      
      DataChoice choice = setDataChoice(msd, 0, "BrightnessTemperature");
      
      msdMap.put(choice.getName(), msd);
   }
   
   RangeProcessor buildRangeProcessor(GranuleAggregation aggReader, ArrayList<NetCDFFile> ncdfal, String prodStr, HashMap metadata) throws Exception {
      int numGrans = ncdfal.size();
      RangeProcessor[] rngProcessors = new RangeProcessor[numGrans];
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
      return aggRngProcessor;
   }
   
   MultiSpectralData buildATMS(MultiDimensionReader reader, MultiDimensionReader geoReader, ArrayList<NetCDFFile> ncdfal, String prodStr, String geoDatasetPath) throws Exception {
      boolean unsigned = true;
      boolean unpack = true;
      boolean range_check_after_scaling = true;
      
      HashMap metadata = fillSwathMetadataTable(
              "XTrack",
              "Track",
              "Channel",
              null,
              getProductName(prodStr),
              prodStr,
              "XTrack",
              "Track",
              geoDatasetPath+"Longitude",
              geoDatasetPath+"Latitude",
              new String[] {"Track", "XTrack", "Channel"},
              new String[] {"Track", "XTrack"},
              new String[] {"Track", "XTrack"},
              unsigned, unpack, range_check_after_scaling, null
      );
      
      HashMap<String, Object> spectTable = SpectrumAdapter.getEmptyMetadataTable();
      spectTable.put("array_name", getProductName(prodStr));
      spectTable.put("product_name", "ATMS-SDR");
      spectTable.put(SpectrumAdapter.channelIndex_name, "Channel");
      spectTable.put(SpectrumAdapter.channelType, "channel_number");
      spectTable.put(SpectrumAdapter.channels_name, "Channel");
      spectTable.put(SpectrumAdapter.x_dim_name, "XTrack");
      spectTable.put(SpectrumAdapter.y_dim_name, "Track");     
      spectTable.put("array_dimension_names", new String[] {"Track", "XTrack", "Channel"});
      spectTable.put("lon_array_dimension_names", new String[] {"Track", "XTrack"});
      spectTable.put("lat_array_dimension_names", new String[] {"Track", "XTrack"});
      
      int numChannels = ATMSChannelCenterFrequencies.length;
      float[] bandArray = new float[numChannels];
      String[] bandNames = new String[numChannels];
      for (int bIdx = 0; bIdx < numChannels; bIdx++) {
              bandArray[bIdx] = ATMSChannelCenterFrequencies[bIdx];
              bandNames[bIdx] = "Ch"+(bIdx + 1)+"("+bandArray[bIdx]+"GHz)";
              bandArray[bIdx] = (float) bIdx + 1;
      }
      spectTable.put(SpectrumAdapter.channelValues, bandArray);
      spectTable.put(SpectrumAdapter.bandNames, bandNames);
  
      
      buildRangeProcessor((GranuleAggregation)reader, ncdfal, prodStr, metadata);
      
      
      SwathAdapter adapter = new SwathAdapter(reader, metadata, geoReader);
      
      SpectrumAdapter spectAdapter = new SpectrumAdapter(reader, spectTable);
      MultiSpectralData msd = new MultiSpectralData(adapter, spectAdapter, 
                   "BrightnessTemperature", "BrightnessTemperature", "ATMS", "SuomiNPP");
      msd.setInitialWavenumber(2f);
            
      return msd;
   }
   
   MultiSpectralData buildCrIS(MultiDimensionReader reader, MultiDimensionReader geoReader, String[] productPaths, String geoDatasetPath) throws Exception {
      boolean unsigned = false;
      boolean unpack = false;
      boolean range_check_after_scaling = false;
      
      MultiSpectralData aggrMSDs = null;
      
      for (int k=0; k<productPaths.length; k++) {
         String productPath = productPaths[k];
      
         HashMap metadata = fillSwathMetadataTable(
                 "dim1",
                 "dim0",
                 "dim3",
                 "dim2",
                 productPath,
                 "radiances",
                 "dim1",
                 "dim0",
                 geoDatasetPath+"Longitude",
                 geoDatasetPath+"Latitude",
                 null,
                 null,
                 null,
                 unsigned, unpack, range_check_after_scaling, "CrIS_SDR"
         );

         HashMap<String, Object> spectTable = SpectrumAdapter.getEmptyMetadataTable();
         spectTable.put("array_name", productPath);
         spectTable.put("product_name", "CrIS_SDR");
         spectTable.put(SpectrumAdapter.channelIndex_name, "dim3");
         spectTable.put(SpectrumAdapter.channelType, "channel_number");
         spectTable.put(SpectrumAdapter.channels_name, "Channel");
         spectTable.put(SpectrumAdapter.x_dim_name, "dim1");
         spectTable.put(SpectrumAdapter.y_dim_name, "dim0");  
         spectTable.put(SpectrumAdapter.FOVindex_name, "dim2");
         

         float scale = 1f;
         float offset = 0f;
         
         float[] range = getValidRange("SCRIS");
         double[] missing = getMissing("SCRIS");
         RangeProcessor rngProcessor = new RangeProcessor(reader, metadata, scale, offset, range[0], range[1], missing);

         SwathAdapter adapter = new CrIS_SDR_SwathAdapter(reader, metadata, geoReader, rngProcessor);
         CrIS_SDR_Spectrum csa = new CrIS_SDR_Spectrum(reader, spectTable);
         MultiSpectralData msd = new CrIS_SDR_MultiSpectralData((CrIS_SDR_SwathAdapter) adapter, csa); 
         msd_CrIS.add(msd);
         aggrMSDs = new MultiSpectralAggr(msd_CrIS.toArray(new MultiSpectralData[msd_CrIS.size()]), "radiances");
      }
      aggrMSDs.setInitialWavenumber(902.25f);
      
      return aggrMSDs;      
   }

   public List getDataChoices() {
       return myDataChoices;
   }
   
   public String getDateTimeStamp() {
      return dateTimeStamp;
   }
   
   public String getDescription() {
      return description;
   }
   
   public boolean isMultiSpectral() {
      return true;
   }
   
   public boolean isSounder() {
      return true;
   }
   
   public HashMap fillSwathMetadataTable(String xtrack, String track, String channel, String fovIndex,
                                    String array, String range, String geoXtrack, String geoTrack,
                                    String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                    boolean unsigned, boolean unpack, boolean range_check_after_scaling, String product_name) {
      
      HashMap metadata = SwathAdapter.getEmptyMetadataTable();
      
      metadata.put(SwathAdapter.xtrack_name, xtrack);
      metadata.put(SwathAdapter.track_name, track);
      metadata.put(SpectrumAdapter.channelIndex_name, channel);
      if (fovIndex != null) {
         metadata.put(SpectrumAdapter.FOVindex_name, fovIndex);
      }
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
      if (product_name != null) {
         metadata.put("product_name", product_name);
      }
      
      return metadata;
   }

   
   public MultiSpectralData getMultiSpectralData(DataChoice choice) {
      return msdMap.get(choice.getName());
   }

   
   DataChoice setDataChoice(MultiSpectralData adapter, int idx, String name) {
       HashMap subset = adapter.getDefaultSubset();
       DataSelection dataSel = new MultiDimensionSubset(subset);
       DataChoice dataChoice = new DataChoice(this, name, null);
       dataChoice.setDataSelection(dataSel);
       myDataChoices.add(dataChoice);
       return dataChoice;
    }  

   public Data getData(DataChoice dataChoice, DataSelection dataSelection)
                                throws VisADException, RemoteException 
    {
      try {
         MultiDimensionAdapter adapter = getMultiSpectralData(dataChoice);
         
         
         MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
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
          case "SATMS":
             name = "All_Data/ATMS-SDR_All/BrightnessTemperature";
             break;
          case "SCRIS":
             name = "All_Data/CrIS-SDR_All/";
             break;
       }
       return name;
    }    
    
    public static String getScaleFactorName(String prodStr) {
       String name = null;
       switch (prodStr) {
          case "SATMS":
             name = "All_Data/ATMS-SDR_All/BrightnessTemperatureFactors";
             break;
       }
       return name;
    }
    
    public static double[] getMissing(String prodStr) {
       float[] missing = null;
       switch (prodStr) {
          case "SATMS":
             missing = getMissingATMS();
             break;
          case "SCRIS":
             missing = getMissingCrIS();
             break;
       }
       double[] dblArray = new double[missing.length];
       for (int i=0; i<dblArray.length; i++) dblArray[i] = missing[i];
       return dblArray;
    }
    
    public static float[] getMissingATMS() {
       float[] missing = new float[] {
          65535 - 65536,
          65534 - 65536,
          65531 - 65536,
          65529 - 65536,
          65528 - 65536
       };
           
       return missing;
    }
    
    public static float[] getMissingCrIS() {
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
          case "SATMS":
             validRange[0] = 0f;
             validRange[1] = 330f;
             break;
       }
       return validRange;
    }
    
    public ColorTable getDefaultColorTable(DataChoice choice) {
       return Hydra.invGrayTable;
    }   
}
