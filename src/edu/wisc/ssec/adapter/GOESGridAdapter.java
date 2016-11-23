package edu.wisc.ssec.adapter;

import edu.wisc.ssec.hydra.GEOSProjection;
import java.util.HashMap;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.Set;
import visad.georef.MapProjection;

public class GOESGridAdapter extends GeoSfcAdapter {

   public static String gridX_name = "GridX";
   public static String gridY_name = "GridY";

   RealType gridx = RealType.getRealType(gridX_name);
   RealType gridy = RealType.getRealType(gridY_name);
   RealType[] domainRealTypes = new RealType[2];

   int GridXLen;
   int GridYLen;

   int gridx_idx;
   int gridy_idx;
   int gridx_tup_idx;
   int gridy_tup_idx;

   MapProjection mapProj;
   Linear2DSet datasetDomain;

   double default_stride = 10;

   public static HashMap getEmptySubset() {
     HashMap<String, double[]> subset = new HashMap<String, double[]>();
     subset.put(gridY_name, new double[3]);
     subset.put(gridX_name, new double[3]);
     return subset;
   }

   public static HashMap<String, Object> getEmptyMetadataTable() {
     HashMap<String, Object> metadata = new HashMap<String, Object>();
     metadata.put(array_name, null);
     metadata.put(gridX_name, null);
     metadata.put(gridY_name, null);
     metadata.put(scale_name, null);
     metadata.put(offset_name, null);
     metadata.put(fill_value_name, null);
     metadata.put(range_name, null);
     return metadata;
   }

   public GOESGridAdapter(MultiDimensionReader reader, HashMap metadata, MapProjection mapProj, double default_stride) {
     super(reader, metadata);
     
     this.mapProj = mapProj;
     this.default_stride = default_stride;

     gridx_idx = getIndexOfDimensionName((String)metadata.get(gridX_name));
     GridXLen = getDimensionLengthFromIndex(gridx_idx);

     gridy_idx = getIndexOfDimensionName((String)metadata.get(gridY_name));
     GridYLen = getDimensionLengthFromIndex(gridy_idx);

     int[] lengths = new int[2];

     if (gridy_idx < gridx_idx) {
       domainRealTypes[0] = gridx;
       domainRealTypes[1] = gridy;
       lengths[0] = GridXLen;
       lengths[1] = GridYLen;
       gridy_tup_idx = 1;
       gridx_tup_idx = 0;
     }
     else {
       domainRealTypes[0] = gridy;
       domainRealTypes[1] = gridx;
       lengths[0] = GridYLen;
       lengths[1] = GridXLen;
       gridy_tup_idx = 0;
       gridx_tup_idx = 1;
     }

     lengths[gridy_tup_idx] = GridYLen;
     lengths[gridx_tup_idx] = GridXLen;
     
     try {
        RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1]);
        datasetDomain = new Linear2DSet(domainTupType, 0, lengths[0]-1, lengths[0], lengths[1]-1, 0, lengths[1]);
     }
     catch (Exception e) {
        e.printStackTrace();
     }
     
     try {
        setRangeProcessor(new RangeProcessor(getReader(), metadata));
     }
     catch (Exception e) {
        System.out.println("RangeProcessor failed to create.");
     }
     
   }
   
   public String getArrayName() {
      return rangeName;
   }

   public Set makeDomain(Object subset) throws Exception {
     double[] first = new double[2];
     double[] last = new double[2];
     int[] length = new int[2];

     // compute coordinates for the Linear2D domainSet
     for (int kk=0; kk<2; kk++) {
       RealType rtype = domainRealTypes[kk];
       String name = rtype.getName();
       double[] coords = (double[]) ((HashMap)subset).get(name);
       // replace with integral swath coordinates
       coords[0] = Math.ceil(coords[0]);
       coords[1] = Math.floor(coords[1]);
       first[kk] = coords[0];
       last[kk] = coords[1];
       length[kk] = (int) ((last[kk] - first[kk])/coords[2] + 1);
       last[kk] = first[kk] + (length[kk]-1)*coords[2];
       // invert south to north (GEOS) orientation (for GOES)
       if (name.equals("GridY")) { //TODO: need to a general way to handle this. Check GEOSInfo?
          double tmp = coords[0];
          coords[0] = GridYLen - coords[1];
          coords[1] = GridYLen - tmp;
       }
     }

     mapProj = new GEOSProjection((GEOSProjection)mapProj, first[0], first[1], last[0]-first[0], last[1]-first[1]);
     RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1], mapProj, null);
     //TODO: need to handle this properly GOES: North to South, GEOS: South to North
     //Linear2DSet domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], first[1], last[1], length[1]);
     Linear2DSet domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], last[1], first[1], length[1]);

     return domainSet;
   }

   public HashMap getDefaultSubset() {
     HashMap subset = GOESGridAdapter.getEmptySubset();

     double[] coords = (double[])subset.get(gridY_name);
     coords[0] = 0.0;
     coords[1] = GridYLen - 1;
     coords[2] = default_stride;
     subset.put(gridY_name, coords);

     coords = (double[])subset.get(gridX_name);
     coords[0] = 0.0;
     coords[1] = GridXLen - 1 ;
     coords[2] = default_stride;
     subset.put(gridX_name, coords);

     return subset;
  }
   
   public void setDomainSet(Linear2DSet dset) {
      // No-op
   }
   
   public Set getDatasetDomain() {
      return datasetDomain;
   }
   
   public Navigation getNavigation() {
      return null;
   }
}