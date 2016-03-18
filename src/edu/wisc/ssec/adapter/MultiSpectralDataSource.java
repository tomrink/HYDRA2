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

import java.io.File;

import java.rmi.RemoteException;

import java.lang.Number;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import ucar.unidata.data.DataCategory;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;
import ucar.unidata.data.DataSelectionComponent;
import ucar.unidata.data.DataSourceDescriptor;
import ucar.unidata.data.DirectDataChoice;
import ucar.unidata.data.GeoLocationInfo;
import ucar.unidata.data.GeoSelection;
import ucar.unidata.util.Misc;

import visad.Data;
import visad.FlatField;
import visad.VisADException;
import visad.FunctionType;
import visad.RealType;
import visad.RealTupleType;
import visad.Linear2DSet;
import visad.Gridded2DSet;
import visad.CoordinateSystem;
import visad.CommonUnit;
import visad.SetType;
import visad.georef.MapProjection;

//import edu.wisc.ssec.mcidasv.data.ComboDataChoice;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * A data source for Multi Dimension Data 
 */

public class MultiSpectralDataSource extends HydraDataSource {

	/** Sources file */
    protected String filename;

    protected MultiDimensionReader reader;

    protected MultiDimensionReader geoReader;

    protected MultiDimensionAdapter[] adapters = null;

    private static final String DATA_DESCRIPTION = "Multi Dimension Data";


    private HashMap defaultSubset;
    private SwathAdapter swathAdapter;
    private SpectrumAdapter spectrumAdapter;
    private MultiSpectralData multiSpectData;

    private ArrayList<MultiSpectralData> multiSpectData_s = new ArrayList<MultiSpectralData>();
    private HashMap<String, MultiSpectralData> adapterMap = new HashMap<String, MultiSpectralData>();

    private List categories;
    private boolean hasImagePreview = false;
    private boolean hasChannelSelect = false;

    private boolean doAggregation = false;

    //private ComboDataChoice comboChoice;

    private FlatField previewImage = null;

    public static final String paramKey = "paramKey";

    private String dateTime = null;

    /**
     * Zero-argument constructor for construction via unpersistence.
     */
    public MultiSpectralDataSource() {}

    public MultiSpectralDataSource(String fileName) throws VisADException {
      this(null, Misc.newList(fileName), null);
    }

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  fileName  name of the hdf file to read
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public MultiSpectralDataSource(DataSourceDescriptor descriptor,
                                 String fileName, Hashtable properties)
            throws VisADException {
        this(descriptor, Misc.newList(fileName), properties);
    }

    /**
     * Construct a new HYDRA hdf data source.
     * @param  descriptor  descriptor for this <code>DataSource</code>
     * @param  sources   List of filenames
     * @param  properties  hashtable of properties
     *
     * @throws VisADException problem creating data
     */
    public MultiSpectralDataSource(DataSourceDescriptor descriptor,
                                 List newSources, Hashtable properties)
            throws VisADException {
        super(descriptor, newSources, DATA_DESCRIPTION, properties);

        this.filename = (String)sources.get(0);

        try {
          setup();
        }
        catch (Exception e) {
          e.printStackTrace();
          throw new VisADException();
        }
    }

    public void setup() throws Exception {
        String name = (new File(filename)).getName();
    	// aggregations will use sets of NetCDFFile readers
    	ArrayList<NetCDFFile> ncdfal = new ArrayList<NetCDFFile>();

        try {
          if (name.startsWith("NSS.HRPT.NP") && name.endsWith("obs.hdf")) { // get file union
            String other = new String(filename);
            other = other.replace("obs", "nav");
            reader = NetCDFFile.makeUnion(filename, other);
          }
          else {
        	  if (sources.size() > 1) {
        		  for (int i = 0; i < sources.size(); i++) {
        			  String s = (String) sources.get(i);
        			  ncdfal.add(new NetCDFFile(s));
        		  }
        		  doAggregation = true;
        	  } else {
        		  reader = new NetCDFFile(filename);
        	  }
          }
        }
        catch (Exception e) {
		e.printStackTrace();
		System.out.println("cannot create NetCDF reader for file: "+filename);
        }
                                                                                                                                                     
        Hashtable<String, String[]> properties = new Hashtable<String, String[]>(); 

        multiSpectData_s.clear();

        if ( name.startsWith("AIRS")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "L1B_AIRS_Science/Data_Fields/radiances");
          table.put(SpectrumAdapter.range_name, "radiances");
          table.put(SpectrumAdapter.channelIndex_name, "Channel");
          table.put(SpectrumAdapter.ancillary_file_name, "/edu/wisc/ssec/adapter/resources/airs/L2.chan_prop.2003.11.19.v6.6.9.anc");
          table.put(SpectrumAdapter.x_dim_name, "GeoXTrack");
          table.put(SpectrumAdapter.y_dim_name, "GeoTrack");
          spectrumAdapter = new AIRS_L1B_Spectrum(reader, table);
                                                                                                                                                     
          table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "L1B_AIRS_Science/Data_Fields/radiances");
          table.put(SwathAdapter.range_name, "radiances");
          table.put("lon_array_name", "L1B_AIRS_Science/Geolocation_Fields/Longitude");
          table.put("lat_array_name", "L1B_AIRS_Science/Geolocation_Fields/Latitude");
          table.put("XTrack", "GeoXTrack");
          table.put("Track", "GeoTrack");
          table.put("geo_Track", "GeoTrack");
          table.put("geo_XTrack", "GeoXTrack");
          table.put(SpectrumAdapter.channelIndex_name, "Channel"); //- think about this?

