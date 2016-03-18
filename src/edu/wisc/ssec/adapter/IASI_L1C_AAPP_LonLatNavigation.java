/*
 * This file is part of McIDAS-V
 *
 * Copyright 2007-2013
 * Space Science and Engineering Center (SSEC)
 * University of Wisconsin - Madison
 * 1225 W. Dayton Street, Madison, WI 53706, USA
 * http://www.ssec.wisc.edu/mcidas
 * 
 * All Rights Reserved
 * 
 * McIDAS-V is built on Unidata's IDV and SSEC's VisAD libraries, and
 * some McIDAS-V source code is based on IDV and VisAD source code.  
 * 
 * McIDAS-V is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Lesser Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 * 
 * McIDAS-V is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser Public License
 * along with this program.  If not, see http://www.gnu.org/licenses.
 */

package edu.wisc.ssec.adapter;

import visad.Gridded2DSet;
import visad.RealTupleType;

public class IASI_L1C_AAPP_LonLatNavigation extends SwathNavigation  {

  private Gridded2DSet gset = null;

  private float[][] lonlat = new float[2][];

  public IASI_L1C_AAPP_LonLatNavigation(SwathAdapter swathAdapter) throws Exception {
    super(swathAdapter);
  }

  Gridded2DSet createInterpSet() throws Exception {

    int[] new_geo_start = new int[3]; 
    int[] new_geo_count = new int[3]; 
    int[] new_geo_stride = new int[3]; 

    // Convert from Swath coords to native storage format
    new_geo_start[geo_xtrack_idx] = geo_start[geo_xtrack_idx]/2;
    new_geo_count[geo_xtrack_idx] = geo_count[geo_xtrack_idx]/2;
    new_geo_stride[geo_xtrack_idx] = 1;

    new_geo_start[geo_track_idx] = geo_start[geo_track_idx]/2;
    new_geo_count[geo_track_idx] = geo_count[geo_track_idx]/2;
    new_geo_stride[geo_track_idx] = 1;

    new_geo_start[2] = 0;
    new_geo_count[2] = 4;
    new_geo_stride[2] = 1;

    // read the lonlats in native storage order. Keep separately.
    int[] ilons = reader.getIntArray(lon_array_name, new_geo_start, new_geo_count, new_geo_stride);
    int[] ilats = reader.getIntArray(lat_array_name, new_geo_start, new_geo_count, new_geo_stride);
    float[] lons = new float[ilons.length];
    float[] lats = new float[ilats.length];
    for (int i=0; i<lons.length; i++) {
        lons[i] = ilons[i]*1.0E-04f;
        lats[i] = ilats[i]*1.0E-04f;
    }
    lonlat[0] = lons;
    lonlat[1] = lats;

    // Convert back to Swath coords from native storage coords
    new_geo_count[geo_xtrack_idx] = 2*new_geo_count[geo_xtrack_idx];
    new_geo_count[geo_track_idx] = 2*new_geo_count[geo_track_idx];

    lons = IASI_L1C_Utility.psuedoScanReorder2(lons, new_geo_count[geo_xtrack_idx], new_geo_count[geo_track_idx]);
    lats = IASI_L1C_Utility.psuedoScanReorder2(lats, new_geo_count[geo_xtrack_idx], new_geo_count[geo_track_idx]);

    gset = new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                   new float[][] {lons, lats},
                        geo_count[idx_order[0]], geo_count[idx_order[1]],
                            null, null, null, false, false);
    return gset;
  }

  public Gridded2DSet getInterpSet() {
    return gset;
  }

  public float[][] getNativeLonLat() {
    return lonlat;
  }
}
