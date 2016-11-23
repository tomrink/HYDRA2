package edu.wisc.ssec.adapter;

import java.util.HashMap;

import visad.FlatField;
import visad.Linear2DSet;
import visad.Linear1DSet;
import visad.Set;

public class IASI_L1C_AAPP_SwathAdapter extends SwathAdapter {

   public IASI_L1C_AAPP_SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
     super(reader, metadata);

     RangeProcessor rangeProcessor = null;
     try {
        rangeProcessor = new IASI_L1C_AAPP_RangeProcessor(new RangeProcessor(reader, metadata));
       
     } catch (Exception e) {
        e.printStackTrace();
     }
     setRangeProcessor(rangeProcessor);
   }

   protected void setLengths() { // define the swath dimensions
     // define the 2D swath dimensions transforming from native (scan, EFOV, 9 IFOVs) -> (scan*2, EFOV*2) 
     int len = getTrackLength();
     setTrackLength(len *= 2);
     len = getXTrackLength();
     setXTrackLength(len *= 2);
   }

   public FlatField getData(Object subset) throws Exception {

     HashMap new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     // reform subset to integral numbers of EFOV (FORs)
     // you may not get exactly what you ask for in this case.
     // Keep the spatial coordinates in the 2D coords, but carry along the IFOV indexes.

     double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
     double[] new_coords = new double[] {2.0*Math.floor((coords[0])/2), 2.0*Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_coords);

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     new_coords = new double[] {2.0*Math.floor((coords[0])/2), 2.0*Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     new_coords = new double[] {0.0, (4.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     Set domainSet = makeDomain(new_subset);


     // transfrom the coordinates from the 2D form back to the (scan, EFOV, 4 IFOVs) form
     // which is the native storage order.  Needed for makeFlatField which accesses readArray directly.

     new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     coords = (double[]) new_subset.get(SwathAdapter.track_name);
     new_coords = new double[] {Math.floor(coords[0]/2), Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_coords);

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     new_coords = new double[] {Math.floor(coords[0]/2), Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_coords);

     new_coords = new double[] {0.0, (4.0 - 1.0), 1.0};
     new_subset.put(SpectrumAdapter.FOVindex_name, new_coords);

     FlatField swath = makeFlatField(domainSet, new_subset);

     return swath;
   }
}
