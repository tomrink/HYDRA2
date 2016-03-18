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

import java.util.HashMap;

import visad.CoordinateSystem;
import visad.FunctionType;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.Set;
import visad.Unit;

public class SwathAdapter extends MultiDimensionAdapter {

      String nav_type = "Interp";
      boolean lon_lat_trusted = true;

      private int TrackLen;
      private int XTrackLen;

      public static String longitude_name = "Longitude";
      public static String latitude_name  = "Latitude";
      public static String track_name  = "Track";
      public static String xtrack_name = "XTrack";
      public static String geo_track_name = "geo_Track";
      public static String geo_xtrack_name  = "geo_XTrack";
      public static String array_dimension_names = "array_dimension_names";
      public static String lon_array_name = "lon_array_name";
      public static String lat_array_name = "lat_array_name";
      public static String lon_array_dimension_names = "lon_array_dimension_names";
      public static String lat_array_dimension_names = "lat_array_dimension_names";
      public static String geo_track_offset_name  = "geoTrack_offset";
      public static String geo_xtrack_offset_name = "geoXTrack_offset";
      public static String geo_track_skip_name  = "geoTrack_skip";
      public static String geo_xtrack_skip_name = "geoXTrack_skip";
      public static String geo_scale_name = "geo_scale_name";
      public static String geo_offset_name = "geo_offset_name";
      public static String geo_fillValue_name = "geo_fillValue_name";
      public static String multiScaleDimensionIndex = "multiScaleDimensionIndex";
      public static String byteSegmentIndexName = "byteSegementIndexName";


      RealType track  = RealType.getRealType(track_name);
      RealType xtrack = RealType.getRealType(xtrack_name);
      RealType[] domainRealTypes = new RealType[2];

      int track_idx      = -1;
      int xtrack_idx     = -1;
      int lon_track_idx  = -1;
      int lon_xtrack_idx = -1;
      int lat_track_idx  = -1;
      int lat_xtrack_idx = -1;
      int range_rank     = -1;

      int geo_track_offset = 0;
      int geo_track_skip = 1;
      int geo_xtrack_offset = 0;
      int geo_xtrack_skip = 1;

      int track_tup_idx;
      int xtrack_tup_idx;

      private SwathNavigation navigation;
      private MultiDimensionReader geoReader;

      private Linear2DSet swathDomain;

      protected Object last_subset;

      int default_stride = 1;

      public static HashMap getEmptySubset() {
        HashMap<String, double[]> subset = new HashMap<String, double[]>();
        subset.put(track_name, new double[3]);
        subset.put(xtrack_name, new double[3]);
        return subset;
      }

      public static HashMap<String, Object> getEmptyMetadataTable() {
    	  HashMap<String, Object> metadata = new HashMap<String, Object>();
    	  metadata.put(array_name, null);
    	  metadata.put(array_dimension_names, null);
    	  metadata.put(track_name, null);
    	  metadata.put(xtrack_name, null);
    	  metadata.put(geo_track_name, null);
    	  metadata.put(geo_xtrack_name, null);
    	  metadata.put(lon_array_name, null);
    	  metadata.put(lat_array_name, null);
    	  metadata.put(lon_array_dimension_names, null);
    	  metadata.put(lat_array_dimension_names, null);
    	  metadata.put(scale_name, null);
    	  metadata.put(offset_name, null);
    	  metadata.put(fill_value_name, null);
    	  metadata.put(range_name, null);
    	  metadata.put(product_name, null);
    	  metadata.put(geo_track_offset_name, null);
    	  metadata.put(geo_xtrack_offset_name, null);
    	  metadata.put(geo_track_skip_name, null);
    	  metadata.put(geo_xtrack_skip_name, null);
          metadata.put(multiScaleDimensionIndex, null);
          metadata.put(byteSegmentIndexName, null);
    	  return metadata;
      }

      public SwathAdapter() {
      }

      public SwathAdapter(MultiDimensionReader reader, HashMap metadata, MultiDimensionReader geoReader) {
        super(reader, metadata);
        this.geoReader = reader;
        if (geoReader != null) {
           this.geoReader = geoReader;
        }
        this.init();
      }

      public SwathAdapter(MultiDimensionReader reader, HashMap metadata) {
        super(reader, metadata);
        this.geoReader = reader;
        this.init();
      }

