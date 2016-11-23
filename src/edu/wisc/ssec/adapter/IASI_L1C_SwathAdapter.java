package edu.wisc.ssec.adapter;

import visad.Data;
import visad.FlatField;
import visad.Set;
import visad.CoordinateSystem;
import visad.RealType;
import visad.RealTupleType;
import visad.SetType;
import visad.Linear2DSet;
import visad.Unit;
import visad.FunctionType;
import visad.VisADException;
import java.rmi.RemoteException;
import java.util.Hashtable;
import java.util.HashMap;


public class IASI_L1C_SwathAdapter extends SwathAdapter {

   public IASI_L1C_SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
     super(reader, metadata);
   }

   protected void setLengths() {
     int len = getTrackLength();
     setTrackLength(len *= 2);
     len = getXTrackLength();
     setXTrackLength( len /= 2);
   }

   public FlatField getData(Object subset) throws Exception {

     HashMap new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     // reform subset to integral numbers of EFOV (FORs)
     // you may not get exactly what you ask for in this case.

     double[] coords = (double[]) new_subset.get(SwathAdapter.track_name);
     double[] new_trk_coords = new double[] {2.0*Math.floor((coords[0])/2), 2.0*Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_trk_coords);

     coords = (double[]) new_subset.get(SwathAdapter.xtrack_name);
     double[] new_xtrk_coords = new double[] {2.0*Math.floor((coords[0])/2), 2.0*Math.floor((coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_xtrk_coords);
 
     Set domainSet = makeDomain(new_subset);

     // transform the integral swath EFOV coordinates to dataset storage indexes.

     new_subset = (HashMap) ((HashMap)subset).clone();
     new_subset.putAll((HashMap)subset);

     new_trk_coords = new double[] {new_trk_coords[0]/2, ((new_trk_coords[1]+1)/2)-1, 1.0};
     new_subset.put(SwathAdapter.track_name, new_trk_coords);

     new_xtrk_coords = new double[] {2.0*new_xtrk_coords[0], (2.0*(new_xtrk_coords[1]+1))-1, 1.0};
     new_subset.put(SwathAdapter.xtrack_name, new_xtrk_coords);

     return makeFlatField(domainSet, new_subset);
   }
}