          swathAdapter = new SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter);
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
          hasChannelSelect = true;
          multiSpectData.init_wavenumber = 919.5f; 
          multiSpectData_s.add(multiSpectData);
          // get date time for this granule
          String mmStr;
          String ddStr;
          String hhStr;
          String minStr;
          Number yyyy = reader.getAttributeValue("L1B_AIRS_Science/Swath_Attributes", "start_year");
          Number mm = reader.getAttributeValue("L1B_AIRS_Science/Swath_Attributes", "start_month");
          if (mm.intValue() <= 9)  {
             mmStr = "0"+mm;
          } else {
             mmStr = mm.toString();
          }
          Number dd = reader.getAttributeValue("L1B_AIRS_Science/Swath_Attributes", "start_day");
          if (dd.intValue() <= 9)  {
             ddStr = "0"+dd;
          } else {
             ddStr = dd.toString();
          }
          Number hh = reader.getAttributeValue("L1B_AIRS_Science/Swath_Attributes", "start_hour");
          if (hh.intValue() <= 9)  {
             hhStr = "0"+hh;
          } else {
             hhStr = hh.toString();
          }
          Number min = reader.getAttributeValue("L1B_AIRS_Science/Swath_Attributes", "start_minute");
          if (min.intValue() <= 9)  {
             minStr = "0"+min;
          } else {
             minStr = min.toString();
          }
         
          dateTime = yyyy+"/"+mmStr+"/"+ddStr+"  "+hhStr+":"+minStr+"Z";
       }
       else if ( name.startsWith("IASI_xxx_1C") && name.endsWith("h5")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_DATA");
          table.put(SpectrumAdapter.range_name, "radiances");
          table.put(SpectrumAdapter.channelIndex_name, "dim2");
          table.put(SpectrumAdapter.x_dim_name, "dim1");
          table.put(SpectrumAdapter.y_dim_name, "dim0");
          spectrumAdapter = new IASI_L1C_Spectrum(reader, table);
                                                                                                                                             
          table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_DATA");
          table.put("range_name", "radiances");
          table.put("lon_array_name", "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_LON_ARRAY");
          table.put("lat_array_name", "U-MARF/EPS/IASI_xxx_1C/DATA/SPECT_LAT_ARRAY");
          table.put("XTrack", "dim1");
          table.put("Track", "dim0");
          table.put("geo_XTrack", "dim1");
          table.put("geo_Track", "dim0");
          table.put("product_name", "IASI_L1C_xxx");
          table.put(SpectrumAdapter.channelIndex_name, "dim2");
          swathAdapter = new IASI_L1C_SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter);
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          multiSpectData.init_wavenumber = 919.5f; 
          hasChannelSelect = true;
          multiSpectData_s.add(multiSpectData);
       }
       else if ( name.contains("IASI_C") && name.endsWith(".nc")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "spectral_radiance");
          table.put(SpectrumAdapter.range_name, "radiances");
          table.put(SpectrumAdapter.channelIndex_name, "spectral");
          table.put(SpectrumAdapter.x_dim_name, "across_track");
          table.put(SpectrumAdapter.y_dim_name, "along_track");
          table.put(SpectrumAdapter.channels_name, "wavenumber");
          spectrumAdapter = new IASI_L1C_NCDF_Spectrum(reader, table);
                                                                                                                                             
          table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "spectral_radiance");
          table.put("range_name", "radiances");
          table.put("lon_array_name", "lon");
          table.put("lat_array_name", "lat");
          table.put("XTrack", "across_track");
          table.put("Track", "along_track");
          table.put("geo_XTrack", "across_track");
          table.put("geo_Track", "along_track");
          table.put("product_name", "IASI_L1C_ncdf");
          table.put(SpectrumAdapter.channelIndex_name, "spectral");
          swathAdapter = new IASI_L1C_SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter);
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          multiSpectData.init_wavenumber = 919.5f; 
          hasChannelSelect = true;
          multiSpectData_s.add(multiSpectData);
       }
       else if (name.startsWith("iasil1c") && name.endsWith(".h5")) { //AAPP
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "Data/scalrad");
          table.put(SpectrumAdapter.range_name, "radiances");
          table.put(SpectrumAdapter.channelIndex_name, "dim3");
          table.put(SpectrumAdapter.FOVindex_name, "dim2");
          table.put(SpectrumAdapter.x_dim_name, "dim1");
          table.put(SpectrumAdapter.y_dim_name, "dim0");
          spectrumAdapter = new IASI_L1C_AAPP_Spectrum(reader, table);     
          
          table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "Data/scalrad");
          table.put("range_name", "radiances");
          table.put("lon_array_name", "Geolocation/Longitude");
          table.put("lat_array_name", "Geolocation/Latitude");
          table.put("XTrack", "dim1");
          table.put("Track", "dim0");
          table.put(SpectrumAdapter.FOVindex_name, "dim2");
          table.put("geo_XTrack", "dim1");
          table.put("geo_Track", "dim0");
          table.put("product_name", "IASI_L1C_AAPP");
          table.put(SpectrumAdapter.channelIndex_name, "dim3");
          swathAdapter = new IASI_L1C_AAPP_SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter);
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          multiSpectData.init_wavenumber = 919.5f; 
          hasChannelSelect = true;
          multiSpectData_s.add(multiSpectData);
       }
       else if ( name.startsWith("IASI")) {
          HashMap table = SpectrumAdapter.getEmptyMetadataTable();
          table.put(SpectrumAdapter.array_name, "observations");
          table.put(SpectrumAdapter.channelIndex_name, "obsChannelIndex");
          table.put(SpectrumAdapter.x_dim_name, "obsElement");
          table.put(SpectrumAdapter.y_dim_name, "obsLine");
          table.put(SpectrumAdapter.channels_name, "observationChannels");
          spectrumAdapter = new SpectrumAdapter(reader, table);

          table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "observations");
          table.put("lon_array_name", "obsLongitude");
          table.put("lat_array_name", "obsLatitude");
          table.put("XTrack", "obsElement");
          table.put("Track", "obsLine");
          table.put("geo_XTrack", "obsElement");
          table.put("geo_Track", "obsLine");
          table.put(SpectrumAdapter.channelIndex_name, "obsChannelIndex"); //- think about this?
          swathAdapter = new SwathAdapter(reader, table);
          HashMap subset = swathAdapter.getDefaultSubset();
          subset.put(SpectrumAdapter.channelIndex_name, new double[] {793,793,1});
          defaultSubset = subset;
          multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter);
          DataCategory.createCategory("MultiSpectral");
          categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;");
          multiSpectData.init_wavenumber = 919.5f; 
          multiSpectData_s.add(multiSpectData);
          hasChannelSelect = true;
       }
       else if (name.startsWith("MOD021KM") || name.startsWith("MYD021KM") || 
               (name.startsWith("a1") && (name.indexOf("1000m") > 0)) || 
               (name.startsWith("t1") && (name.indexOf("1000m") > 0)) ) {
         Date date = getMODISdateFromFilename(name);
         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_Emissive");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Latitude");
         //table.put("lon_array_name", "MODIS_Swath_Type_GEO/Geolocation_Fields/Longitude");
         //table.put("lat_array_name", "MODIS_Swath_Type_GEO/Geolocation_Fields/Latitude");
         table.put("XTrack", "Max_EV_frames");
         table.put("Track", "10*nscans");
         table.put("geo_Track", "2*nscans");
         table.put("geo_XTrack", "1KM_geo_dim");
         //table.put("geo_Track", "nscans*10");
         //table.put("geo_XTrack", "mframes");
         table.put("scale_name", "radiance_scales");
         table.put("offset_name", "radiance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Emissive_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_Emissive");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));
         
         // initialize the aggregation reader object
         if (doAggregation) {
        	 try {
        		 reader = new GranuleAggregation(ncdfal, "10*nscans");
                         geoReader = new GranuleAggregation(ncdfal, "2*nscans");
        	 } catch (Exception e) {
        		 throw new VisADException("Unable to initialize aggregation reader");
        	 }
         }

         swathAdapter = new SwathAdapter(reader, table, geoReader);
         swathAdapter.setDefaultStride(10);

         HashMap subset = swathAdapter.getDefaultSubset();

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_Emissive");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_Emissive");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
           {3.799f,3.992f,3.968f,4.070f,4.476f,4.549f,6.784f,7.345f,8.503f,
            9.700f,11.000f,12.005f,13.351f,13.717f,13.908f,14.205f});
         table.put(SpectrumAdapter.bandNames, new String[] 
           {"20","21","22","23","24","25","27","28","29",
            "30","31","32","33","34","35","36"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, table);

         multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter, "MODIS", "Aqua");
         multiSpectData.setInitialWavenumber(11.0f);
         defaultSubset = multiSpectData.getDefaultSubset();

         //previewImage = multiSpectData.getImage(defaultSubset);
         multiSpectData_s.add(multiSpectData);

         //--- aggregate reflective bands
         table = SwathAdapter.getEmptyMetadataTable();

         table.put("array_name", "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Latitude");
         table.put("XTrack", "Max_EV_frames");
         table.put("Track", "10*nscans");
         table.put("geo_Track", "2*nscans");
         table.put("geo_XTrack", "1KM_geo_dim");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "EV_1KM_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_RefSB");

         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));

         SwathAdapter sadapt0 = new SwathAdapter(reader, table, geoReader);
         sadapt0.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_1KM_RefSB");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.412f,.450f,.487f,.531f,.551f,.666f,.668f,.677f,.679f,.748f,
             .869f,.905f,.936f,.940f,1.375f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"8","9","10","11","12","13lo","13hi","14lo","14hi","15",
             "16","17","18","19","26"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter specadap0 = new SpectrumAdapter(reader, table);
         MultiSpectralData multispec0 = new MultiSpectralData(sadapt0, specadap0, "Reflectance", "Reflectance", "MODIS", "Aqua", date);

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;

         table = SwathAdapter.getEmptyMetadataTable();

         table.put("array_name", "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Latitude");
         table.put("XTrack", "Max_EV_frames");
         table.put("Track", "10*nscans");
         table.put("geo_Track", "2*nscans");
         table.put("geo_XTrack", "1KM_geo_dim");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");

         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));

         SwathAdapter sadapt1 = new SwathAdapter(reader, table, geoReader);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr1km_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.650f,.855f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"1","2"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter specadap1 = new SpectrumAdapter(reader, table);
         MultiSpectralData multispec1 = new MultiSpectralData(sadapt1, specadap1, "Reflectance", "Reflectance", "MODIS", "Aqua", date);

         table = SwathAdapter.getEmptyMetadataTable();

         table.put("array_name", "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_Aggr1km_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Latitude");
         table.put("XTrack", "Max_EV_frames");
         table.put("Track", "10*nscans");
         table.put("geo_Track", "2*nscans");
         table.put("geo_XTrack", "1KM_geo_dim");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "EV_500_Aggr1km_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_500M");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(5.0));
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));


         SwathAdapter sadapt2 = new SwathAdapter(reader, table, geoReader);


         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_Aggr1km_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_500M");
         table.put(SpectrumAdapter.x_dim_name, "Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "10*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.470f,.555f,1.240f,1.638f,2.130f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"3","4","5","6","7"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter specadap2 = new SpectrumAdapter(reader, table);
         MultiSpectralData multispec2 = new MultiSpectralData(sadapt2, specadap2, "Reflectance", "Reflectance", "MODIS", "Aqua", date);

         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multispec1, multispec2, multispec0});
         aggr.setInitialWavenumber(0.650f);
         aggr.setDataRange(new float[] {0f, 0.8f});
         multiSpectData_s.add(aggr);
       }
       else if (name.startsWith("MOD02QKM") || name.startsWith("MYD02QKM") ||
               (name.startsWith("a1") && (name.indexOf("250m") > 0)) ||
               (name.startsWith("t1") && (name.indexOf("250m") > 0)) ) {
         Date date = getMODISdateFromFilename(name);
         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Latitude");
         table.put("XTrack", "4*Max_EV_frames");
         table.put("Track", "40*nscans");
         table.put("geo_Track", "10*nscans");
         table.put("geo_XTrack", "Max_EV_frames");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(4.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(4.0));
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));
         // initialize the aggregation reader object
         if (doAggregation) {
                 try {
                         reader = new GranuleAggregation(ncdfal, "40*nscans");
                         geoReader = new GranuleAggregation(ncdfal, "10*nscans");
                 } catch (Exception e) {
                         throw new VisADException("Unable to initialize aggregation reader");
                 }
         }
         swathAdapter = new SwathAdapter(reader, table, geoReader);
         swathAdapter.setDefaultStride(40);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SpectrumAdapter.x_dim_name, "4*Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "40*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.650f,.855f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"1","2"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, table);

         multiSpectData = new MultiSpectralData(swathAdapter, spectrumAdapter, "Reflectance", "Reflectance", "MODIS", "Aqua", date);
         multiSpectData.setInitialWavenumber(0.650f);
         multiSpectData.setDataRange(new float[] {0f, 0.8f});
         defaultSubset = multiSpectData.getDefaultSubset();
         //previewImage = multiSpectData.getImage(defaultSubset);
         multiSpectData_s.add(multiSpectData);

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;

         multiSpectData_s.add(null);
       }
       else if (name.startsWith("MOD02HKM") || name.startsWith("MYD02HKM") ||
               (name.startsWith("a1") && (name.indexOf("500m") > 0)) ||
               (name.startsWith("t1") && (name.indexOf("500m") > 0)) ) {
         Date date = getMODISdateFromFilename(name);
         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr500_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Latitude");
         //table.put("solarZenith_array_name", "MODIS_SWATH_Type_L1B/Data_Fields/SolarZenith");
         table.put("XTrack", "2*Max_EV_frames");
         table.put("Track", "20*nscans");
         table.put("geo_Track", "10*nscans");
         table.put("geo_XTrack", "Max_EV_frames");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(2.0));
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));

         // initialize the aggregation reader object
         if (doAggregation) {
                 try {
                         reader = new GranuleAggregation(ncdfal, "20*nscans");
                         geoReader = new GranuleAggregation(ncdfal, "10*nscans");
                 } catch (Exception e) {
                         throw new VisADException("Unable to initialize aggregation reader");
                 }
         }


         SwathAdapter swathAdapter0 = new SwathAdapter(reader, table, geoReader);
         swathAdapter0.setDefaultStride(20);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data_Fields/EV_250_Aggr500_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_250M");
         table.put(SpectrumAdapter.x_dim_name, "2*Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "20*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.650f,.855f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"1","2"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", "MODIS", "Aqua", date);

         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_RefSB");
         table.put("lon_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Longitude");
         table.put("lat_array_name", "MODIS_SWATH_Type_L1B/Geolocation_Fields/Latitude");
         //table.put("solarZenith_array_name", "MODIS_SWATH_Type_L1B/Data_Fields/SolarZenith");
         table.put("XTrack", "2*Max_EV_frames");
         table.put("Track", "20*nscans");
         table.put("geo_Track", "10*nscans");
         table.put("geo_XTrack", "Max_EV_frames");
         table.put("scale_name", "reflectance_scales");
         table.put("offset_name", "reflectance_offsets");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands");
         table.put(SpectrumAdapter.channelIndex_name, "Band_500M");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(2.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(2.0));
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));

         SwathAdapter swathAdapter1 = new SwathAdapter(reader, table, geoReader);
         swathAdapter1.setDefaultStride(20);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "MODIS_SWATH_Type_L1B/Data_Fields/EV_500_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "Band_500M");
         table.put(SpectrumAdapter.x_dim_name, "2*Max_EV_frames");
         table.put(SpectrumAdapter.y_dim_name, "20*nscans");
         table.put(SpectrumAdapter.channelValues, new float[]
            {.470f,.555f,1.240f,1.638f,2.130f});
         table.put(SpectrumAdapter.bandNames, new String[]
            {"3","4","5","6","7"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", "MODIS", "Aqua", date);

         MultiSpectralAggr aggr = 
            new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1});
         aggr.setInitialWavenumber(0.650f);
         aggr.setDataRange(new float[] {0f, 0.8f});
         multiSpectData_s.add(aggr);
         multiSpectData = aggr;
         defaultSubset = aggr.getDefaultSubset();
         //previewImage = aggr.getImage(defaultSubset);

         DataCategory.createCategory("MultiSpectral");
         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
         hasImagePreview = true;
         hasChannelSelect = true;

         multiSpectData_s.add(null);
       }
       else if (name.startsWith("AVHR_xxx_1B") && name.endsWith(".h5")) {
         HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel3ab");
         swthTable.put("lon_array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/IMAGE_LON_ARRAY");
         swthTable.put("lat_array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/IMAGE_LAT_ARRAY");
         swthTable.put("XTrack", "dim1");
         swthTable.put("Track", "dim0");
         swthTable.put("geo_Track", "dim0");
         swthTable.put("geo_XTrack", "dim1");
         swthTable.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         swthTable.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         swthTable.put(SwathAdapter.geo_track_skip_name, Double.toString(1.0));
         swthTable.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(20.0));
         swthTable.put("product_name", "AVHR_EPS_xxx_1B");
         swthTable.put("range_name", "Emissive_Bands");


         SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setRangeProcessor(new AVHR_xxx_L1_RangeProcessor(reader, "CH3B"));
         swathAdapter0.setDefaultStride(10);
         HashMap subset = swathAdapter0.getDefaultSubset();
         defaultSubset = subset;

         HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel3ab");
         specTable.put(SpectrumAdapter.x_dim_name, "dim1");
         specTable.put(SpectrumAdapter.y_dim_name, "dim0");
         specTable.put(SpectrumAdapter.channelValues, new float[] {3.740f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch3b"});
         specTable.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "BrightnessTemp", "BrightnessTemp", null, null);

         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel4");
         table.put("lon_array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/IMAGE_LON_ARRAY");
         table.put("lat_array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/IMAGE_LAT_ARRAY");
         table.put("XTrack", "dim1");
         table.put("Track", "dim0");
         table.put("geo_Track", "dim0");
         table.put("geo_XTrack", "dim1");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(1.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(20.0));
         table.put("product_name", "AVHR_EPS_xxx_1B");
         table.put("range_name", "Emissive_Bands");


         SwathAdapter swathAdapter1 = new SwathAdapter(reader, table);
         swathAdapter1.setRangeProcessor(new AVHR_xxx_L1_RangeProcessor(reader, "CH4"));
         swathAdapter1.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel4");
         table.put(SpectrumAdapter.x_dim_name, "dim1");
         table.put(SpectrumAdapter.y_dim_name, "dim0");
         table.put(SpectrumAdapter.channelValues, new float[] {10.80f});
         table.put(SpectrumAdapter.bandNames, new String[] {"ch4"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "BrightnessTemp", "BrightnessTemp", null, null);

         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel5");
         table.put("lon_array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/IMAGE_LON_ARRAY");
         table.put("lat_array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/IMAGE_LAT_ARRAY");
         table.put("XTrack", "dim1");
         table.put("Track", "dim0");
         table.put("geo_Track", "dim0");
         table.put("geo_XTrack", "dim1");
         table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         table.put(SwathAdapter.geo_track_skip_name, Double.toString(1.0));
         table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(20.0));
         table.put("product_name", "AVHR_EPS_xxx_1B");
         table.put("range_name", "Emissive_Bands");


         SwathAdapter swathAdapter2 = new SwathAdapter(reader, table);
         swathAdapter2.setRangeProcessor(new AVHR_xxx_L1_RangeProcessor(reader, "CH5"));
         swathAdapter2.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel5");
         table.put(SpectrumAdapter.x_dim_name, "dim1");
         table.put(SpectrumAdapter.y_dim_name, "dim0");
         table.put(SpectrumAdapter.channelValues, new float[] {12.00f});
         table.put(SpectrumAdapter.bandNames, new String[] {"ch5"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "BrightnessTemp", "BrightnessTemp", null, null);


         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2});
         aggr.setInitialWavenumber(10.8f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
         defaultSubset = aggr.getDefaultSubset();

         //- now do the reflective bands
         swthTable.put("array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel1");
         swthTable.put("range_name", "Reflective_Bands");

         swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setRangeProcessor(new AVHR_xxx_L1_RangeProcessor(reader, "CH1"));
         swathAdapter0.setDefaultStride(10);

         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel1");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.630f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch1"});
         spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", null, null);

         swthTable.put("array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel2");
         swthTable.put("range_name", "Reflective_Bands");
         
         swathAdapter1 = new SwathAdapter(reader, swthTable);
         swathAdapter1.setRangeProcessor(new AVHR_xxx_L1_RangeProcessor(reader, "CH2"));
         swathAdapter1.setDefaultStride(10);
         
         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel2");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.862f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch2"});
         spectrumAdapter1 = new SpectrumAdapter(reader, specTable);

         multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", null, null);

         swthTable.put("array_name", "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel3ab");
         swthTable.put("range_name", "Reflective_Bands");

         swathAdapter2 = new SwathAdapter(reader, swthTable);
         swathAdapter2.setRangeProcessor(new AVHR_xxx_L1_RangeProcessor(reader, "CH3A"));
         swathAdapter2.setDefaultStride(10);

         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/AVHR_xxx_1B/DATA/Channel3ab");
         specTable.put(SpectrumAdapter.channelValues, new float[] {1.610f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch3a"});
         spectrumAdapter2 = new SpectrumAdapter(reader, specTable);

         multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "Reflectance", "Reflectance", null, null);


         aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2});
         aggr.setInitialWavenumber(0.630f);
         aggr.setDataRange(new float[] {0f, 100f});
         multiSpectData_s.add(aggr);

         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
       }
       else if (name.contains("AVHR_C") && name.endsWith(".nc")) {
         HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "scene_radiances3b");
         swthTable.put("lon_array_name", "lon");
         swthTable.put("lat_array_name", "lat");
         swthTable.put("XTrack", "across_track");
         swthTable.put("Track", "along_track");
         swthTable.put("geo_Track", "along_track");
         swthTable.put("geo_XTrack", "across_track");
         swthTable.put("scale_name", "scale_factor");
         swthTable.put("fill_value_name", "_FillValue");
         swthTable.put("product_name", "AVHR_1B_NCDF");
         swthTable.put("range_name", "Emissive_Bands");


         SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setRangeProcessor(new AVHR_ncdf_L1_RangeProcessor(reader, swthTable, "CH3B"));
         swathAdapter0.setDefaultStride(10);
         HashMap subset = swathAdapter0.getDefaultSubset();
         defaultSubset = subset;

         HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "scene_radiances3b");
         specTable.put(SpectrumAdapter.x_dim_name, "across_track");
         specTable.put(SpectrumAdapter.y_dim_name, "along_track");
         specTable.put(SpectrumAdapter.channelValues, new float[] {3.740f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"3b"});
         specTable.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "BrightnessTemp", "BrightnessTemp", null, null);

         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "scene_radiances4");
         table.put("lon_array_name", "lon");
         table.put("lat_array_name", "lat");
         table.put("XTrack", "across_track");
         table.put("Track", "along_track");
         table.put("geo_Track", "along_track");
         table.put("geo_XTrack", "across_track");
         table.put("scale_name", "scale_factor");
         table.put("fill_value_name", "_FillValue");
         table.put("product_name", "AVHR_1B_NCDF");
         table.put("range_name", "Emissive_Bands");


         SwathAdapter swathAdapter1 = new SwathAdapter(reader, table);
         swathAdapter1.setRangeProcessor(new AVHR_ncdf_L1_RangeProcessor(reader, table, "CH4"));
         swathAdapter1.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "scene_radiances4");
         table.put(SpectrumAdapter.x_dim_name, "across_track");
         table.put(SpectrumAdapter.y_dim_name, "along_track");
         table.put(SpectrumAdapter.channelValues, new float[] {10.80f});
         table.put(SpectrumAdapter.bandNames, new String[] {"4"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "BrightnessTemp", "BrightnessTemp", null, null);

         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "scene_radiances5");
         table.put("lon_array_name", "lon");
         table.put("lat_array_name", "lat");
         table.put("XTrack", "across_track");
         table.put("Track", "along_track");
         table.put("geo_Track", "along_track");
         table.put("geo_XTrack", "across_track");
         table.put("scale_name", "scale_factor");
         table.put("fill_value_name", "_FillValue");
         table.put("product_name", "AVHR_1B_NCDF");
         table.put("range_name", "Emissive_Bands");


         SwathAdapter swathAdapter2 = new SwathAdapter(reader, table);
         swathAdapter2.setRangeProcessor(new AVHR_ncdf_L1_RangeProcessor(reader, table, "CH5"));
         swathAdapter2.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "scene_radiances5");
         table.put(SpectrumAdapter.x_dim_name, "across_track");
         table.put(SpectrumAdapter.y_dim_name, "along_track");
         table.put(SpectrumAdapter.channelValues, new float[] {12.00f});
         table.put(SpectrumAdapter.bandNames, new String[] {"5"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "BrightnessTemp", "BrightnessTemp", null, null);


         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2});
         aggr.setInitialWavenumber(10.8f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
         defaultSubset = aggr.getDefaultSubset();

         //- now do the reflective bands
         swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "scene_radiances1");
         swthTable.put("lon_array_name", "lon");
         swthTable.put("lat_array_name", "lat");
         swthTable.put("XTrack", "across_track");
         swthTable.put("Track", "along_track");
         swthTable.put("geo_Track", "along_track");
         swthTable.put("geo_XTrack", "across_track");
         swthTable.put("scale_name", "scale_factor");
         swthTable.put("fill_value_name", "_FillValue");
         swthTable.put("product_name", "AVHR_1B_NCDF");
         swthTable.put("range_name", "Reflective_Bands");

         swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setRangeProcessor(new AVHR_ncdf_L1_RangeProcessor(reader, swthTable, "CH1"));
         swathAdapter0.setDefaultStride(10);

         specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "scene_radiances1");
         specTable.put(SpectrumAdapter.x_dim_name, "across_track");
         specTable.put(SpectrumAdapter.y_dim_name, "along_track");
         specTable.put(SpectrumAdapter.channelType, "wavelength");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.630f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"1"});
         spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", null, null);


         swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "scene_radiances2");
         swthTable.put("lon_array_name", "lon");
         swthTable.put("lat_array_name", "lat");
         swthTable.put("XTrack", "across_track");
         swthTable.put("Track", "along_track");
         swthTable.put("geo_Track", "along_track");
         swthTable.put("geo_XTrack", "across_track");
         swthTable.put("scale_name", "scale_factor");
         swthTable.put("fill_value_name", "_FillValue");
         swthTable.put("product_name", "AVHR_1B_NCDF");
         swthTable.put("range_name", "Reflective_Bands");
         
         swathAdapter1 = new SwathAdapter(reader, swthTable);
         swathAdapter1.setRangeProcessor(new AVHR_ncdf_L1_RangeProcessor(reader, swthTable, "CH2"));
         swathAdapter1.setDefaultStride(10);
         
         specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "scene_radiances2");
         specTable.put(SpectrumAdapter.x_dim_name, "across_track");
         specTable.put(SpectrumAdapter.y_dim_name, "along_track");
         specTable.put(SpectrumAdapter.channelType, "wavelength");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.862f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"2"});
         spectrumAdapter1 = new SpectrumAdapter(reader, specTable);

         multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", null, null);

         swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "scene_radiances3a");
         swthTable.put("lon_array_name", "lon");
         swthTable.put("lat_array_name", "lat");
         swthTable.put("XTrack", "across_track");
         swthTable.put("Track", "along_track");
         swthTable.put("geo_Track", "along_track");
         swthTable.put("geo_XTrack", "across_track");
         swthTable.put("scale_name", "scale_factor");
         swthTable.put("fill_value_name", "_FillValue");
         swthTable.put("product_name", "AVHR_1B_NCDF");
         swthTable.put("range_name", "Reflective_Bands");

         swathAdapter2 = new SwathAdapter(reader, swthTable);
         swathAdapter2.setRangeProcessor(new AVHR_ncdf_L1_RangeProcessor(reader, swthTable, "CH3A"));
         swathAdapter2.setDefaultStride(10);


         specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "scene_radiances3a");
         specTable.put(SpectrumAdapter.x_dim_name, "across_track");
         specTable.put(SpectrumAdapter.y_dim_name, "along_track");
         specTable.put(SpectrumAdapter.channelType, "wavelength");
         specTable.put(SpectrumAdapter.channelValues, new float[] {1.610f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"3a"});
         spectrumAdapter2 = new SpectrumAdapter(reader, specTable);

         multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "Reflectance", "Reflectance", null, null);


         aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2});
         aggr.setInitialWavenumber(0.630f);
         aggr.setDataRange(new float[] {0f, 100f});
         multiSpectData_s.add(aggr);

         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
       }
       else if (name.startsWith("MHSx_xxx_1B") && name.endsWith(".h5")) {
         HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel1");
         swthTable.put("lon_array_name", "U-MARF/EPS/MHSx_xxx_1B/DATA/IMAGE_LON_ARRAY");
         swthTable.put("lat_array_name", "U-MARF/EPS/MHSx_xxx_1B/DATA/IMAGE_LAT_ARRAY");
         swthTable.put("XTrack", "dim1");
         swthTable.put("Track", "dim0");
         swthTable.put("geo_Track", "dim0");
         swthTable.put("geo_XTrack", "dim1");
         swthTable.put("product_name", "MHS_xxx_1B");
         swthTable.put("range_name", "Emissive_Bands");

         SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setRangeProcessor(new MHS_xxx_L1_RangeProcessor(reader, "CH1"));
         swathAdapter0.setDefaultStride(1);
         HashMap subset = swathAdapter0.getDefaultSubset();
         defaultSubset = subset;

         HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel1");
         specTable.put(SpectrumAdapter.x_dim_name, "dim1");
         specTable.put(SpectrumAdapter.y_dim_name, "dim0");
         specTable.put(SpectrumAdapter.channelValues, new float[] {89.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch1"});
         specTable.put(SpectrumAdapter.channelType, "frequency");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "BrightnessTemp", "BrightnessTemp", null, null);
         
         // Channel 2
         swthTable.put("array_name", "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel2");
         SwathAdapter swathAdapter1 = new SwathAdapter(reader, swthTable);
         swathAdapter1.setRangeProcessor(new MHS_xxx_L1_RangeProcessor(reader, "CH2"));
         swathAdapter1.setDefaultStride(1);

         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel2");
         specTable.put(SpectrumAdapter.channelValues, new float[] {157.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch2"});
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "BrightnessTemp", "BrightnessTemp", null, null);

         // Channel 3
         swthTable.put("array_name", "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel3");
         SwathAdapter swathAdapter2 = new SwathAdapter(reader, swthTable);
         swathAdapter2.setRangeProcessor(new MHS_xxx_L1_RangeProcessor(reader, "CH3"));
         swathAdapter2.setDefaultStride(1);

         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel3");
         specTable.put(SpectrumAdapter.channelValues, new float[] {182.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch3"});
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, specTable);
         
         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "BrightnessTemp", "BrightnessTemp", null, null);

         // Channel 4
         swthTable.put("array_name", "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel4");
         SwathAdapter swathAdapter3 = new SwathAdapter(reader, swthTable);
         swathAdapter3.setRangeProcessor(new MHS_xxx_L1_RangeProcessor(reader, "CH4"));
         swathAdapter3.setDefaultStride(1);

         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel4");
         specTable.put(SpectrumAdapter.channelValues, new float[] {184.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch4"});
         SpectrumAdapter spectrumAdapter3 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData3 = new MultiSpectralData(swathAdapter3, spectrumAdapter3, "BrightnessTemp", "BrightnessTemp", null, null);

         // Channel 5
         swthTable.put("array_name", "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel5");
         SwathAdapter swathAdapter4 = new SwathAdapter(reader, swthTable);
         swathAdapter4.setRangeProcessor(new MHS_xxx_L1_RangeProcessor(reader, "CH5"));
         swathAdapter4.setDefaultStride(1);

         specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/MHSx_xxx_1B/DATA/Channel5");
         specTable.put(SpectrumAdapter.channelValues, new float[] {190.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch5"});
         SpectrumAdapter spectrumAdapter4 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData4 = new MultiSpectralData(swathAdapter4, spectrumAdapter4, "BrightnessTemp", "BrightnessTemp", null, null);

         MultiSpectralAggr aggr = new MultiSpectralAggr(
            new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2, multiSpectData3, multiSpectData4});
         aggr.setInitialWavenumber(89.0f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
       }
       else if (name.contains("MHS_C") && name.endsWith(".nc")) {
         HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "channel_1");
         swthTable.put("lon_array_name", "lon");
         swthTable.put("lat_array_name", "lat");
         swthTable.put("XTrack", "across_track");
         swthTable.put("Track", "along_track");
         swthTable.put("geo_Track", "along_track");
         swthTable.put("geo_XTrack", "across_track");
         swthTable.put("scale_name", "scale_factor");
         swthTable.put("fill_value_name", "_FillValue");
         swthTable.put("product_name", "MHS_1B_NCDF");
         swthTable.put("range_name", "Emissive_Bands");

         SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setRangeProcessor(new MHS_L1_NCDF_RangeProcessor(reader, swthTable, "CH1"));
         swathAdapter0.setDefaultStride(1);
         HashMap subset = swathAdapter0.getDefaultSubset();
         defaultSubset = subset;

         HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "channel_1");
         specTable.put(SpectrumAdapter.x_dim_name, "across_track");
         specTable.put(SpectrumAdapter.y_dim_name, "along_track");
         specTable.put(SpectrumAdapter.channelValues, new float[] {89.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch1"});
         specTable.put(SpectrumAdapter.channelType, "frequency");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "BrightnessTemp", "BrightnessTemp", null, null);
         
         // Channel 2
         swthTable.put("array_name", "channel_2");
         SwathAdapter swathAdapter1 = new SwathAdapter(reader, swthTable);
         swathAdapter1.setRangeProcessor(new MHS_L1_NCDF_RangeProcessor(reader, swthTable, "CH2"));
         swathAdapter1.setDefaultStride(1);

         specTable.put(SpectrumAdapter.array_name, "channel_2");
         specTable.put(SpectrumAdapter.channelValues, new float[] {157.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch2"});
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "BrightnessTemp", "BrightnessTemp", null, null);

         // Channel 3
         swthTable.put("array_name", "channel_3");
         SwathAdapter swathAdapter2 = new SwathAdapter(reader, swthTable);
         swathAdapter2.setRangeProcessor(new MHS_L1_NCDF_RangeProcessor(reader, swthTable, "CH3"));
         swathAdapter2.setDefaultStride(1);

         specTable.put(SpectrumAdapter.array_name, "channel_3");
         specTable.put(SpectrumAdapter.channelValues, new float[] {182.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch3"});
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, specTable);
         
         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "BrightnessTemp", "BrightnessTemp", null, null);

         // Channel 4
         swthTable.put("array_name", "channel_4");
         SwathAdapter swathAdapter3 = new SwathAdapter(reader, swthTable);
         swathAdapter3.setRangeProcessor(new MHS_L1_NCDF_RangeProcessor(reader, swthTable, "CH4"));
         swathAdapter3.setDefaultStride(1);

         specTable.put(SpectrumAdapter.array_name, "channel_4");
         specTable.put(SpectrumAdapter.channelValues, new float[] {184.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch4"});
         SpectrumAdapter spectrumAdapter3 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData3 = new MultiSpectralData(swathAdapter3, spectrumAdapter3, "BrightnessTemp", "BrightnessTemp", null, null);

         // Channel 5
         swthTable.put("array_name", "channel_5");
         SwathAdapter swathAdapter4 = new SwathAdapter(reader, swthTable);
         swathAdapter4.setRangeProcessor(new MHS_L1_NCDF_RangeProcessor(reader, swthTable, "CH5"));
         swathAdapter4.setDefaultStride(1);

         specTable.put(SpectrumAdapter.array_name, "channel_5");
         specTable.put(SpectrumAdapter.channelValues, new float[] {190.0f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"ch5"});
         SpectrumAdapter spectrumAdapter4 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData4 = new MultiSpectralData(swathAdapter4, spectrumAdapter4, "BrightnessTemp", "BrightnessTemp", null, null);

         MultiSpectralAggr aggr = new MultiSpectralAggr(
            new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2, multiSpectData3, multiSpectData4});
         aggr.setInitialWavenumber(89.0f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
       }
       else if (name.startsWith("HIRS_xxx_1B") && name.endsWith(".h5")) {

         int numChannels = 19; // Emissive channels
         MultiSpectralData[] msdArray = new MultiSpectralData[numChannels];
         String[] arrayNames = new String[] {"Channel1", "Channel2", "Channel3", "Channel4", "Channel5", "Channel6", "Channel7",
                                           "Channel10", "Channel8", "Channel9", "Channel11", "Channel12", "Channel13", "Channel14",
                                           "Channel15", "Channel16", "Channel17", "Channel18", "Channel19"};
         String[] bandNames = new String[] {"CH1", "CH2", "CH3", "CH4", "CH5", "CH6", "CH7",
                                          "CH10", "CH8", "CH9", "CH11", "CH12", "CH13", "CH14",
                                          "CH15", "CH16", "CH17", "CH18", "CH19"};

         for (int k=0; k<numChannels; k++) {
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", "U-MARF/EPS/HIRS_xxx_1B/DATA/"+arrayNames[k]);
           swthTable.put("lon_array_name", "U-MARF/EPS/HIRS_xxx_1B/DATA/IMAGE_LON_ARRAY");
           swthTable.put("lat_array_name", "U-MARF/EPS/HIRS_xxx_1B/DATA/IMAGE_LAT_ARRAY");
           swthTable.put("XTrack", "dim1");
           swthTable.put("Track", "dim0");
           swthTable.put("geo_Track", "dim0");
           swthTable.put("geo_XTrack", "dim1");
           swthTable.put("product_name", "HIRS_xxx_1B");
           swthTable.put("range_name", "Emissive_Bands");

           SwathAdapter swathAdapter = new SwathAdapter(reader, swthTable);
           HIRS_xxx_L1_RangeProcessor rngProcessor = new HIRS_xxx_L1_RangeProcessor(reader, bandNames[k]);
           swathAdapter.setRangeProcessor(rngProcessor);
           swathAdapter.setDefaultStride(1);
           HashMap subset = swathAdapter.getDefaultSubset();
           defaultSubset = subset;

           HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
           float waveNum = rngProcessor.gamma;
           specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/HIRS_xxx_1B/DATA/"+arrayNames[k]);
           specTable.put(SpectrumAdapter.x_dim_name, "dim1");
           specTable.put(SpectrumAdapter.y_dim_name, "dim0");
           specTable.put(SpectrumAdapter.channelValues, new float[] {waveNum});
           specTable.put(SpectrumAdapter.bandNames, new String[] {bandNames[k]});
           specTable.put(SpectrumAdapter.channelType, "wavenumber");
           SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, specTable);

           MultiSpectralData msd = new MultiSpectralData(swathAdapter, spectrumAdapter, "BrightnessTemp", "BrightnessTemp", null, null);
           msdArray[k] = msd;
         }

         MultiSpectralAggr aggr = new MultiSpectralAggr(msdArray);
         aggr.setInitialWavenumber(898.8f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
       }
       else if (name.contains("HIRS_C") && name.endsWith(".nc")) {
         int numChannels = 19; // Emissive channels
         MultiSpectralData[] msdArray = new MultiSpectralData[numChannels];
         String[] arrayNames = new String[] {"channel_01", "channel_02", "channel_03", "channel_04", "channel_05", "channel_06", "channel_07",
                                           "channel_10", "channel_08", "channel_09", "channel_11", "channel_12", "channel_13", "channel_14",
                                           "channel_15", "channel_16", "channel_17", "channel_18", "channel_19"};
         String[] bandNames = new String[] {"1", "2", "3", "4", "5", "6", "7",
                                            "10", "8", "9", "11", "12", "13", "14",
                                            "15", "16", "17", "18", "19"};

         for (int k=0; k<numChannels; k++) {
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", arrayNames[k]);
           swthTable.put("lon_array_name", "lon");
           swthTable.put("lat_array_name", "lat");
           swthTable.put("XTrack", "across_track");
           swthTable.put("Track", "along_track");
           swthTable.put("geo_Track", "along_track");
           swthTable.put("geo_XTrack", "across_track");
           swthTable.put("scale_name", "scale_factor");
           swthTable.put("fill_value_name", "_FillValue");
           swthTable.put("product_name", "HIRS_1B_NCDF");
           swthTable.put("range_name", "Emissive_Bands");

           SwathAdapter swathAdapter = new SwathAdapter(reader, swthTable);
           HIRS_L1_NCDF_RangeProcessor rngProcessor = new HIRS_L1_NCDF_RangeProcessor(reader, swthTable, bandNames[k]);
           swathAdapter.setRangeProcessor(rngProcessor);
           swathAdapter.setDefaultStride(1);
           HashMap subset = swathAdapter.getDefaultSubset();
           defaultSubset = subset;

           HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
           float waveNum = rngProcessor.wnc;
           specTable.put(SpectrumAdapter.array_name, arrayNames[k]);
           specTable.put(SpectrumAdapter.x_dim_name, "across_track");
           specTable.put(SpectrumAdapter.y_dim_name, "along_track");
           specTable.put(SpectrumAdapter.channelValues, new float[] {waveNum});
           specTable.put(SpectrumAdapter.bandNames, new String[] {bandNames[k]});
           specTable.put(SpectrumAdapter.channelType, "wavenumber");
           SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, specTable);

           MultiSpectralData msd = new MultiSpectralData(swathAdapter, spectrumAdapter, "BrightnessTemp", "BrightnessTemp", null, null);
           msdArray[k] = msd;
         }

         MultiSpectralAggr aggr = new MultiSpectralAggr(msdArray);
         aggr.setInitialWavenumber(898.59f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
       }
       else if (name.startsWith("hirsl1c") && name.endsWith(".h5")) {
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", "Data/btemps");
           swthTable.put("lon_array_name", "Geolocation/Longitude");
           swthTable.put("lat_array_name", "Geolocation/Latitude");
           swthTable.put(SwathAdapter.geo_scale_name, "Scale");
           swthTable.put(SpectrumAdapter.channelIndex_name, "dim2");
           swthTable.put("XTrack", "dim1");
           swthTable.put("Track", "dim0");
           swthTable.put("geo_Track", "dim0");
           swthTable.put("geo_XTrack", "dim1");
           swthTable.put("product_name", "HIRS_L1C_AAPP");
           swthTable.put("range_name", "Emissive_Bands");
           SwathAdapter swathAdapter = new SwathAdapter(reader, swthTable);   
           swathAdapter.setRangeProcessor(new HIRS_L1C_RangeProcessor(reader, swthTable));
           swathAdapter.setDefaultStride(1);
           
           float[] waveNumbers = new float[] {668.66f, 679.18f, 689.7f, 701.99f, 716.47f, 731.71f, 748.82f, 898.59f, 
                                               1028.5f, 800.93f, 1361.9f, 1530.1f, 2189.7f, 2212.3f, 2237.6f, 2245.6f, 2418.9f, 2516.1f, 2663.7f};
           
           String[] bandNames = new String[] {"1", "2", "3", "4", "5", "6", "7",
                                              "8", "9", "10", "11", "12", "13", "14",
                                              "15", "16", "17", "18", "19"};
           
           HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
           specTable.put(SpectrumAdapter.array_name, "Data/btemps");
           specTable.put(SpectrumAdapter.channelIndex_name, "dim2");
           specTable.put(SpectrumAdapter.x_dim_name, "dim1");
           specTable.put(SpectrumAdapter.y_dim_name, "dim0");
           specTable.put(SpectrumAdapter.channelValues, waveNumbers);
           specTable.put(SpectrumAdapter.bandNames, bandNames);
           specTable.put(SpectrumAdapter.channelType, "wavenumber");
           specTable.put(SpectrumAdapter.channelIndices_name, new int[] {0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18});
           SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, specTable);
           
           MultiSpectralData msd = new MultiSpectralData(swathAdapter, spectrumAdapter, "BrightnessTemp", "BrightnessTemp", null, null);
           msd.setInitialWavenumber(898.59f);
           multiSpectData_s.add(msd);
       }
       else if (name.startsWith("AMSA_xxx_1B") && name.endsWith(".h5")) {
         int numChannels = 15; // Emissive channels
         MultiSpectralData[] msdArray = new MultiSpectralData[numChannels];
         String[] arrayNames = new String[] {"Channel1", "Channel2", "Channel3", "Channel4", "Channel5", "Channel6", "Channel7",
                                             "Channel8", "Channel9", "Channel10", "Channel11", "Channel12", "Channel13", "Channel14",
                                             "Channel15"};
         String[] bandNames = new String[] {"CH1", "CH2", "CH3", "CH4", "CH5", "CH6", "CH7",
                                            "CH8", "CH9", "CH10", "CH11", "CH12", "CH13", "CH14", "CH15"};

         float[] frequency = new float[] {23.8f, 31.4f, 50.3f, 52.8f, 53.596f, 54.4f, 54.94f, 55.5f,
                                          57.290f, 57.291f, 57.292f, 57.293f, 57.294f, 57.295f, 89.0f };

         for (int k=0; k<numChannels; k++) {
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", "U-MARF/EPS/AMSA_xxx_1B/DATA/"+arrayNames[k]);
           swthTable.put("lon_array_name", "U-MARF/EPS/AMSA_xxx_1B/DATA/IMAGE_LON_ARRAY");
           swthTable.put("lat_array_name", "U-MARF/EPS/AMSA_xxx_1B/DATA/IMAGE_LAT_ARRAY");
           swthTable.put("XTrack", "dim1");
           swthTable.put("Track", "dim0");
           swthTable.put("geo_Track", "dim0");
           swthTable.put("geo_XTrack", "dim1");
           swthTable.put("product_name", "AMSA_xxx_1B");
           swthTable.put("range_name", "Emissive_Bands");

           SwathAdapter swathAdapter = new SwathAdapter(reader, swthTable);
           AMSA_xxx_L1_RangeProcessor rngProcessor = new AMSA_xxx_L1_RangeProcessor(reader, bandNames[k]);
           swathAdapter.setRangeProcessor(rngProcessor);
           swathAdapter.setDefaultStride(1);
           HashMap subset = swathAdapter.getDefaultSubset();
           defaultSubset = subset;

           HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
           specTable.put(SpectrumAdapter.array_name, "U-MARF/EPS/AMSA_xxx_1B/DATA/"+arrayNames[k]);
           specTable.put(SpectrumAdapter.x_dim_name, "dim1");
           specTable.put(SpectrumAdapter.y_dim_name, "dim0");
           specTable.put(SpectrumAdapter.channelValues, new float[] {frequency[k]});
           specTable.put(SpectrumAdapter.bandNames, new String[] {bandNames[k]});
           specTable.put(SpectrumAdapter.channelType, "frequency");
           SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, specTable);

           MultiSpectralData msd = new MultiSpectralData(swathAdapter, spectrumAdapter, "BrightnessTemp", "BrightnessTemp", null, null);
           msdArray[k] = msd;
         }

         MultiSpectralAggr aggr = new MultiSpectralAggr(msdArray);
         aggr.setInitialWavenumber(23.8f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
       }
       else if (name.contains("AMSUA_C") && name.endsWith(".nc")) {
         int numChannels = 15; // Emissive channels
         MultiSpectralData[] msdArray = new MultiSpectralData[numChannels];
         String[] arrayNames = new String[] {"channel_1", "channel_2", "channel_3", "channel_4", "channel_5", "channel_6", "channel_7",
                                             "channel_8", "channel_9", "channel_10", "channel_11", "channel_12", "channel_13", "channel_14",
                                             "channel_15"};
         String[] bandNames = new String[] {"CH1", "CH2", "CH3", "CH4", "CH5", "CH6", "CH7",
                                            "CH8", "CH9", "CH10", "CH11", "CH12", "CH13", "CH14", "CH15"};

         float[] frequency = new float[] {23.8f, 31.4f, 50.3f, 52.8f, 53.596f, 54.4f, 54.94f, 55.5f,
                                          57.290f, 57.291f, 57.292f, 57.293f, 57.294f, 57.295f, 89.0f };

         for (int k=0; k<numChannels; k++) {
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", arrayNames[k]);
           swthTable.put("lon_array_name", "lon");
           swthTable.put("lat_array_name", "lat");
           swthTable.put("XTrack", "across_track");
           swthTable.put("Track", "along_track");
           swthTable.put("geo_Track", "along_track");
           swthTable.put("geo_XTrack", "across_track");
           swthTable.put("scale_name", "scale_factor");
           swthTable.put("fill_value_name", "_FillValue");
           swthTable.put("product_name", "AMSA_1B_NCDF");
           swthTable.put("range_name", "Emissive_Bands");

           SwathAdapter swathAdapter = new SwathAdapter(reader, swthTable);
           AMSA_L1_NCDF_RangeProcessor rngProcessor = new AMSA_L1_NCDF_RangeProcessor(reader, swthTable, bandNames[k]);
           swathAdapter.setRangeProcessor(rngProcessor);
           swathAdapter.setDefaultStride(1);
           HashMap subset = swathAdapter.getDefaultSubset();
           defaultSubset = subset;

           HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
           specTable.put(SpectrumAdapter.array_name, arrayNames[k]);
           specTable.put(SpectrumAdapter.x_dim_name, "across_track");
           specTable.put(SpectrumAdapter.y_dim_name, "along_track");
           specTable.put(SpectrumAdapter.channelValues, new float[] {frequency[k]});
           specTable.put(SpectrumAdapter.bandNames, new String[] {bandNames[k]});
           specTable.put(SpectrumAdapter.channelType, "frequency");
           SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, specTable);

           MultiSpectralData msd = new MultiSpectralData(swathAdapter, spectrumAdapter, "BrightnessTemp", "BrightnessTemp", null, null);
           msdArray[k] = msd;
         }

         MultiSpectralAggr aggr = new MultiSpectralAggr(msdArray);
         aggr.setInitialWavenumber(23.8f);
         aggr.setDataRange(new float[] {180f, 340f});
         multiSpectData = aggr;
         multiSpectData_s.add(aggr);
       }
       else if (name.startsWith("amsual1c") && name.endsWith(".h5")) {
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", "Data/btemps");
           swthTable.put("lon_array_name", "Geolocation/Longitude");
           swthTable.put("lat_array_name", "Geolocation/Latitude");
           swthTable.put(SwathAdapter.geo_scale_name, "Scale");
           swthTable.put(SpectrumAdapter.channelIndex_name, "dim2");
           swthTable.put("XTrack", "dim1");
           swthTable.put("Track", "dim0");
           swthTable.put("geo_Track", "dim0");
           swthTable.put("geo_XTrack", "dim1");
           swthTable.put("product_name", "AMSUA_L1C_AAPP");
           swthTable.put("range_name", "Emissive_Bands");
           SwathAdapter swathAdapter = new SwathAdapter(reader, swthTable);   
           swathAdapter.setRangeProcessor(new AMSUA_L1C_AAPP_RangeProcessor(reader, swthTable));
           swathAdapter.setDefaultStride(1);

           float[] waveNumbers = new float[] {0.793883f, 1.047391f, 1.677827f, 1.761218f, 1.78777f, 1.814589f, 1.832601f, 1.851281f, 
                                               1.9111f, 1.9112f, 1.9113f, 1.9114f, 1.9115f, 1.9116f, 2.96872f};
           
           String[] bandNames = new String[] {"1", "2", "3", "4", "5", "6", "7",
                                              "8", "9", "10", "11", "12", "13", "14", "15"};
           
           HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
           specTable.put(SpectrumAdapter.array_name, "Data/btemps");
           specTable.put(SpectrumAdapter.channelIndex_name, "dim2");
           specTable.put(SpectrumAdapter.x_dim_name, "dim1");
           specTable.put(SpectrumAdapter.y_dim_name, "dim0");
           specTable.put(SpectrumAdapter.channelValues, waveNumbers);
           specTable.put(SpectrumAdapter.bandNames, bandNames);
           specTable.put(SpectrumAdapter.channelType, "wavenumber");
           SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, specTable);
           
           MultiSpectralData msd = new MultiSpectralData(swathAdapter, spectrumAdapter, "BrightnessTemp", "BrightnessTemp", null, null);
           msd.setInitialWavenumber(1.047391f);
           multiSpectData_s.add(msd);          
       }
       else if (name.startsWith("mhsl1c") && name.endsWith(".h5")) {
           HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
           swthTable.put("array_name", "Data/btemps");
           swthTable.put("lon_array_name", "Geolocation/Longitude");
           swthTable.put("lat_array_name", "Geolocation/Latitude");
           swthTable.put(SwathAdapter.geo_scale_name, "Scale");
           swthTable.put(SpectrumAdapter.channelIndex_name, "dim2");
           swthTable.put("XTrack", "dim1");
           swthTable.put("Track", "dim0");
           swthTable.put("geo_Track", "dim0");
           swthTable.put("geo_XTrack", "dim1");
           swthTable.put("product_name", "AMSUA_L1C_AAPP");
           swthTable.put("range_name", "Emissive_Bands");
           SwathAdapter swathAdapter = new SwathAdapter(reader, swthTable);   
           swathAdapter.setRangeProcessor(new MHS_L1C_AAPP_RangeProcessor(reader, swthTable));
           swathAdapter.setDefaultStride(1);

           float[] waveNumbers = new float[] {2.96872f, 5.236956f, 6.114597f, 6.114598f, 6.348092f};
           
           String[] bandNames = new String[] {"1", "2", "3", "4", "5"};
           
           HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
           specTable.put(SpectrumAdapter.array_name, "Data/btemps");
           specTable.put(SpectrumAdapter.channelIndex_name, "dim2");
           specTable.put(SpectrumAdapter.x_dim_name, "dim1");
           specTable.put(SpectrumAdapter.y_dim_name, "dim0");
           specTable.put(SpectrumAdapter.channelValues, waveNumbers);
           specTable.put(SpectrumAdapter.bandNames, bandNames);
           specTable.put(SpectrumAdapter.channelType, "wavenumber");
           SpectrumAdapter spectrumAdapter = new SpectrumAdapter(reader, specTable);
           
           MultiSpectralData msd = new MultiSpectralData(swathAdapter, spectrumAdapter, "BrightnessTemp", "BrightnessTemp", null, null);
           msd.setInitialWavenumber(5.236956f);
           multiSpectData_s.add(msd);          
       }
       else if (name.contains("MERSI_1000M_L1B")) {
         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "EV_1KM_RefSB");
         table.put("lon_array_name", "Longitude");
         table.put("lat_array_name", "Latitude");
         table.put("XTrack", "dim2");
         table.put("Track", "dim1");
         table.put("geo_Track", "dim0");
         table.put("geo_XTrack", "dim1");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands6_20");
         table.put("product_name", "MERSI_L1B");
         table.put(SpectrumAdapter.channelIndex_name, "dim0");
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));

         SwathAdapter swathAdapter0 = new SwathAdapter(reader, table);
         swathAdapter0.setRangeProcessor(new MERSI_L1B_Refl_RangeProcessor(reader, 4));
         swathAdapter0.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "EV_1KM_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "dim0");
         table.put(SpectrumAdapter.x_dim_name, "dim2");
         table.put(SpectrumAdapter.y_dim_name, "dim1");
         table.put(SpectrumAdapter.channelValues, new float[]
           {0.412f,0.443f,0.490f,0.520f,0.565f,0.650f,0.685f,0.765f,0.865f,
            0.905f,0.940f,0.980f,1.030f,1.640f,2.130f});
         table.put(SpectrumAdapter.bandNames, new String[]
           {"6","7","8","9","10","11","12","13","14",
            "15","16","17","18","19","20"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", null, null);
         multiSpectData0.setInitialWavenumber(0.865f);
         defaultSubset = multiSpectData0.getDefaultSubset();

         // QKM -> KM aggregated bands
         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "EV_250_Aggr\\.1KM_RefSB");
         table.put("lon_array_name", "Longitude");
         table.put("lat_array_name", "Latitude");
         table.put("XTrack", "dim2");
         table.put("Track", "dim1");
         table.put("geo_Track", "dim0");
         table.put("geo_XTrack", "dim1");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands1_4");
         table.put("product_name", "MERSI_L1B");
         table.put(SpectrumAdapter.channelIndex_name, "dim0");
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));

         SwathAdapter swathAdapter1 = new SwathAdapter(reader, table);
         swathAdapter1.setRangeProcessor(new MERSI_L1B_Refl_RangeProcessor(reader, 0));
         swathAdapter1.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "EV_250_Aggr\\.1KM_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "dim0");
         table.put(SpectrumAdapter.x_dim_name, "dim2");
         table.put(SpectrumAdapter.y_dim_name, "dim1");
         table.put(SpectrumAdapter.channelValues, new float[]
           {0.470f,0.550f,0.6501f,0.8651f});
         table.put(SpectrumAdapter.bandNames, new String[]
           {"1","2","3","4"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", null, null);
         multiSpectData1.setInitialWavenumber(0.8651f);

         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData1, multiSpectData0});
         aggr.setInitialWavenumber(0.865f);
         multiSpectData_s.add(aggr);

         // the emmissive band
         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "EV_250_Aggr\\.1KM_Emissive");
         table.put("lon_array_name", "Longitude");
         table.put("lat_array_name", "Latitude");
         table.put("XTrack", "dim1");
         table.put("Track", "dim0");
         table.put("geo_Track", "dim0");
         table.put("geo_XTrack", "dim1");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Emissive_Bands");

         SwathAdapter swathAdapter2 = new SwathAdapter(reader, table);
         swathAdapter2.setRangeProcessor(new MERSI_L1B_Emis_RangeProcessor());
         swathAdapter2.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "EV_250_Aggr\\.1KM_Emissive");
         table.put(SpectrumAdapter.x_dim_name, "dim1");
         table.put(SpectrumAdapter.y_dim_name, "dim0");
         table.put(SpectrumAdapter.channelValues, new float[] {11.50f});
         table.put(SpectrumAdapter.bandNames, new String[] {"5"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "BrightnessTemp", "BrightnessTemp", null, null);
         MultiSpectralAggr multiSpectAggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData2});
         multiSpectAggr.setInitialWavenumber(11.5f);
       }
       else if (name.contains("MERSI_0250M_L1B")) {
         HashMap swthTable = SwathAdapter.getEmptyMetadataTable();
         swthTable.put("array_name", "EV_250_RefSB_b4");
         swthTable.put("lon_array_name", "Longitude");
         swthTable.put("lat_array_name", "Latitude");
         swthTable.put("XTrack", "dim1");
         swthTable.put("Track", "dim0");
         swthTable.put("geo_Track", "dim0");
         swthTable.put("geo_XTrack", "dim1");
         swthTable.put("fill_value_name", "_FillValue");
         swthTable.put("range_name", "Reflective_Bands");
         swthTable.put("product_name", "MERSI_L1B");
         swthTable.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
         swthTable.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(0.0));
         swthTable.put(SwathAdapter.geo_track_skip_name, Double.toString(4.0));
         swthTable.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(4.0));

         SwathAdapter swathAdapter3 = new SwathAdapter(reader, swthTable);
         swathAdapter3.setRangeProcessor(new MERSI_L1B_Refl_RangeProcessor(reader, 3));
         swathAdapter3.setDefaultStride(20);

         HashMap specTable = SpectrumAdapter.getEmptyMetadataTable();
         specTable.put(SpectrumAdapter.array_name, "EV_250_RefSB_b4");
         specTable.put(SpectrumAdapter.x_dim_name, "dim1");
         specTable.put(SpectrumAdapter.y_dim_name, "dim0");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.8651f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"4"});
         specTable.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter3 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData3 = new MultiSpectralData(swathAdapter3, spectrumAdapter3, "Reflectance", "Reflectance", null, null);


         swthTable.put("array_name", "EV_250_RefSB_b1");
         SwathAdapter swathAdapter0 = new SwathAdapter(reader, swthTable);
         swathAdapter0.setRangeProcessor(new MERSI_L1B_Refl_RangeProcessor(reader, 0));
         swathAdapter0.setDefaultStride(20);

         specTable.put(SpectrumAdapter.array_name, "EV_250_RefSB_b1");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.470f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"1"});
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", null, null);

         swthTable.put("array_name", "EV_250_RefSB_b2");

         SwathAdapter swathAdapter1 = new SwathAdapter(reader, swthTable);
         swathAdapter1.setRangeProcessor(new MERSI_L1B_Refl_RangeProcessor(reader, 1));
         swathAdapter1.setDefaultStride(20);

         specTable.put(SpectrumAdapter.array_name, "EV_250_RefSB_b2");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.550f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"2"});
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", null, null);

         swthTable.put("array_name", "EV_250_RefSB_b3");

         SwathAdapter swathAdapter2 = new SwathAdapter(reader, swthTable);
         swathAdapter2.setRangeProcessor(new MERSI_L1B_Refl_RangeProcessor(reader, 2));
         swathAdapter2.setDefaultStride(20);

         specTable.put(SpectrumAdapter.array_name, "EV_250_RefSB_b3");
         specTable.put(SpectrumAdapter.channelValues, new float[] {0.651f});
         specTable.put(SpectrumAdapter.bandNames, new String[] {"3"});
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, specTable);

         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "Reflectance", "Reflectance", null, null);


         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData0, multiSpectData1, multiSpectData2, multiSpectData3});
         aggr.setInitialWavenumber(0.651f);
         aggr.setDataRange(new float[] {0f, 1f});
         multiSpectData_s.add(aggr);

         categories = DataCategory.parseCategories("MultiSpectral;MultiSpectral;IMAGE");
       }
       else if (name.startsWith("FY3C_MERSI") && name.contains("1000M")) {
         String other = filename.replaceAll("1000M", "GEO1K");
         MultiDimensionReader geoReader = new NetCDFFile(other);
         HashMap table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "Data/EV_1KM_RefSB");
         table.put("lon_array_name", "Geolocation/Longitude");
         table.put("lat_array_name", "Geolocation/Latitude");
         table.put("XTrack", "dim2");
         table.put("Track", "dim1");
         table.put("geo_Track", "dim0");
         table.put("geo_XTrack", "dim1");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands6_20");
         table.put("product_name", "MERSI_L1B");
         table.put(SpectrumAdapter.channelIndex_name, "dim0");
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));

         SwathAdapter swathAdapter0 = new SwathAdapter(reader, table, geoReader);
         swathAdapter0.setRangeProcessor(new MERSI_L1B_Refl_RangeProcessor(reader, 4, "FY3C_MERSI"));
         swathAdapter0.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "Data/EV_1KM_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "dim0");
         table.put(SpectrumAdapter.x_dim_name, "dim2");
         table.put(SpectrumAdapter.y_dim_name, "dim1");
         table.put(SpectrumAdapter.channelValues, new float[]
           {0.412f,0.443f,0.490f,0.520f,0.565f,0.650f,0.685f,0.765f,0.865f,
            0.905f,0.940f,0.980f,1.030f,1.640f,2.130f});
         table.put(SpectrumAdapter.bandNames, new String[]
           {"6","7","8","9","10","11","12","13","14",
            "15","16","17","18","19","20"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "Reflectance", "Reflectance", null, null);
         multiSpectData0.setInitialWavenumber(0.865f);
         defaultSubset = multiSpectData0.getDefaultSubset();

         // QKM -> KM aggregated bands
         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "Data/EV_250_Aggr\\.1KM_RefSB");
         table.put("lon_array_name", "Geolocation/Longitude");
         table.put("lat_array_name", "Geolocation/Latitude");
         table.put("XTrack", "dim2");
         table.put("Track", "dim1");
         table.put("geo_Track", "dim0");
         table.put("geo_XTrack", "dim1");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Reflective_Bands1_4");
         table.put("product_name", "MERSI_L1B");
         table.put(SpectrumAdapter.channelIndex_name, "dim0");
         table.put(SwathAdapter.multiScaleDimensionIndex, Integer.toString(0));

         SwathAdapter swathAdapter1 = new SwathAdapter(reader, table, geoReader);
         swathAdapter1.setRangeProcessor(new MERSI_L1B_Refl_RangeProcessor(reader, 0, "FY3C_MERSI"));
         swathAdapter1.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "Data/EV_250_Aggr\\.1KM_RefSB");
         table.put(SpectrumAdapter.channelIndex_name, "dim0");
         table.put(SpectrumAdapter.x_dim_name, "dim2");
         table.put(SpectrumAdapter.y_dim_name, "dim1");
         table.put(SpectrumAdapter.channelValues, new float[]
           {0.470f,0.550f,0.6501f,0.8651f});
         table.put(SpectrumAdapter.bandNames, new String[]
           {"1","2","3","4"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", null, null);
         multiSpectData1.setInitialWavenumber(0.8651f);

         MultiSpectralAggr aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData1, multiSpectData0});
         aggr.setInitialWavenumber(0.865f);
         multiSpectData_s.add(aggr);

         // the emmissive band
         table = SwathAdapter.getEmptyMetadataTable();
         table.put("array_name", "Data/EV_250_Aggr\\.1KM_Emissive");
         table.put("lon_array_name", "Geolocation/Longitude");
         table.put("lat_array_name", "Geolocation/Latitude");
         table.put("XTrack", "dim1");
         table.put("Track", "dim0");
         table.put("geo_Track", "dim0");
         table.put("geo_XTrack", "dim1");
         table.put("fill_value_name", "_FillValue");
         table.put("range_name", "Emissive_Bands");

         SwathAdapter swathAdapter2 = new SwathAdapter(reader, table, geoReader);
         swathAdapter2.setRangeProcessor(new MERSI_L1B_Emis_RangeProcessor());
         swathAdapter2.setDefaultStride(10);

         table = SpectrumAdapter.getEmptyMetadataTable();
         table.put(SpectrumAdapter.array_name, "Data/EV_250_Aggr\\.1KM_Emissive");
         table.put(SpectrumAdapter.x_dim_name, "dim1");
         table.put(SpectrumAdapter.y_dim_name, "dim0");
         table.put(SpectrumAdapter.channelValues, new float[] {11.50f});
         table.put(SpectrumAdapter.bandNames, new String[] {"5"});
         table.put(SpectrumAdapter.channelType, "wavelength");
         SpectrumAdapter spectrumAdapter2 = new SpectrumAdapter(reader, table);

         MultiSpectralData multiSpectData2 = new MultiSpectralData(swathAdapter2, spectrumAdapter2, "BrightnessTemp", "BrightnessTemp", null, null);

         aggr = new MultiSpectralAggr(new MultiSpectralData[] {multiSpectData2});
         aggr.setInitialWavenumber(11.50f);
         multiSpectData_s.add(aggr);
       }
       else if (name.startsWith("hrpt") && name.endsWith(".h5")) {
           // Emissive bands
           HashMap table = SwathAdapter.getEmptyMetadataTable();
           table.put("array_name", "Data/hrpt");
           table.put("lon_array_name", "Geolocation/Longitude");
           table.put("lat_array_name", "Geolocation/Latitude");
           table.put("Track", "dim0");
           table.put("XTrack", "dim1");
           table.put("geo_Track", "dim0");
           table.put("geo_XTrack", "dim1");
           table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
           table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(24.0));
           table.put(SwathAdapter.geo_track_skip_name, Double.toString(1.0));
           table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(40.0));
           table.put(SwathAdapter.geo_scale_name, "Scale");
           table.put(SwathAdapter.geo_fillValue_name, "FillValue");
           table.put("range_name", "Emissive_Bands");
           table.put("product_name", "AVHRR_L1B_AAPP");
           table.put(SpectrumAdapter.channelIndex_name, "dim2");
           
           SwathAdapter swathAdapter0 = new SwathAdapter(reader, table);
           
           table = SpectrumAdapter.getEmptyMetadataTable();
           table.put(SpectrumAdapter.array_name, "Data/hrpt");
           table.put(SpectrumAdapter.channelIndex_name, "dim2");
           table.put(SpectrumAdapter.x_dim_name, "dim1");
           table.put(SpectrumAdapter.y_dim_name, "dim0");
           table.put(SpectrumAdapter.channelValues, new float[] {3.74f, 10.8f, 12.0f});
           table.put(SpectrumAdapter.bandNames, new String[] {"CH3b", "CH4", "CH5"});
           table.put(SpectrumAdapter.channelType, "wavelength");
           // Create a 3 emissive channel view of the multiChannel array 'hrpt'
           table.put(SpectrumAdapter.channelIndices_name, new int[] {2, 3, 4});
           
           SpectrumAdapter spectrumAdapter0 = new SpectrumAdapter(reader, table);
           
           MultiSpectralData multiSpectData0 = new MultiSpectralData(swathAdapter0, spectrumAdapter0, "BrightnessTemp", "BrightnessTemp", null, null);
           multiSpectData0.setInitialWavenumber(10.8f);
           
           multiSpectData_s.add(multiSpectData0);
           
           //Reflective Bands
           table = SwathAdapter.getEmptyMetadataTable();
           table.put("array_name", "Data/hrpt");
           table.put("lon_array_name", "Geolocation/Longitude");
           table.put("lat_array_name", "Geolocation/Latitude");
           table.put("Track", "dim0");
           table.put("XTrack", "dim1");
           table.put("geo_Track", "dim0");
           table.put("geo_XTrack", "dim1");
           table.put(SwathAdapter.geo_track_offset_name, Double.toString(0.0));
           table.put(SwathAdapter.geo_xtrack_offset_name, Double.toString(24.0));
           table.put(SwathAdapter.geo_track_skip_name, Double.toString(1.0));
           table.put(SwathAdapter.geo_xtrack_skip_name, Double.toString(40.0));
           table.put(SwathAdapter.geo_scale_name, "Scale");
           table.put(SwathAdapter.geo_fillValue_name, "FillValue");
           table.put("range_name", "Reflective_Bands");
           table.put("product_name", "AVHRR_L1B_AAPP");
           table.put(SpectrumAdapter.channelIndex_name, "dim2");
           
           SwathAdapter swathAdapter1 = new SwathAdapter(reader, table);
           
           table = SpectrumAdapter.getEmptyMetadataTable();
           table.put(SpectrumAdapter.array_name, "Data/hrpt");
           table.put(SpectrumAdapter.channelIndex_name, "dim2");
           table.put(SpectrumAdapter.x_dim_name, "dim1");
           table.put(SpectrumAdapter.y_dim_name, "dim0");
           table.put(SpectrumAdapter.channelValues, new float[] {0.63f, 0.862f, 1.61f});
           table.put(SpectrumAdapter.bandNames, new String[] {"CH1", "CH2", "CH3a"});
           table.put(SpectrumAdapter.channelType, "wavelength");
           // create a 3 reflective channel view of the multiChannel array (dim=5) hrpt
           table.put(SpectrumAdapter.channelIndices_name, new int[] {0, 1, 2});
           
           SpectrumAdapter spectrumAdapter1 = new SpectrumAdapter(reader, table);
           
           MultiSpectralData multiSpectData1 = new MultiSpectralData(swathAdapter1, spectrumAdapter1, "Reflectance", "Reflectance", null, null);
           multiSpectData1.setInitialWavenumber(0.862f);
           
           multiSpectData_s.add(multiSpectData1);
       }
       else {
          HashMap table = SwathAdapter.getEmptyMetadataTable();
          table.put("array_name", "MODIS_SWATH_Type_L1B/Data_Fields/EV_1KM_Emissive");
          table.put("lon_array_name", "pixel_longitude");
          table.put("lat_array_name", "pixel_latitude");
          table.put("XTrack", "elements");
          table.put("Track", "lines");
          table.put("geo_Track", "lines");
          table.put("geo_XTrack", "elements");
          table.put("scale_name", "scale_factor");
          table.put("offset_name", "add_offset");
          table.put("fill_value_name", "_FillValue");
          swathAdapter = new SwathAdapter(reader, table);
          categories = DataCategory.parseCategories("2D grid;GRID-2D;");
          defaultSubset = swathAdapter.getDefaultSubset();
       }
       setProperties(properties);
    }

    public void initAfterUnpersistence() {
      try {
        setup();
      } 
      catch (Exception e) {
      }
    }

    /**
     * Make and insert the <code>DataChoice</code>-s for this
     * <code>DataSource</code>.
     */
    public void doMakeDataChoices() {
        try {
          for (int k=0; k<multiSpectData_s.size(); k++) {
            MultiSpectralData adapter = multiSpectData_s.get(k);
            DataChoice choice = doMakeDataChoice(k, adapter);
            adapterMap.put(choice.getName(), adapter);
            addDataChoice(choice);
          }
        }
        catch(Exception e) {
          e.printStackTrace();
        }
    }

    private DataChoice doMakeDataChoice(int idx, MultiSpectralData adapter) throws Exception {
        String name = "_    ";
        DataSelection dataSel = new MultiDimensionSubset();
        if (adapter != null) {
          name = adapter.getName();
          //dataSel = new MultiDimensionSubset(defaultSubset);
          dataSel = new MultiDimensionSubset(adapter.getDefaultSubset());
        }

        Hashtable subset = new Hashtable();
        subset.put(MultiDimensionSubset.key, dataSel);
        if (adapter != null) {
          subset.put(MultiSpectralDataSource.paramKey, adapter.getParameter());
        }

        DirectDataChoice ddc = new DirectDataChoice(this, new Integer(idx), name, name, categories, subset);
        ddc.setProperties(subset);
        return ddc;
    }

    /**
     * Check to see if this <code>HDFHydraDataSource</code> is equal to the object
     * in question.
     * @param o  object in question
     * @return true if they are the same or equivalent objects
     */
    public boolean equals(Object o) {
        if ( !(o instanceof MultiSpectralDataSource)) {
            return false;
        }
        return (this == (MultiSpectralDataSource) o);
    }

    public MultiSpectralData getMultiSpectralData() {
      return multiSpectData;
    }

    public MultiSpectralData getMultiSpectralData(DataChoice choice) {
      return adapterMap.get(choice.getName());
    }

    public MultiSpectralData getMultiSpectralData(String name) {
      return adapterMap.get(name);
    }

    public MultiSpectralData getMultiSpectralData(int idx) {
      return multiSpectData_s.get(idx);
    }

    public String getDatasetName() {
      return filename;
    }

    public void setDatasetName(String name) {
      filename = name;
    }

