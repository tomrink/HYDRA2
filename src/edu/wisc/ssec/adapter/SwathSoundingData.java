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

public class SwathSoundingData extends MultiDimensionAdapter {

  SwathAdapter swathAdapter = null;
  AtmSoundingAdapter soundingAdapter = null;
  CoordinateSystem cs = null;

  HashMap soundingSelect = null;
  String sensorName = null;
  String platformName = null;
  String paramName = null;
  String inputParamName = null;
  String name = null;

  public float init_level = 700f;
  public String init_bandName = null;

  float[] dataRange = new float[] {180f, 320f};

  
  public SwathSoundingData(SwathAdapter swathAdapter, AtmSoundingAdapter soundingAdapter,
                           String inputParamName, String paramName, String sensorName, String platformName) {
    this.swathAdapter = swathAdapter;
    this.soundingAdapter = soundingAdapter;
    this.paramName = paramName;
    this.inputParamName = inputParamName;
    this.name = swathAdapter.getArrayName();

    if (soundingAdapter != null) {
      this.soundingSelect = soundingAdapter.getDefaultSubset();
      try {
        setInitialLevel(init_level);
      } 
      catch (Exception e) {
        e.printStackTrace();
        System.out.println("could not initialize initial wavenumber");
      }
    }

    this.sensorName = sensorName;
    this.platformName = platformName;
  }

  public SwathSoundingData(SwathAdapter swathAdapter, AtmSoundingAdapter soundingAdapter,
                           String sensorName, String platformName) {
    this(swathAdapter, soundingAdapter, "Radiance", "BrightnessTemp", sensorName, platformName);
  }

  public SwathSoundingData(SwathAdapter swathAdapter, AtmSoundingAdapter soundingAdapter) {
    this(swathAdapter, soundingAdapter, null, null);
  }

  public SwathSoundingData() {
    this(null, null, null, null);
  }

  public FlatField getSounding(int[] coords) 
      throws Exception, VisADException, RemoteException {
    if (coords == null) return null;
    if (soundingAdapter == null) return null;
    soundingSelect.put(AtmSoundingAdapter.x_dim_name, new double[] {(double)coords[0], (double)coords[0], 1.0});
    soundingSelect.put(AtmSoundingAdapter.y_dim_name, new double[] {(double)coords[1], (double)coords[1], 1.0});

    FlatField sounding = soundingAdapter.getData(soundingSelect);
    return sounding;
  }

  public FlatField getSounding(RealTuple location) 
      throws Exception, VisADException, RemoteException {
    if (soundingAdapter == null) return null;
    int[] coords = getSwathCoordinates(location, cs);
    if (coords == null) return null;
    soundingSelect.put(AtmSoundingAdapter.x_dim_name, new double[] {(double)coords[0], (double)coords[0], 1.0});
    soundingSelect.put(AtmSoundingAdapter.y_dim_name, new double[] {(double)coords[1], (double)coords[1], 1.0});

    FlatField sounding = soundingAdapter.getData(soundingSelect);
    return sounding;
  }

