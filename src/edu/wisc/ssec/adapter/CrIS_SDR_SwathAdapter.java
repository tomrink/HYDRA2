package edu.wisc.ssec.adapter;

import java.util.HashMap;

import visad.FlatField;
import visad.Linear2DSet;
import visad.Linear1DSet;
import visad.Set;

public class CrIS_SDR_SwathAdapter extends SwathAdapter {

   public CrIS_SDR_SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
      this(reader, metadata, null);
   }

   public CrIS_SDR_SwathAdapter(MultiDimensionReader reader, HashMap metadata, MultiDimensionReader geoReader) {
      this(reader, metadata, geoReader, null);
   }
   public CrIS_SDR_SwathAdapter(MultiDimensionReader reader, HashMap metadata, MultiDimensionReader geoReader, RangeProcessor rngProcessor) {
     super(reader, metadata, geoReader);

     try {
        if (rngProcessor == null) {
           rngProcessor = new RangeProcessor(reader, metadata);
        }
        RangeProcessor crisRngProcessor = new CrIS_RangeProcessor(rngProcessor);
        setRangeProcessor(crisRngProcessor);
       
     } catch (Exception e) {
        e.printStackTrace();
     }
   }

   protected void setLengths() { // define the swath dimensions
     // define the 2D swath dimensions transforming from native (scan, EFOV, 9 IFOVs) -> (scan*3, EFOV*3) 
     int len = getTrackLength();
     setTrackLength(len *= 3);
     len = getXTrackLength();
     setXTrackLength(len *= 3);
   }

   public FlatField getData(Object subset) throws Exception {

     HashMap new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     // reform subset to integral numbers of EFOV (FORs)
     // you may not get exactly what you ask for in this case.
     // Keep the spatial coordinates in the 2D coords, but carry along the IFOV indexes.

     double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
     double[] new_coords = new double[] {3.0*Math.floor((coords[0])/3), 3.0*Math.floor((coords[1]+1)/3)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_coords);

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     new_coords = new double[] {3.0*Math.floor((coords[0])/3), 3.0*Math.floor((coords[1]+1)/3)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     new_coords = new double[] {0.0, (9.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     Set domainSet = makeDomain(new_subset);


     // transfrom the coordinates from the 2D form back to the (scan, EFOV, 9 IFOVs) form
     // which is the native storage order.  Needed for makeFlatField which accesses readArray directly.

     new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     coords = (double[]) new_subset.get(SwathAdapter.track_name);
     new_coords = new double[] {Math.floor(coords[0]/3), Math.floor((coords[1]+1)/3)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_coords);

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     new_coords = new double[] {Math.floor(coords[0]/3), Math.floor((coords[1]+1)/3)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     new_coords = new double[] {0.0, (9.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     FlatField swath = makeFlatField(domainSet, new_subset);

     return swath;
   }
}
