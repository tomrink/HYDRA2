package edu.wisc.ssec.adapter;

import java.awt.geom.Rectangle2D;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import visad.CoordinateSystem;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded2DSet;
import visad.Linear1DSet;
import visad.Linear2DSet;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.SampledSet;
import visad.Set;
import visad.SetType;
import visad.VisADException;

public class CrIS_SwathSoundingData extends SwathSoundingData {

  SwathNavigation swathNav = null;
  SwathAdapter swathAdapter = null;
  private float[][] lonlat = null;
   
  public CrIS_SwathSoundingData(SwathAdapter swathAdapter, AtmSoundingAdapter soundingAdapter) {
     super(swathAdapter, soundingAdapter, null, null);
     this.swathAdapter = swathAdapter;
     try {
        swathNav = swathAdapter.getNavigation();
        LongitudeLatitudeCoordinateSystem cs = (LongitudeLatitudeCoordinateSystem) swathNav.getVisADCoordinateSystem(null, swathAdapter.getDefaultSubset());
        Gridded2DSet gset = cs.getTheGridded2DSet();
        float[][] tmp = gset.getSamples(false);
        float[] lons = new float[tmp[0].length];
        float[] lats = new float[tmp[0].length];
        System.arraycopy(tmp[0], 0, lons, 0, lons.length);
        System.arraycopy(tmp[1], 0, lats, 0, lats.length);
        lonlat = new float[][] {lons, lats};
     }
     catch (Exception e) {
     }
  }

  public FlatField getSounding(int[] coords) 
      throws Exception, VisADException, RemoteException {
    if (coords == null) return null;

    int ii = 0;
    int jj = 0;
    int kk = 0;

    double[] scan = new double[] {jj, jj, 1.0};
    double[] step = new double[] {ii, ii, 1.0};
    double[] fov = new double[] {kk, kk, 1.0};

    soundingSelect.put(AtmSoundingAdapter.x_dim_name, step);
    soundingSelect.put(AtmSoundingAdapter.y_dim_name, scan);
    soundingSelect.put(AtmSoundingAdapter.FOVindex_name, fov);

    FlatField sounding = soundingAdapter.getData(soundingSelect);
    float[][] vals = sounding.getFloats(false);
    for (int k=0; k<vals[0].length; k++) {
       if (vals[0][k] == -9999.0) vals[0][k] = Float.NaN;
    }
    return sounding;
  }

  public FlatField getSounding(RealTuple location) 
      throws Exception, VisADException, RemoteException {

    double[] tmp = location.getValues();
    float[][] loc = new float[][] {{(float)tmp[1]}, {(float)tmp[0]}};
    if (loc[0][0] > 180) loc[0][0] -= 360;

    int kk = -1;
    int ii = 0;
    int jj = 0;

    float[][] lonlatFOV = new float[2][9];

    int trkLen = swathAdapter.getTrackLength();
    trkLen /= 3;
    scanloop: for (jj=0; jj<trkLen; jj++) {
       for (ii=0; ii<30; ii++) {
           int start = jj*270 + ii*3;
           for (int n=0; n<3; n++) {
               for (int m=0; m<3; m++) {
                   int idx = n*3 + m;
                   int k = start + n*90 + m;
                   lonlatFOV[0][idx] = lonlat[0][k];
                   lonlatFOV[1][idx] = lonlat[1][k];
               }
           }
           Gridded2DSet gsetFOV = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple, lonlatFOV, 3, 3);
           int[] idx = gsetFOV.valueToIndex(loc);
           kk = idx[0];
           if (kk >= 0) {
              break scanloop;
           }
       }
    }

    if (kk < 0) { // incoming (lon,lat) not inside any 3x3 box
      return null;
    } 
    else {
       int n = kk/3;
       int m = kk % 3;
       int i = ii*3 + m;
       int j = jj*3 + n;
       double[] scan = new double[] {j, j, 1.0};
       double[] step = new double[] {i, i, 1.0};

       soundingSelect.put(AtmSoundingAdapter.x_dim_name, step);
       soundingSelect.put(AtmSoundingAdapter.y_dim_name, scan);

       FlatField sounding = soundingAdapter.getData(soundingSelect);
       float[][] vals = sounding.getFloats(false);
       for (int k=0; k<vals[0].length; k++) {
          if (vals[0][k] == -9999.0) vals[0][k] = Float.NaN;
       }
       return sounding;
    }
  }
}