  public FlatField getImage(HashMap subset) 
    throws Exception, VisADException, RemoteException {
    FlatField image = swathAdapter.getData(subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();

    int levelIndex = (int) ((double[])subset.get(AtmSoundingAdapter.levelIndex_name))[0];
    float level = soundingAdapter.getLevelFromLevelIndex(levelIndex);

    return image;
    //return convertImage(image, level, paramName);
  }

  public FlatField getImage(float level, HashMap subset) 
      throws Exception, VisADException, RemoteException {
    if (soundingAdapter == null) { 
       return getImage(subset);
    }
    int levelIndex = soundingAdapter.getLevelIndexFromLevel(level);
    subset.put(AtmSoundingAdapter.levelIndex_name, new double[] {(double)levelIndex, (double)levelIndex, 1.0});
    FlatField image = swathAdapter.getData(subset);
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();
    return image;
    //return convertImage(image, channel, paramName);
  }

  public FlatField getData(Object subset) throws Exception {
    return getImage((HashMap)subset);
  }

  public Set makeDomain(Object subset) throws Exception {
    throw new Exception("makeDomain unimplented");
  } 


  public void setDataRange(float[] range) {
    dataRange = range;
  }

  public float[] getDataRange() {
    return dataRange;
  }

  public String getParameter() {
    return paramName;
  }

  public String getName() {
    return name;
  }

  public CoordinateSystem getCoordinateSystem() {
    return cs;
  }

  public void setCoordinateSystem(CoordinateSystem cs) {
    this.cs = cs;
  }

  public float[] getSoundingLevels() {
    return soundingAdapter.getLevels();
  }

  public void setInitialLevel(float val) {
    init_level = val;
  }

  public int[] getSwathCoordinates(RealTuple location, CoordinateSystem cs) 
      throws VisADException, RemoteException {
    if (location == null) return null;
    if (cs == null) return null;
    Real[] comps = location.getRealComponents();
    //- trusted: latitude:0, longitude:1
    float lon = (float) comps[1].getValue();
    float lat = (float) comps[0].getValue();
    if (lon < -180) lon += 360f;
    if (lon > 180) lon -= 360f;
    float[][] xy = cs.fromReference(new float[][] {{lon}, {lat}});
    if ((Float.isNaN(xy[0][0])) || Float.isNaN(xy[1][0])) return null;
    Set domain = swathAdapter.getSwathDomain();
    int[] idx = domain.valueToIndex(xy);
    xy = domain.indexToValue(idx);
    int[] coords = new int[2];
    coords[0] = (int) xy[0][0];
    coords[1] = (int) xy[1][0];
    if ((coords[0] < 0)||(coords[1] < 0)) return null;
    return coords;
  }

  public RealTuple getEarthCoordinates(float[] xy)
      throws VisADException, RemoteException {
    float[][] tup = cs.toReference(new float[][] {{xy[0]}, {xy[1]}});
    return new RealTuple(RealTupleType.SpatialEarth2DTuple, new double[] {(double)tup[0][0], (double)tup[1][0]});
  }

  public int getLevelIndexFromLevel(float level) throws Exception {
    return soundingAdapter.getLevelIndexFromLevel(level);
  }

  public float getLevelFromLevelIndex(int index) throws Exception {
    return soundingAdapter.getLevelFromLevelIndex(index);
  }

  public Rectangle2D getLonLatBoundingBox(CoordinateSystem cs) {
    return null;
  }

  public Rectangle2D getLonLatBoundingBox(HashMap subset) 
      throws Exception {
    Set domainSet = swathAdapter.makeDomain(subset);
    return getLonLatBoundingBox(domainSet);
  }

  public static Rectangle2D getLonLatBoundingBox(FlatField field) {
    Set domainSet = field.getDomainSet();
    return getLonLatBoundingBox(domainSet);
  }

  public static float[][] getLonLatBoundingCorners(Set domainSet) {
    CoordinateSystem cs =
      ((SetType)domainSet.getType()).getDomain().getCoordinateSystem();

    float start0, stop0, start1, stop1;
    int len0, len1;
    float minLon = Float.MAX_VALUE;
    float minLat = Float.MAX_VALUE;
    float maxLon = -Float.MAX_VALUE;
    float maxLat = -Float.MAX_VALUE;

    float[][] corners = null;

    if (domainSet instanceof Linear2DSet) {
      Linear1DSet lset = ((Linear2DSet)domainSet).getLinear1DComponent(0);
      start0 = (float) lset.getFirst();
      stop0 = (float) lset.getLast();
      len0 = lset.getLengthX();
      lset = ((Linear2DSet)domainSet).getLinear1DComponent(1);
      start1 = (float) lset.getFirst();
      stop1 = (float) lset.getLast();
      len1 = lset.getLengthX();

      float x, y, del_x, del_y;
      float lonA = Float.NaN;
      float lonB = Float.NaN;
      float lonC = Float.NaN;
      float lonD = Float.NaN;
      float latA = Float.NaN;
      float latB = Float.NaN;
      float latC = Float.NaN;
      float latD = Float.NaN;

      int nXpts = len0/1;
      int nYpts = len1/1;

      del_x = (stop0 - start0)/nXpts;
      del_y = (stop1 - start1)/nYpts;
      x = start0;
      y = start1;
      try {
        for (int j=0; j<nYpts; j++) {
          y = start1+j*del_y;
          for (int i=0; i<nXpts; i++) {
            x = start0 + i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonA = lon;
              latA = lat;
              break;
            }
          }
          for (int i=0; i<nXpts; i++) {
            x = stop0 - i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonB = lon;
              latB = lat;
              break;
            }
          }
          if (!Float.isNaN(lonA) && !Float.isNaN(lonB)) {
            break;
          }
        }

        for (int j=0; j<nYpts; j++) {
          y = stop1-j*del_y;
          for (int i=0; i<nXpts; i++) {
            x = start0 + i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonC = lon;
              latC = lat;
              break;
            }
          }
          for (int i=0; i<nXpts; i++) {
            x = stop0 - i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonD = lon;
              latD = lat;
              break;
            }
          }
          if (!Float.isNaN(lonC) && !Float.isNaN(lonD)) {
            break;
          }
         }
         corners = new float[][] {{lonA,lonB,lonC,lonD},{latA,latB,latC,latD}};
         for (int k=0; k<corners[0].length; k++) {
            float lon = corners[0][k];
            float lat = corners[1][k];
            /**
            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
            */
         }
       } catch (Exception e) {
       }
    }
    else if (domainSet instanceof Gridded2DSet) {
      int[] lens = ((Gridded2DSet)domainSet).getLengths();
      start0 = 0f;
      start1 = 0f;
      stop0 = (float) lens[0];
      stop1 = (float) lens[1];

      float x, y, del_x, del_y;
      del_x = (stop0 - start0)/10;
      del_y = (stop1 - start1)/10;
      x = start0;
      y = start1;
      try {
        for (int j=0; j<11; j++) {
          y = start1+j*del_y;
          for (int i=0; i<11; i++) {
            x = start0+i*del_x;
            float[][] lonlat = ((Gridded2DSet)domainSet).gridToValue(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if ((lon > 180 || lon < -180) || (lat > 90 || lat < -90)) continue;
            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
          }
        }
      } catch (Exception e) {
      }
    }


    float del_lon = maxLon - minLon;
    float del_lat = maxLat - minLat;

    return corners;
  }

  public static Rectangle2D getLonLatBoundingBox(Set domainSet) {
    CoordinateSystem cs = 
      ((SetType)domainSet.getType()).getDomain().getCoordinateSystem();

    float start0, stop0, start1, stop1;
    int len0, len1;
    float minLon = Float.MAX_VALUE;
    float minLat = Float.MAX_VALUE;
    float maxLon = -Float.MAX_VALUE;
    float maxLat = -Float.MAX_VALUE;


    if (domainSet instanceof Linear2DSet) {
      Linear1DSet lset = ((Linear2DSet)domainSet).getLinear1DComponent(0);
      start0 = (float) lset.getFirst();
      stop0 = (float) lset.getLast();
      len0 = lset.getLengthX();
      lset = ((Linear2DSet)domainSet).getLinear1DComponent(1);
      start1 = (float) lset.getFirst();
      stop1 = (float) lset.getLast();
      len1 = lset.getLengthX();

      float x, y, del_x, del_y;
      float lonA = Float.NaN;
      float lonB = Float.NaN;
      float lonC = Float.NaN;
      float lonD = Float.NaN;
      float latA = Float.NaN;
      float latB = Float.NaN;
      float latC = Float.NaN;
      float latD = Float.NaN;

      int nXpts = len0/8;
      int nYpts = len1/8;

      del_x = (stop0 - start0)/nXpts;
      del_y = (stop1 - start1)/nYpts;

      x = start0;
      y = start1;
      try {
        for (int j=0; j<nYpts; j++) {
          y = start1+j*del_y;
          for (int i=0; i<nXpts; i++) {
            x = start0 + i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonA = lon;
              latA = lat;
              break;
            }
          }
          for (int i=0; i<nXpts; i++) {
            x = stop0 - i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonB = lon;
              latB = lat;
              break;
            }
          }
          if (!Float.isNaN(lonA) && !Float.isNaN(lonB)) {
            break;
          }
        }

        for (int j=0; j<nYpts; j++) {
          y = stop1-j*del_y;
          for (int i=0; i<nXpts; i++) {
            x = start0 + i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonC = lon;
              latC = lat;
              break;
            }
          }
          for (int i=0; i<nXpts; i++) {
            x = stop0 - i*del_x;
            float[][] lonlat = cs.toReference(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if (!Float.isNaN(lon) && !Float.isNaN(lat)) {
              lonD = lon;
              latD = lat;
              break;
            }
          }
          if (!Float.isNaN(lonC) && !Float.isNaN(lonD)) {
            break;
          }
         }
         float[][] corners = {{lonA,lonB,lonC,lonD},{latA,latB,latC,latD}};
         for (int k=0; k<corners[0].length; k++) {
            float lon = corners[0][k];
            float lat = corners[1][k];
            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
         }
       } catch (Exception e) {
       }
    }
    else if (domainSet instanceof Gridded2DSet) {
      int[] lens = ((Gridded2DSet)domainSet).getLengths();
      start0 = 0f;
      start1 = 0f;
      stop0 = (float) lens[0];
      stop1 = (float) lens[1];

      float x, y, del_x, del_y;
      del_x = (stop0 - start0)/10;
      del_y = (stop1 - start1)/10;
      x = start0;
      y = start1;
      try {
        for (int j=0; j<11; j++) {
          y = start1+j*del_y;
          for (int i=0; i<11; i++) {
            x = start0+i*del_x;
            float[][] lonlat = ((Gridded2DSet)domainSet).gridToValue(new float[][] {{x}, {y}});
            float lon = lonlat[0][0];
            float lat = lonlat[1][0];
            if ((lon > 180 || lon < -180) || (lat > 90 || lat < -90)) continue;
            if (lon < minLon) minLon = lon;
            if (lat < minLat) minLat = lat;
            if (lon > maxLon) maxLon = lon;
            if (lat > maxLat) maxLat = lat;
          }
        }
      } catch (Exception e) {
      }
    }
    

    float del_lon = maxLon - minLon;
    float del_lat = maxLat - minLat;

    return new Rectangle2D.Float(minLon, minLat, del_lon, del_lat);
  }

  public HashMap getDefaultSubset() {
    HashMap subset = swathAdapter.getDefaultSubset();
    double levIdx=0;

    try {
       levIdx = soundingAdapter.getLevelIndexFromLevel(init_level);
    }
    catch (Exception e) {
      System.out.println("couldn't get levIdx, using zero");
    }
      
    subset.put(AtmSoundingAdapter.levelIndex_name, new double[] {levIdx, levIdx, 1});
    return subset;
  }
 

  public AtmSoundingAdapter getAtmSoundingAdapter() {
    return soundingAdapter;
  }
}
