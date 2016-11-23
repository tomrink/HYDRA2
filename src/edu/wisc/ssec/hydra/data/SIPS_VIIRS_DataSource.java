package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.AggregationRangeProcessor;
import edu.wisc.ssec.adapter.ArrayAdapter;
import edu.wisc.ssec.adapter.GranuleAggregation;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.RangeProcessor;
import visad.VisADException;
import visad.Data;
import visad.FlatField;
import java.rmi.RemoteException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import visad.util.Util;


public abstract class SIPS_VIIRS_DataSource extends DataSource {

   String dateTimeStamp = null;

   String description = null;

   File[] files = null;

   ArrayList<DataChoice> myDataChoices = new ArrayList<DataChoice>();
   ArrayList<SwathAdapter> swathAdapters = new ArrayList<SwathAdapter>();
   
   String[] bandNames = null;
   float[] centerWavelength = null;
   
   ArrayList<NetCDFFile> ncdfal;
   ArrayList<NetCDFFile> ncdfalGeo;
   
   MultiDimensionReader reader;
   MultiDimensionReader geoReader;
   
   ArrayAdapter solzenAdapter;
   
   int numGrans;


   public SIPS_VIIRS_DataSource(File directory) {
     this(directory.listFiles());
   }

   public SIPS_VIIRS_DataSource(File[] files) {

      try { 
         initReader(files);
         init();
      } catch (Exception e) {
        e.printStackTrace();
      }
      
   }
   
   void init() throws Exception {
   }

