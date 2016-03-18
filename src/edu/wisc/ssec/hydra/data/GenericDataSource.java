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
import java.util.Enumeration;
import edu.wisc.ssec.adapter.SwathAdapter;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import visad.VisADException;
import visad.Data;
import java.rmi.RemoteException;


public class GenericDataSource extends DataSourceImpl {

   String dateTimeStamp = null;

   String description = null;

   File[] files = null;

   HashMap<DataChoice, SwathAdapter> dataChoiceToAdapter = new HashMap<DataChoice, SwathAdapter>();

   HashMap<String, DimensionSet>  arrayNameToDims = new HashMap<String, DimensionSet>();

   ArrayList<FieldInfo> swathFields = new ArrayList<FieldInfo>();


   public GenericDataSource(File directory) {
     this(directory.listFiles());
   }

   public GenericDataSource(File[] files) {
      super(new DataSourceDescriptor(), "GenericDataSource", "GenericDataSource", new Hashtable());

      this.files = files;
      int numFiles = files.length;
      File file = files[0];
      try { 
        init(file);
      } catch (Exception e) {
        e.printStackTrace();
      }
   }

   void init(File file) throws Exception {
      NetCDFFile reader = new NetCDFFile(file.getAbsolutePath());

      String name = file.getName();

      if (name.startsWith("NPR-MIRS") && name.endsWith(".nc")) {
         DimensionSet dimSet = new DimensionSet("Scanline", "Field_of_view", null, null, "Scanline", "Field_of_view");
         String[] arrayNames = new String[] {"RR", "TPW", "CldTop", "CldBase"};
         String[] rangeNames = new String[] {"RainRate", "TPW", "CloudTopPress", "CloudBasePress"};

         for (int k=0; k<arrayNames.length; k++) {
            SwathInfo sInfo = new SwathInfo();
            sInfo.track = dimSet.track;
            sInfo.xtrack = dimSet.xtrack;
            sInfo.geo_track = dimSet.geo_track;
            sInfo.geo_xtrack = dimSet.geo_xtrack;
            sInfo.lonArrayName = "Longitude";
            sInfo.latArrayName = "Latitude";

            FieldInfo fInfo = new FieldInfo();
            fInfo.dimSet = dimSet;
            fInfo.arrayName = arrayNames[k];
            fInfo.rangeName = rangeNames[k];
            fInfo.scaleName = "scale";
            fInfo.divideByScale = true;

            fInfo.swathInfo = sInfo;
          
            swathFields.add(fInfo);
         }
      }


      for (int k=0; k<swathFields.size(); k++) {
         FieldInfo fInfo = swathFields.get(k);
         SwathAdapter swathAdapter = buildSwathAdapter(reader, fInfo);

         HashMap subset = swathAdapter.getDefaultSubset();
         MultiDimensionSubset dataSel = new MultiDimensionSubset(subset);
         Hashtable props = new Hashtable();
         props.put(MultiDimensionSubset.key, dataSel);
         DirectDataChoice dataChoice = new DirectDataChoice(this, k, fInfo.rangeName, fInfo.rangeName, null, props);
         addDataChoice(dataChoice);
         dataChoiceToAdapter.put(dataChoice, swathAdapter);
      }
      
   }

   public SwathAdapter buildSwathAdapter(NetCDFFile reader, FieldInfo swathField) {
      HashMap metadata = SwathAdapter.getEmptyMetadataTable();

      SwathInfo swthInfo = swathField.swathInfo;

      metadata.put(SwathAdapter.array_name, swathField.arrayName);
      metadata.put(SwathAdapter.scale_name, swathField.scaleName);
      metadata.put(SwathAdapter.range_name, swathField.rangeName);
      metadata.put(SwathAdapter.xtrack_name, swthInfo.xtrack);
      metadata.put(SwathAdapter.track_name, swthInfo.track);
      metadata.put(SwathAdapter.geo_xtrack_name, swthInfo.geo_xtrack);
      metadata.put(SwathAdapter.geo_track_name, swthInfo.geo_track);
      metadata.put(SwathAdapter.lon_array_name, swthInfo.lonArrayName);
      metadata.put(SwathAdapter.lat_array_name, swthInfo.latArrayName);
      if ((swthInfo.lonDimNames != null && swthInfo.latDimNames != null)) {
         metadata.put(SwathAdapter.lon_array_dimension_names, swthInfo.lonDimNames);
         metadata.put(SwathAdapter.lat_array_dimension_names, swthInfo.latDimNames);
      }

      if (swathField.divideByScale) {
         metadata.put("divideByScale", "divideByScale");
      }

      SwathAdapter swathAdapter = new SwathAdapter(reader, metadata);

      return swathAdapter;
   }

