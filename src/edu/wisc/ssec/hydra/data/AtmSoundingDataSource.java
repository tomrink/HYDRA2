package edu.wisc.ssec.hydra.data;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.AtmSoundingAdapter;
import edu.wisc.ssec.adapter.SwathSoundingData;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.RangeProcessor;
import visad.VisADException;
import visad.Data;
import java.rmi.RemoteException;


public class AtmSoundingDataSource extends DataSource {

   String dateTimeStamp = null;

   String description = null;

   File[] files = null;

   SwathSoundingData data = null;
   
   ArrayList<DataChoice> myDataChoices = new ArrayList<DataChoice>();
   ArrayList<SwathSoundingData> mySoundingDatas = new ArrayList<SwathSoundingData>();
   

   public AtmSoundingDataSource(File directory) {
     this(directory.listFiles());
   }

   public AtmSoundingDataSource(File[] files) {

      this.files = files;
      int numFiles = files.length;
      File file = files[0];
      
      dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
      description = DataSource.getDescriptionFromFilename(file.getName());
      
      try { 
        init(file);
      } catch (Exception e) {
        e.printStackTrace();
      }
   }

   void init(File file) throws Exception {
   }
   
   public List getDataChoices() {
       return myDataChoices;
   }

   public SwathSoundingData buildAdapter(MultiDimensionReader reader, String xtrack, String track, String levelIndex, String levelsName, float[] levelValues, 
                                         String array, String range, String geoXtrack, String geoTrack,
                                         String lonArray, String latArray, String[] arrayDims, String[] lonArrayDims, String[] latArrayDims,
                                         String fillValueName) {

      HashMap metadata = AtmSoundingAdapter.getEmptyMetadataTable();
      metadata.put(AtmSoundingAdapter.levels_name, levelsName);
      metadata.put(AtmSoundingAdapter.array_name, array);
      metadata.put(AtmSoundingAdapter.range_name, range);
      metadata.put(AtmSoundingAdapter.levelIndex_name, levelIndex);
      metadata.put(AtmSoundingAdapter.x_dim_name, xtrack);
      metadata.put(AtmSoundingAdapter.y_dim_name, track);
      metadata.put(AtmSoundingAdapter.levelValues, levelValues);
      if (arrayDims != null) {
         metadata.put(AtmSoundingAdapter.array_dimension_names, arrayDims);     
      }
      if (fillValueName != null) {
         metadata.put(AtmSoundingAdapter.fill_value_name, fillValueName);
      }

      AtmSoundingAdapter soundingAdapter = new AtmSoundingAdapter(reader, metadata);
      
      try {
          soundingAdapter.setRangeProcessor(RangeProcessor.createRangeProcessor(reader, metadata));
      }
      catch (Exception e) {
          e.printStackTrace();
      }

      metadata = SwathAdapter.getEmptyMetadataTable();

      metadata.put(SwathAdapter.xtrack_name, xtrack);
      metadata.put(SwathAdapter.track_name, track);
      metadata.put(SwathAdapter.geo_xtrack_name, geoXtrack);
      metadata.put(SwathAdapter.geo_track_name, geoTrack);
      metadata.put(SwathAdapter.array_name, array);
      metadata.put(SwathAdapter.range_name, range);
      metadata.put(SwathAdapter.lon_array_name, lonArray);
      metadata.put(SwathAdapter.lat_array_name, latArray);
      metadata.put(SwathAdapter.lon_array_dimension_names, lonArrayDims);
      metadata.put(SwathAdapter.lat_array_dimension_names, latArrayDims);
      metadata.put(AtmSoundingAdapter.levelIndex_name, levelIndex);
      if (arrayDims != null) {
         metadata.put(SwathAdapter.array_dimension_names, arrayDims);     
      }
      if (fillValueName != null) {
         metadata.put(SwathAdapter.fill_value_name, fillValueName);
      }

      SwathAdapter swathAdapter = new SwathAdapter(reader, metadata);

      SwathSoundingData data = new SwathSoundingData(swathAdapter, soundingAdapter);

      return data;
   }
   
   public SwathSoundingData getSwathSoundingData(DataChoice dataChoice) {
      for (int k=0; k<myDataChoices.size(); k++) {
          if (myDataChoices.get(k).equals(dataChoice)) {
              return mySoundingDatas.get(k);
          }
      }
      return null;
   }

   public String getDescription() {
     return description;
   }

   public String getDateTimeStamp() {
     return dateTimeStamp;
   }
   
   public boolean isAtmRetrieval() {
      return true;
   }


   public Data getData(DataChoice dataChoice, DataSelection dataSelection)
            throws VisADException, RemoteException 
    {
      try {
         data = getSwathSoundingData(dataChoice);
         return data.getImage(data.getDefaultSubset());
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
    }
    
    public static AtmSoundingDataSource makeDataSource() {
        return null;
    }

}