   void initReader(File[] files) throws Exception {
      ArrayList<File> sortedList = DataSource.getTimeSortedFileList(files);
      Object[] sortedFiles = sortedList.toArray();
      files = new File[files.length];
      for (int k=0; k<sortedFiles.length; k++) {
         files[k] = (File) sortedFiles[k];
      }
      
      ArrayList<File> geoFileList = new ArrayList<File>();

      File file = files[0];
      String name = file.getName();
     
      File[] geoFiles = new File[files.length];
      
      String prefix = null;
      if (name.startsWith("VL1BI")) {
         prefix = "VGEOI_snpp";
      }
      else if (name.startsWith("VL1BM")) {
         prefix = "VGEOM_snpp";
      }
      else if (name.startsWith("VL1BD")) {
         prefix = "VGEOD_snpp";
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
         String regex = prefix+"_"+strs[2]+"_"+strs[3]+".*";
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
      
      ncdfal = new ArrayList<NetCDFFile>();
      ncdfalGeo = new ArrayList<NetCDFFile>();
      
      numGrans = files.length;
      
      for (int k=0; k<numGrans; k++) {
         ncdfal.add(new NetCDFFile(files[k].getAbsolutePath()));
         ncdfalGeo.add(new NetCDFFile(geoFiles[k].getAbsolutePath()));
      }
      
      reader = new GranuleAggregation(ncdfal, "number_of_lines");
      geoReader = new GranuleAggregation(ncdfalGeo, "number_of_lines");
      
      
      dateTimeStamp = DataSource.getDateTimeStampFromFilename(files[0].getName());
   }
   
   public List getDataChoices() {
       return myDataChoices;
   }
   
   RangeProcessor buildRadToBT(HashMap metadata, String btLUTname) throws Exception  {
      RangeProcessor[] rngProcessors = new RangeProcessor[numGrans];
      
      for (int k=0; k<numGrans; k++) {
         rngProcessors[k] = new BTbyLUT(ncdfal.get(k), metadata, btLUTname);
      }
      
      RangeProcessor rangeProcessor = new AggregationRangeProcessor((GranuleAggregation)reader, metadata, rngProcessors);
      
      return rangeProcessor;
   }
   
   public HashMap fillSwathMetadata(String xtrack, String track, 
                                    String array, String range, String geoXtrack, String geoTrack,
                                    String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                    String fillValueName) {
      
      HashMap metadata = SwathAdapter.getEmptyMetadataTable();

      metadata.put(SwathAdapter.xtrack_name, xtrack);
      metadata.put(SwathAdapter.track_name, track);
      metadata.put(SwathAdapter.geo_xtrack_name, geoXtrack);
      metadata.put(SwathAdapter.geo_track_name, geoTrack);
      metadata.put(SwathAdapter.array_name, array);
      metadata.put(SwathAdapter.range_name, range);
      metadata.put(SwathAdapter.lon_array_name, lonArray);
      metadata.put(SwathAdapter.lat_array_name, latArray);
      //metadata.put(SwathAdapter.scale_name, "scale_factor");
      metadata.put("unsigned", "true");
      //metadata.put("valid_low", "valid_min");
      //metadata.put("valid_high", "valid_max");
      if (lonArrayDims != null) {
         metadata.put(SwathAdapter.lon_array_dimension_names, lonArrayDims);
      }
      if (latArrayDims != null) {
         metadata.put(SwathAdapter.lat_array_dimension_names, latArrayDims);
      }
      if (arrayDims != null) {
         metadata.put(SwathAdapter.array_dimension_names, arrayDims);     
      }
      if (fillValueName != null) {
         //metadata.put(SwathAdapter.fill_value_name, fillValueName);
      }
      return metadata;
   }

   public SwathAdapter buildReflAdapter(String xtrack, String track, 
                                    String array, String range, String geoXtrack, String geoTrack,
                                    String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                    String fillValueName) throws Exception {


      HashMap metadata = fillSwathMetadata(xtrack, track, array, range, geoXtrack, geoTrack, lonArray, latArray, arrayDims, lonArrayDims, latArrayDims, fillValueName);

      String geoGroupName = "geolocation_data/";
      HashMap szmetadata = new HashMap();
      szmetadata.put(SwathAdapter.track_name, "number_of_lines");
      szmetadata.put(SwathAdapter.xtrack_name, "number_of_pixels");
      szmetadata.put("array_name", geoGroupName+"solar_zenith");
      szmetadata.put("array_dimension_names", new String[] {"number_of_lines", "number_of_pixels"});
      szmetadata.put("unsigned", "true");
      solzenAdapter = new ArrayAdapter(geoReader, szmetadata);
      
      SwathAdapter swathAdapter;
      
      if (geoReader != null) {
         swathAdapter = new SwathAdapter(reader, metadata, geoReader);
      }
      else {
         swathAdapter = new SwathAdapter(reader, metadata);
      }
      
      return swathAdapter;
   }
   
      public SwathAdapter buildEmisAdapter(String xtrack, String track, 
                                    String array, String range, String geoXtrack, String geoTrack,
                                    String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                    String fillValueName, String btLUTName) throws Exception {


      HashMap metadata = fillSwathMetadata(xtrack, track, array, range, geoXtrack, geoTrack, lonArray, latArray, arrayDims, lonArrayDims, latArrayDims, fillValueName);



      SwathAdapter swathAdapter;
      buildRadToBT(metadata, btLUTName);
      
      if (geoReader != null) {
         swathAdapter = new SwathAdapter(reader, metadata, geoReader);
      }
      else {
         swathAdapter = new SwathAdapter(reader, metadata);
      }
      

      return swathAdapter;
   }
   
      public SwathAdapter buildAdapter(String xtrack, String track, 
                                    String array, String range, String geoXtrack, String geoTrack,
                                    String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                    String fillValueName) {


      HashMap metadata = fillSwathMetadata(xtrack, track, array, range, geoXtrack, geoTrack, lonArray, latArray, arrayDims, lonArrayDims, latArrayDims, fillValueName);
      SwathAdapter swathAdapter;
      
      if (geoReader != null) {
         swathAdapter = new SwathAdapter(reader, metadata, geoReader);
      }
      else {
         swathAdapter = new SwathAdapter(reader, metadata);
      }

      return swathAdapter;
   }
   
   public SwathAdapter getSwathAdapter(DataChoice dataChoice) {
      for (int k=0; k<myDataChoices.size(); k++) {
          if (myDataChoices.get(k).equals(dataChoice)) {
              return swathAdapters.get(k);
          }
      }
      return null;
   }

   public String getDescription() {
     return "SNPP VIIRS";
   }

   public String getDateTimeStamp() {
     return dateTimeStamp;
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
         
         MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
         HashMap subset = select.getSubset();
         
         Data data = adapter.getData(subset);
         String name = dataChoice.getName();
         if (name.equals("I01") || name.equals("I02") || name.equals("I03") ||
             name.equals("M01") || name.equals("M02") || name.equals("M03") ||
             name.equals("M04") || name.equals("M05") || name.equals("M06") ||
             name.equals("M07") || name.equals("M08") || name.equals("M09") ||
             name.equals("M10") || name.equals("M11")) {
            
            float[] refls = (((FlatField)data).getFloats(false))[0];
            float[] solzen = ((FlatField)solzenAdapter.getData(subset)).getFloats()[0];
            for (int k=0; k<refls.length; k++) {
               float refl = refls[k];
               float solz = solzen[k];
               if (solz < 80.0f) {
                  refls[k] = refl/((float)Math.cos((Math.PI/180.0)*solz));
               }
            }
         }
         else if (name.equals("DNB")) {
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
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
    }
    
    public abstract float getNadirResolution(DataChoice choice);
    
}

class BTbyLUT extends RangeProcessor {
   
   float[] btLUT;
   
   
   public BTbyLUT(MultiDimensionReader reader, HashMap metadata, String btLUTname) throws Exception {
      super(reader, metadata);
      
      int numLUTvals = (reader.getDimensionLengths(btLUTname))[0];
      btLUT = reader.getFloatArray(btLUTname, new int[] {0}, new int[] {numLUTvals}, new int[] {1});
   }
   
   
   /**
    * calls super to unscale radiances then converts to BT
    * 
    */
   public float[] processRange(short[] values, HashMap subset) {
      
      float[] brightnessTemps = new float[values.length];
      java.util.Arrays.fill(brightnessTemps, Float.NaN);
      
      for (int k=0; k<values.length; k++) {
         int ii = Util.unsignedShortToInt(values[k]);
         if (ii >= 0 && ii < 65536) {
            float bt = btLUT[ii];
            if (bt != -999.9f) {
               brightnessTemps[k] = btLUT[ii];
            }
         }
      }
      
      return brightnessTemps;
   }
}