//    public ComboDataChoice getComboDataChoice() {
//      return comboChoice;
//    }

    public String getDateTime() {
      return dateTime;
    }

    /**
     * Called by the IDV's persistence manager in an effort to collect all of
     * the files that should be included in a zipped bundle.
     * 
     * @return Singleton list containing the file that this data source came from.
     */
    @Override public List getDataPaths() {
        return Collections.singletonList(filename);
    }

  /**
    public HashMap getSubsetFromLonLatRect(MultiDimensionSubset select, GeoSelection geoSelection) {
      GeoLocationInfo ginfo = geoSelection.getBoundingBox();
      return adapters[0].getSubsetFromLonLatRect(select.getSubset(), ginfo.getMinLat(), ginfo.getMaxLat(),
                                        ginfo.getMinLon(), ginfo.getMaxLon());
    }
   */

    public synchronized Data getData(String name, HashMap subset) throws VisADException, RemoteException {
      MultiSpectralData msd =  getMultiSpectralData(name);
      Data data = null;
      try {
        data = msd.getImage(subset);
      } catch (Exception e) {
        e.printStackTrace();
      }
      return data;
    }


    public synchronized Data getData(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {
       return this.getDataInner(dataChoice, category, dataSelection, requestProperties);

    }

    protected Data getDataInner(DataChoice dataChoice, DataCategory category,
                                DataSelection dataSelection, Hashtable requestProperties)
                                throws VisADException, RemoteException {

        //- this hack keeps the HydraImageProbe from doing a getData()
        //- TODO: need to use categories?
        if (requestProperties != null) {
          if ((requestProperties.toString()).contains("ReadoutProbe")) {
            return null;
          }
        }

        GeoLocationInfo ginfo = null;
        GeoSelection geoSelection = null;
        
        if ((dataSelection != null) && (dataSelection.getGeoSelection() != null)) {
          if (dataSelection.getGeoSelection().getBoundingBox() != null) {
            geoSelection = dataSelection.getGeoSelection();
          }
          else if (dataChoice.getDataSelection() != null) {
            geoSelection = dataChoice.getDataSelection().getGeoSelection();
          }
        }

        if (geoSelection != null) {
          ginfo = geoSelection.getBoundingBox();
        }

        Data data = null;

        try {
            HashMap subset = null;
            if (ginfo != null) {
              subset = swathAdapter.getSubsetFromLonLatRect(ginfo.getMinLat(), ginfo.getMaxLat(),
                                        ginfo.getMinLon(), ginfo.getMaxLon());
            }
            else {
              MultiDimensionSubset select = null;
              Hashtable table = dataChoice.getProperties();
              Enumeration keys = table.keys();
              while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if (key instanceof MultiDimensionSubset) {
                  select = (MultiDimensionSubset) table.get(key);
                }
              }  
              if (select != null) {
                subset = select.getSubset();
              }

              if (dataSelection != null) {
                  Hashtable props = dataSelection.getProperties();
                  if (props != null) {
                    if (props.containsKey(MultiDimensionSubset.key)) {
                      subset = (HashMap)((MultiDimensionSubset)props.get(MultiDimensionSubset.key)).getSubset();
                    }
                    else {
                      subset = defaultSubset;
                    }
                    if (props.containsKey(SpectrumAdapter.channelIndex_name)) {
                      int idx = ((Integer) props.get(SpectrumAdapter.channelIndex_name)).intValue();
                      double[] coords = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
                      if (coords == null) {
                        coords = new double[] {(double)idx, (double)idx, (double)1};
                        subset.put(SpectrumAdapter.channelIndex_name, coords);
                      }
                      else {
                        coords[0] = (double)idx;
                        coords[1] = (double)idx;
                        coords[2] = (double)1;
                      }
                   }
                 }
               }
            }

            if (subset != null) {
              MultiSpectralData multiSpectData = getMultiSpectralData(dataChoice);
              if (multiSpectData != null) {
                data = multiSpectData.getImage(subset);
                data = applyProperties(data, requestProperties, subset);
              }
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("getData exception e=" + e);
        }
        return data;
    }


    protected Data applyProperties(Data data, Hashtable requestProperties, HashMap subset) 
          throws VisADException, RemoteException {
      Data new_data = data;

      if (requestProperties == null) {
        new_data = data;
        return new_data;
      }
      return new_data;
    }

    protected void initDataSelectionComponents(
         List<DataSelectionComponent> components,
             final DataChoice dataChoice) {

/*
      if (System.getProperty("os.name").equals("Mac OS X") && hasImagePreview && hasChannelSelect) {
        try {
          components.add(new ImageChannelSelection(new PreviewSelection(dataChoice, previewImage, null), new ChannelSelection(dataChoice)));
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
      else {
        if (hasImagePreview) {
          try {
            previewSelection = new PreviewSelection(dataChoice, previewImage, null);
            components.add(previewSelection);
          } catch (Exception e) {
            System.out.println("Can't make PreviewSelection: "+e);
            e.printStackTrace();
          }
        }
        if (hasChannelSelect) {
          try {
            components.add(new ChannelSelection(dataChoice));
          } 
          catch (Exception e) {
            e.printStackTrace();
          }
        }
      }
*/
    }


   public static MapProjection getSwathProjection(FlatField image, float[][] corners) throws VisADException, RemoteException {
      MapProjection mp = null;
      FunctionType fnc_type = (FunctionType) image.getType();
      RealTupleType rtt = fnc_type.getDomain();
      CoordinateSystem cs = rtt.getCoordinateSystem();
      Gridded2DSet domainSet = (Gridded2DSet) image.getDomainSet();

      if (cs instanceof visad.CachingCoordinateSystem) {
         cs = ((visad.CachingCoordinateSystem)cs).getCachedCoordinateSystem();
      }

      if (cs instanceof LongitudeLatitudeCoordinateSystem) {
         try {
           mp = new LambertAEA(corners);
         } catch (Exception e) {
           System.out.println(" getDataProjection"+e);
         }
         return mp;
      }
      else {
         return null;
      }
   }


  public static Linear2DSet makeGrid(MapProjection mp, double res) throws Exception {
    Rectangle2D rect = mp.getDefaultMapArea();

    int xLen = (int) (rect.getWidth()/res);
    int yLen = (int) (rect.getHeight()/res);

    RealType xmap = RealType.getRealType("xmap", CommonUnit.meter);
    RealType ymap = RealType.getRealType("ymap", CommonUnit.meter);

    RealTupleType rtt = new visad.RealTupleType(xmap, ymap, mp, null);

    Linear2DSet grid = new Linear2DSet(rtt, rect.getX(), (xLen-1)*res, xLen,
		                            rect.getY(), (yLen-1)*res, yLen);
    return grid;
  }

  public static Linear2DSet makeGrid(MapProjection mp, float[][] corners, float res) throws Exception {
     float[][] xy = mp.fromReference(corners);

     float min_x = Float.MAX_VALUE;
     float min_y = Float.MAX_VALUE;
     float max_x = -Float.MAX_VALUE;
     float max_y = -Float.MAX_VALUE;

     for (int k=0; k<xy[0].length;k++) {
       if (xy[0][k] < min_x) min_x = xy[0][k];
       if (xy[1][k] < min_y) min_y = xy[1][k];
       if (xy[0][k] > max_x) max_x = xy[0][k];
       if (xy[1][k] > max_y) max_y = xy[1][k];
     }

     RealType xmap = RealType.getRealType("xmap", CommonUnit.meter);
     RealType ymap = RealType.getRealType("ymap", CommonUnit.meter);

     RealTupleType rtt = new visad.RealTupleType(xmap, ymap, mp, null);

     min_x = ((int) (min_x/res)) * res;
     max_x = ((int) (max_x/res)) * res;
     min_y = ((int) (min_y/res)) * res;
     max_y = ((int) (max_y/res)) * res;

     float del_x = max_x - min_x;
     float del_y = max_y - min_y;

     int xLen = (int) (del_x/res);
     int yLen = (int) (del_y/res);

     Linear2DSet grid = new Linear2DSet(rtt, min_x, min_x + (xLen-1)*res, xLen,
                                             min_y, min_y + (yLen-1)*res, yLen);

     return grid;
  }

  
 public static Date getMODISdateFromFilename(String filename) {
    Date date = null;
    try {
       if (filename.startsWith("MOD") || filename.startsWith("MYD")) {
           
           String yyyydddhhmm = filename.substring(10,22);
           SimpleDateFormat sdf = new SimpleDateFormat("yyyyDDD.HHmm");
           sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
           date = sdf.parse(yyyydddhhmm);
        }
        else if (filename.startsWith("a1") || filename.startsWith("t1")) {
            
           String yydddhhmm = filename.substring(3,13);
           SimpleDateFormat sdf = new SimpleDateFormat("yyDDD.HHmm");
           sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
           date = sdf.parse(yydddhhmm);
        }
    }
    catch (Exception e) {
        e.printStackTrace();
    }
    
    return date;
 }


 public static boolean validLonLat(float[][] lonlat) {
   float lon = lonlat[0][0];
   float lat = lonlat[1][0];
   return ((lon >= -180f && lon <= 360f) && (lat >= -90f && lat <= 90f));
 }

}
