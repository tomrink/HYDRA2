package edu.wisc.ssec.adapter;

import visad.Set;
import visad.Gridded2DSet;
import visad.Gridded2DDoubleSet;
import visad.Linear2DSet;
import visad.CoordinateSystem;
import visad.GridCoordinateSystem;
import visad.RealTupleType;
import java.util.HashMap;


public class AVHR_1B_NCDF_LonLatNavigation extends SwathNavigation  {


  public AVHR_1B_NCDF_LonLatNavigation(SwathAdapter swathAdapter) throws Exception {
    super(swathAdapter);
  }

  Gridded2DSet createInterpSet() throws Exception {
    float scale = 1E-04f;

    // TODO: need to investigate this offset to fix nav. It looks like there's one
    //       extra cross-track nav point.
    geo_start[1] = geo_start[1] + 1;

    int[] lonValues = reader.getIntArray(lon_array_name, geo_start, geo_count, geo_stride);
    int[] latValues = reader.getIntArray(lat_array_name, geo_start, geo_count, geo_stride);

    float[] lons = new float[lonValues.length];
    float[] lats = new float[latValues.length];

    for (int k=0; k<lons.length; k++) {
      lons[k] = ((float)lonValues[k])*scale;
      lats[k] = ((float)latValues[k])*scale;
    }

    Gridded2DSet gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                   new float[][] {lons, lats},
                        geo_count[idx_order[0]], geo_count[idx_order[1]],
                            null, null, null, false, false);
    return gset;
  }
}