      private void init() {
        track_idx = getIndexOfDimensionName((String)metadata.get(track_name));
        TrackLen = getDimensionLengthFromIndex(track_idx);

        xtrack_idx = getIndexOfDimensionName((String)metadata.get(xtrack_name));
        XTrackLen = getDimensionLengthFromIndex(xtrack_idx);

        int[] lengths = new int[2];

        if (track_idx < xtrack_idx) {
          domainRealTypes[0] = xtrack;
          domainRealTypes[1] = track;
          lengths[0] = XTrackLen;
          lengths[1] = TrackLen;
          track_tup_idx = 1;
          xtrack_tup_idx = 0;
        }
        else {
          domainRealTypes[0] = track;
          domainRealTypes[1] = xtrack;
          lengths[0] = TrackLen;
          lengths[1] = XTrackLen;
          track_tup_idx = 0;
          xtrack_tup_idx = 1;
        }

        setLengths();

        lengths[track_tup_idx]  = TrackLen;
        lengths[xtrack_tup_idx] = XTrackLen;

        /* TODO: RangeProcessor works as a post-process but individual swaths in an aggregation
                 may need separate processors so the logic below handles as a pre-process for
                 individual swaths prior to aggregation - improve this (too complicated):
                 the individual rangeProcessors are set into the aggregation reader.
         */
        try {
          if (!(reader instanceof GranuleAggregation)) {
             RangeProcessor rangeProcessor = RangeProcessor.createRangeProcessor(reader, metadata);
             setRangeProcessor(rangeProcessor);
          }
          else if (((GranuleAggregation)reader).getRangeProcessor(arrayName) == null) {
             RangeProcessor.createRangeProcessor(reader, metadata);
          }
        }
        catch (Exception e) {
          System.out.println("RangeProcessor failed to create");
          e.printStackTrace();
        }


        try {
          navigation = SwathNavigation.createNavigation(this);
          RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1]);
          swathDomain = new Linear2DSet(domainTupType, 0, lengths[0]-1, lengths[0], 0, lengths[1]-1, lengths[1]);
        }
        catch (Exception e) {
          System.out.println("Navigation failed to create");
          e.printStackTrace();
        }

        if (XTrackLen <= 256) {
          default_stride = 1;
        }
        else {
          default_stride = (int) XTrackLen/256;
        }
        
        /* force default stride even */
        if (default_stride > 1) {
          default_stride = (default_stride/2)*2;
        }
        
      }

      protected void setLengths() {
      }

      public int getTrackLength() {
        return TrackLen;
      }

      public int getXTrackLength() {
        return XTrackLen;
      }

      public SwathNavigation getNavigation() {
        return navigation;
      }

      public MultiDimensionReader getGeoReader() {
        return geoReader;
      }

      protected void setTrackLength(int len) {
        TrackLen = len;
      }

      protected void setXTrackLength(int len) {
        XTrackLen = len;
      }

      public String getArrayName() {
        return rangeName;
      }

      public Set makeDomain(Object subset) throws Exception {

        double[] first = new double[2];
        double[] last = new double[2];
        int[] length = new int[2];

        HashMap<String, double[]> domainSubset = new HashMap<String, double[]>();
        domainSubset.put(track_name, (double[]) ((HashMap)subset).get(track_name));
        domainSubset.put(xtrack_name, (double[]) ((HashMap)subset).get(xtrack_name));

        domainSubset.put(track_name, new double[] {0,0,0});
        domainSubset.put(xtrack_name, new double[] {0,0,0});

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

          double[] new_coords = domainSubset.get(name);
          new_coords[0] = first[kk];
          new_coords[1] = last[kk];
          new_coords[2] = coords[2];
        }
        last_subset = subset;

        Linear2DSet domainSet = new Linear2DSet(first[0], last[0], length[0], first[1], last[1], length[1]);
        CoordinateSystem cs = navigation.getVisADCoordinateSystem(domainSet, subset);

        RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1], cs, null);
        domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], first[1], last[1], length[1]);

        return domainSet;
      }

      public FunctionType getMathType() {
        return null;
      }

      public RealType[] getDomainRealTypes() {
        return domainRealTypes;
      }

      public Linear2DSet getSwathDomain() {
        return swathDomain;
      }
      
      public boolean spatialEquals(Object last_subset, Object subset) {
        double[] last_coords = (double[]) ((HashMap)last_subset).get(track_name);
        double[] coords = (double[]) ((HashMap)subset).get(track_name);

        for (int k=0; k<coords.length; k++) {
          if (coords[k] != last_coords[k]) {
            return false;
          }
        }

        last_coords = (double[]) ((HashMap)last_subset).get(xtrack_name);
        coords = (double[]) ((HashMap)subset).get(xtrack_name);

        for (int k=0; k<coords.length; k++) {
          if (coords[k] != last_coords[k]) { 
             return false;
          }
        }
      
        return true;
      }

      public void setDefaultStride(int stride) {
        default_stride = stride;
      }

      public HashMap getDefaultSubset() {
        HashMap subset = SwathAdapter.getEmptySubset();

        double[] coords = (double[])subset.get("Track");
        coords[0] = 0.0;
        coords[1] = TrackLen - 1;
        coords[2] = (double)default_stride;
        subset.put("Track", coords);

        coords = (double[])subset.get("XTrack");
        coords[0] = 0.0;
        coords[1] = XTrackLen - 1 ;
        coords[2] = (double)default_stride;
        subset.put("XTrack", coords);
        return subset;
      }
}