   public SwathAdapter buildSwathAdapter(NetCDFFile reader, String arrayName, String rangeName, String track, String xtrack,
                                         String latArray, String lonArray, String geoTrack, String geoXtrack,
                                         String[] latArrayDims, String[] lonArrayDims, String scaleName, boolean divideByScale) {

      HashMap metadata = SwathAdapter.getEmptyMetadataTable();

      metadata.put(SwathAdapter.array_name, arrayName);
      metadata.put(SwathAdapter.scale_name, scaleName);
      metadata.put(SwathAdapter.range_name, rangeName);
      metadata.put(SwathAdapter.xtrack_name, xtrack);
      metadata.put(SwathAdapter.track_name, track);
      metadata.put(SwathAdapter.geo_xtrack_name, geoXtrack);
      metadata.put(SwathAdapter.geo_track_name, geoTrack);
      metadata.put(SwathAdapter.lon_array_name, lonArray);
      metadata.put(SwathAdapter.lat_array_name, latArray);
      if ((lonArrayDims != null && latArrayDims != null)) {
         metadata.put(SwathAdapter.lon_array_dimension_names, lonArrayDims);
         metadata.put(SwathAdapter.lat_array_dimension_names, latArrayDims);
      }

      if (divideByScale) {
         metadata.put("divideByScale", "divideByScale");
      }

      SwathAdapter swathAdapter = new SwathAdapter(reader, metadata);

      return swathAdapter;
   }

   public SwathAdapter getSwathAdapter(DataChoice dataChoice) {
      return dataChoiceToAdapter.get(dataChoice);
   }

   public void doMakeDataChoice(String name, int bandIdx, int idx, DataChoice targetDataChoice, List category) {
     Hashtable subset = targetDataChoice.getProperties();
     DataChoice dataChoice = new DirectDataChoice(this, idx, name, name, category, subset);
     dataChoice.setProperties(subset);
     addDataChoice(dataChoice);
   }

   public String getDescription() {
     return "AIRS Retrvl";
   }

   public String getDateTimeStamp() {
     return " ";
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
         SwathAdapter adapter = dataChoiceToAdapter.get(dataChoice);

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

         return adapter.getData(subset);
      } catch (Exception e) {
         e.printStackTrace();
      }

      return null;
    }

}

class DimensionSet {

   String track = null;
   String xtrack = null;
   String vertical = null;   // perpendicular to (track,xtrack), altitude or pressure coords
   String channel = null;    // non spatial, spectral dimension
   String other = null;      // non sptial, index dimension 
   String geo_track = null;
   String geo_xtrack = null;

   public DimensionSet(String track, String xtrack, String vertical, String other,
                       String geo_track, String geo_xtrack) {
      this.track = track;
      this.xtrack = xtrack;
      this.geo_track = geo_track;
      this.geo_xtrack = geo_xtrack;
   }
}

class SwathInfo {

   String track = null;
   String xtrack = null;
   String geo_track = null;
   String geo_xtrack = null;
   String lonArrayName = null;
   String latArrayName = null;
   String geo_scale = null;
   String geo_offset = null;
   String[] lonDimNames = null;
   String[] latDimNames = null;
}

class FieldInfo {

   DimensionSet dimSet = null;

   String arrayName = null;
   String rangeName = null;
   String scaleName = null;
   String offsetName = null;
   String multiScaleDimensionIndex = null;
   boolean divideByScale = false;
 
   SwathInfo swathInfo = null;
}
