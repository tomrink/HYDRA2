package edu.wisc.ssec.hydra.data;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataChoice;
import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;
import edu.wisc.ssec.adapter.MultiSpectralDataSource;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.CrIS_SDR_SwathAdapter;
import edu.wisc.ssec.adapter.AtmSoundingAdapter;
import edu.wisc.ssec.adapter.SwathSoundingData;
import edu.wisc.ssec.adapter.CrIS_SwathSoundingData;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.GranuleAggregation;
import visad.VisADException;
import visad.Data;
import visad.FlatField;
import java.rmi.RemoteException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class CrIS_SoundingDataSource extends AtmSoundingDataSource {

   String dateTimeStamp = null;

   public CrIS_SoundingDataSource(File directory) {
     this(directory.listFiles());
   }

   public CrIS_SoundingDataSource(File[] files) {
      super(files);
      /* not yet
      try {
         ArrayList<NetCDFFile> ncdfal = new ArrayList<NetCDFFile>();
         TreeSet<String> pathToProducts = new TreeSet<String>();
         ncdfal.add(new NetCDFFile(files[0].getAbsolutePath()));
         ncdfal.add(new NetCDFFile(files[1].getAbsolutePath()));
         pathToProducts.add("TAir");
         //pathToProducts.add("H20MMR");
         //pathToProducts.add("03VMR");
         GranuleAggregation aggReader = new GranuleAggregation(ncdfal, pathToProducts, "Scan", "View");
         init(aggReader);
      } catch(Exception e) {
        e.printStackTrace();
      }
      */
   }

   void init(File file) throws Exception {
      NetCDFFile reader = new NetCDFFile(file.getAbsolutePath());

      double[] dvals = {0.0050, 0.0161, 0.0384, 0.0769, 0.137, 0.2244, 0.3454, 0.5064, 0.714, 0.9753, 1.2972, 1.6872, 2.1526, 2.7009, 3.3398, 4.077, 4.9204, 5.8776, 6.9567, 8.1655, 9.5119, 11.0038, 12.6492, 14.4559, 16.4318, 18.5847, 20.9224, 23.4526, 26.1829, 29.121, 32.2744, 35.6505, 39.2566, 43.1001, 47.1882, 51.5278, 56.126, 60.9895, 66.1253, 71.5398, 77.2396, 83.231, 89.5204, 96.1138, 103.0172, 110.2366, 117.7775, 125.6456, 133.8462, 142.3848, 151.2664, 160.4959, 170.0784, 180.0183, 190.3203, 200.9887, 212.0277, 223.4415, 235.2338, 247.4085, 259.9691, 272.9191, 286.2617, 300.0, 314.1369, 328.6753, 343.6176, 358.9665, 374.7241, 390.8926, 407.4738, 424.4698, 441.8819, 459.7118, 477.9607, 496.6298, 515.72, 535.2322, 555.1669, 575.5248, 596.3062, 617.5112, 639.1398, 661.192, 683.6673, 706.5654, 729.8857, 753.6275, 777.7897, 802.3714, 827.3713, 852.788, 878.6201, 904.8659, 931.5236, 958.5911, 986.0666, 1013.9476, 1042.2319, 1070.917, 1100.0};

      float[] vals = new float[101];
      for (int k=0; k<vals.length; k++) {
         vals[k] = (float) dvals[k];
      }
      
      SwathSoundingData dataTA = buildAdapter(reader, "View", "Scan", "Level", "vertPressLevel", vals, "TAir", "Temperature", "View", "Scan",
                                   new String[] {"Level", "Scan", "View"},
                                   "Longitude", "Latitude", new String[] {"Scan", "View"}, new String[] {"Scan", "View"},
                                   null, "missing_value", "CrIS_RTV");
      HashMap subset = dataTA.getDefaultSubset();
      DataSelection dataSel = new MultiDimensionSubset(subset);
      Hashtable props = new Hashtable();
      props.put(MultiDimensionSubset.key, dataSel);
      DataChoice dataChoice = new DirectDataChoice(this, 0, "Temp", "Temp", null, props);
      dataChoice.setProperties(props);
      myDataChoices.add(dataChoice);
      mySoundingDatas.add(dataTA);

      SwathSoundingData dataWV = buildAdapter(reader, "View", "Scan", "Level", "vertPressLevel", vals, "H2OMMR", "WV", "View", "Scan",
                                   new String[] {"Level", "Scan", "View"},
                                   "Longitude", "Latitude", new String[] {"Scan", "View"}, new String[] {"Scan", "View"},
                                   null, "missing_value", "CrIS_RTV");
      dataWV.setDataRange(new float[] {0, 20});
      subset = dataWV.getDefaultSubset();
      dataSel = new MultiDimensionSubset(subset);
      props = new Hashtable();
      props.put(MultiDimensionSubset.key, dataSel);
      dataChoice = new DirectDataChoice(this, 1, "WV", "WV", null, props);
      myDataChoices.add(dataChoice);
      mySoundingDatas.add(dataWV);

      SwathSoundingData dataO3 = buildAdapter(reader, "View", "Scan", "Level", "vertPressLevel", vals, "O3VMR", "O3", "View", "Scan",
                                   new String[] {"Level", "Scan", "View"},
                                   "Longitude", "Latitude", new String[] {"Scan", "View"}, new String[] {"Scan", "View"},
                                   null, "missing_value", "CrIS_RTV");
      dataO3.setDataRange(new float[] {0, 20});
      subset = dataO3.getDefaultSubset();
      dataSel = new MultiDimensionSubset(subset);
      props = new Hashtable();
      props.put(MultiDimensionSubset.key, dataSel);
      dataChoice = new DirectDataChoice(this, 2, "O3", "O3", null, props);
      myDataChoices.add(dataChoice);
      mySoundingDatas.add(dataO3);
   }

   public SwathSoundingData buildAdapter(MultiDimensionReader reader, String xtrack, String track, String levelIndex, String levelsName, float[] levelValues, 
                                         String array, String range, String geoXtrack, String geoTrack, String[] arrayDims,
                                         String lonArray, String latArray, String[] lonArrayDims, String[] latArrayDims,
                                         String fovIndex, String fill, String product) {

      HashMap metadata = AtmSoundingAdapter.getEmptyMetadataTable();
      metadata.put(AtmSoundingAdapter.levels_name, levelsName);
      metadata.put(AtmSoundingAdapter.array_name, array);
      metadata.put(AtmSoundingAdapter.range_name, range);
      metadata.put(AtmSoundingAdapter.levelIndex_name, levelIndex);
      metadata.put(AtmSoundingAdapter.x_dim_name, xtrack);
      metadata.put(AtmSoundingAdapter.y_dim_name, track);
      metadata.put(AtmSoundingAdapter.levelValues, levelValues);
      metadata.put(AtmSoundingAdapter.array_dimension_names, arrayDims);
      if (fovIndex != null) {
         metadata.put(AtmSoundingAdapter.FOVindex_name, fovIndex);
      }

      AtmSoundingAdapter soundingAdapter = new AtmSoundingAdapter(reader, metadata);

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
      metadata.put(SwathAdapter.array_dimension_names, arrayDims);
      metadata.put(AtmSoundingAdapter.levelIndex_name, levelIndex);
      metadata.put(SwathAdapter.product_name, product);
      metadata.put(SwathAdapter.fill_value_name, fill);
      if (fovIndex != null) {
         metadata.put(AtmSoundingAdapter.FOVindex_name, fovIndex);
      }

      SwathAdapter swathAdapter = new SwathAdapter(reader, metadata);

      SwathSoundingData data = new CrIS_SwathSoundingData(swathAdapter, soundingAdapter);

      return data;
   }

   public String getDescription() {
     return "CrIS Retrvl";
   }

   public String getDateTimeStamp() {
     String name = files[0].getName();
     
     String yyyymmdd = name.substring(6,14);
     String hhmm = name.substring(16,20);
     
     try {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmm");
        Date datetime = sdf.parse(yyyymmdd.concat(hhmm));
        dateTimeStamp = DataSource.makeDateTimeStamp(datetime);
     }
     catch (Exception e) {
         e.printStackTrace();
     }
     
     return dateTimeStamp;
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
         data = getSwathSoundingData(dataChoice);
         return data.getImage(data.getDefaultSubset());
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
    }
}
