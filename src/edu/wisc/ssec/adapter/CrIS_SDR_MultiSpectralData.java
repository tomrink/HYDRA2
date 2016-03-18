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

public class CrIS_SDR_MultiSpectralData extends MultiSpectralData {

  SwathNavigation swathNav = null;
  SwathAdapter swathAdapter = null;
  private float[][] lonlat = null;
   
  public CrIS_SDR_MultiSpectralData(SwathAdapter swathAdapter, SpectrumAdapter spectrumAdapter) {
     super(swathAdapter, spectrumAdapter, null, null);
     this.swathAdapter = swathAdapter;
     try {
        swathNav = swathAdapter.getNavigation();
        swathNav.getVisADCoordinateSystem(null, swathAdapter.getDefaultSubset());
        float[][] tmp = ((CrIS_SDR_LonLatNavigation)swathNav).getNativeLonLat();
        float[] lons = new float[tmp[0].length];
        float[] lats = new float[tmp[0].length];
        System.arraycopy(tmp[0], 0, lons, 0, lons.length);
        System.arraycopy(tmp[1], 0, lats, 0, lats.length);
        lonlat = new float[][] {lons, lats}; 
     }
     catch (Exception e) {
        e.printStackTrace();
     }
  }
  
  void setSpectrumAdapterProcessor() {
  }

  public FlatField getSpectrum(int[] coords) 
      throws Exception, VisADException, RemoteException {
    if (coords == null) return null;

    int ii = 0;
    int jj = 0;
    int kk = 0;

    double[] scan = new double[] {jj, jj, 1.0};
    double[] step = new double[] {ii, ii, 1.0};
    double[] fov = new double[] {kk, kk, 1.0};

    spectrumSelect.put(SpectrumAdapter.x_dim_name, step);
    spectrumSelect.put(SpectrumAdapter.y_dim_name, scan);
    spectrumSelect.put(SpectrumAdapter.FOVindex_name, fov);

    FlatField spectrum = spectrumAdapter.getData(spectrumSelect);
    return convertSpectrum(spectrum, paramName);
  }

  public FlatField getSpectrum(RealTuple location) 
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
           int start = jj*270 + ii*9;
           System.arraycopy(lonlat[0], start, lonlatFOV[0], 0, 9);
           System.arraycopy(lonlat[1], start, lonlatFOV[1], 0, 9);
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
       double[] scan = new double[] {jj, jj, 1.0};
       double[] step = new double[] {ii, ii, 1.0};
       double[] fov = new double[] {kk, kk, 1.0};

       spectrumSelect.put(SpectrumAdapter.x_dim_name, step);
       spectrumSelect.put(SpectrumAdapter.y_dim_name, scan);
       spectrumSelect.put(SpectrumAdapter.FOVindex_name, fov);

       FlatField spectrum = spectrumAdapter.getData(spectrumSelect);
       return convertSpectrum(spectrum, paramName);
    }
  }
}
