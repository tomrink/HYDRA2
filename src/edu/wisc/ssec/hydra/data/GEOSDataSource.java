package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.hydra.GEOSProjection;
import edu.wisc.ssec.hydra.GEOSTransform;

import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.HDFArray;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.adapter.MultiDimensionReader;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.RangeProcessor;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.rmi.RemoteException;

import ucar.nc2.Variable;
import ucar.nc2.Attribute;

import visad.Data;
import visad.Linear2DSet;
import visad.RealTupleType;
import visad.RealType;
import visad.Set;
import visad.VisADException;
import visad.georef.MapProjection;

public class GEOSDataSource extends DataSource {

  NetCDFFile reader;
  ArrayList<Variable> projVarList = new ArrayList<Variable>();
  ArrayList<Variable> varsWithProj = new ArrayList<Variable>();
  HashMap<String, Variable> projXCoordVars = new HashMap<String, Variable>();
  HashMap<String, Variable> projYCoordVars = new HashMap<String, Variable>();
  HashMap<String, Variable> timeCoordVars = new HashMap<String, Variable>();

  private ArrayList<DataChoice> myDataChoices = new ArrayList<DataChoice>();
  private ArrayList<GOESGridAdapter> adapters = new ArrayList<GOESGridAdapter>();
  
  double default_stride = 10;
  
  String dateTimeStamp;
  
  public GEOSDataSource(File file) {
     this(file, 4);
  }

  public GEOSDataSource(File file, double default_stride) {
    
    this.default_stride = default_stride;
    this.dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
    
    try {
      init(file.getPath());
    } 
    catch (Exception e) {
      e.printStackTrace();
    }
  }

  public void init(String filename) throws Exception {
     reader = new NetCDFFile(filename);

     HashMap varMap = reader.getVarMap();
     Iterator<Variable> iter = varMap.values().iterator();
     while (iter.hasNext()) {
        Variable var = iter.next();
        String varName = var.getShortName();
        int[] varDims = reader.getDimensionLengths(varName);
        int rank = varDims.length;

        Attribute attr = var.findAttribute("grid_mapping_name");
        if (attr != null) {
           projVarList.add(var);
        }
        else if (var.findAttribute("grid_mapping") != null) {
           varsWithProj.add(var);
        }
        else {
           attr = var.findAttribute("standard_name");
           if (attr != null) {
              String stndName = attr.getStringValue();
              if (stndName.equals("projection_x_coordinate")) {
                 projXCoordVars.put(varName, var);
              }
              else if (stndName.equals("projection_y_coordinate")) {
                 projYCoordVars.put(varName, var);
              }
              else if (stndName.equals("time")) {
                 timeCoordVars.put(varName, var);
              }
           }
           else {
              varsWithProj.add(var);
           }
        }

        if (rank == 1) {
           attr = var.findAttribute("units");
           String[] dimNames = reader.getDimensionNames(varName);
           if (attr != null) {
              String str = attr.getStringValue();
              visad.Unit unit = null;
              try {
                 unit = visad.data.units.Parser.parse(str);
              }
              catch (Exception e) {
              }
              if (unit != null && unit.isConvertible(visad.SI.second)) {
                 if (varName.equals(dimNames[0])) {
                    timeCoordVars.put(varName, var);
                 }
              }
           }
        }
     }

     iter = varsWithProj.iterator();
     while (iter.hasNext()) {
        Variable var = iter.next();
        String varName = var.getShortName();
        if (varName.contains("longitude") || varName.contains("latitude")) { // don't want to display these
           continue;
        }
        Attribute attr = var.findAttribute("coordinates");
        if (attr != null) {
           String str = attr.getStringValue();
           String[] strs = str.split(" ");
        }
        
        String[] dimNames = reader.getDimensionNames(varName);
        
        Variable varX = null;
        Variable varY = null;
        Variable varT = null;
        
        for (int k=0; k<dimNames.length; k++) {
           Iterator itr = projXCoordVars.keySet().iterator();
           while (itr.hasNext()) {
              Object key = itr.next();
              Variable vr = projXCoordVars.get(key);
              String name = vr.getShortName();
              String coordDimName = reader.getDimensionNames(name)[0];
              if (dimNames[k].equals(coordDimName)) {
                 varX = vr;
                 break;
              }
           }
           
           itr = projYCoordVars.keySet().iterator();
           while (itr.hasNext()) {
              Object key = itr.next();
              Variable vr = projYCoordVars.get(key);
              String name = vr.getShortName();
              String coordDimName = reader.getDimensionNames(name)[0];
              if (dimNames[k].equals(coordDimName)) {
                 varY = vr;
                 break;
              }
           }
           
           itr = timeCoordVars.keySet().iterator();
           while (itr.hasNext()) {
              Object key = itr.next();
              Variable vr = timeCoordVars.get(key);
              String name = vr.getShortName();
              String coordDimName = reader.getDimensionNames(name)[0];
              if (dimNames[k].equals(coordDimName)) {
                 varT = vr;
                 break;
              }
           }          

        }

        Variable projVar = projVarList.get(0); //TODO: may be more than one 

        if (varX != null && varY != null) {
           GEOSInfo geosInfo = new GEOSInfo(reader, var, projVar, varT, varX, varY);
           String name = var.getShortName();

            HashMap metadata = GOESGridAdapter.getEmptyMetadataTable();
            metadata.put(MultiDimensionAdapter.array_name, geosInfo.getName());
            metadata.put(GOESGridAdapter.gridX_name, geosInfo.getXDimName());
            metadata.put(GOESGridAdapter.gridY_name, geosInfo.getYDimName());
            metadata.put(MultiDimensionAdapter.fill_value_name, "_FillValue");

            GOESGridAdapter goesAdapter = new GOESGridAdapter(reader, metadata, geosInfo.getMapProjection(), default_stride);
            HashMap subset = goesAdapter.getDefaultSubset();
            if (geosInfo.getTDimName() != null) {
               subset.put(geosInfo.getTDimName(), new double[] {0.0, 0.0, 1.0});
            }
            DataSelection dataSel = new MultiDimensionSubset(subset);
            DataChoice dataChoice = new DataChoice(this, name, null);
            dataChoice.setDataSelection(dataSel);
            addDataChoice(dataChoice);
            adapters.add(goesAdapter);
        }
     }

  }
  
  public String getDateTimeStamp() {
     return dateTimeStamp;
  }
  
  public boolean getDoReproject(DataChoice choice) {
     return false;
  }
  
  public void addDataChoice(DataChoice dataChoice) {
      myDataChoices.add(dataChoice); 
  }
  
  public List getDataChoices() {
     return myDataChoices; 
  }
  

  public Data getData(DataChoice dataChoice, DataSelection dataSelection)
      throws VisADException, RemoteException
  {
      try {
         ArrayList dataChoices = (ArrayList) getDataChoices();
         int idx = dataChoices.indexOf(dataChoice);
         GOESGridAdapter adapter = adapters.get(idx);

         MultiDimensionSubset select = (MultiDimensionSubset)dataChoice.getDataSelection();
         HashMap subset = select.getSubset();

         return adapter.getData(subset);
      } catch (Exception e) {
         e.printStackTrace();
      }
      return null;
  }

}

class GEOSInfo {

   String xVarName;
   String xDimName; 
   String yVarName;
   String yDimName;
   String varName;
   String projVarName;
   String tVarName;
   String tDimName;

   int xDimLen;
   int yDimLen;
   int tDimLen;

   double subLonDegrees;

   MapProjection mapProj;


   public GEOSInfo(NetCDFFile reader, Variable var, Variable projVar, Variable timeCoordVar, Variable xCoordVar, Variable yCoordVar) throws Exception {
   
      varName = var.getShortName();

      xVarName = xCoordVar.getShortName();
      yVarName = yCoordVar.getShortName();

      xDimName = (reader.getDimensionNames(xVarName))[0];
      yDimName = (reader.getDimensionNames(yVarName))[0];

      xDimLen = reader.getDimensionLength(xDimName);
      yDimLen = reader.getDimensionLength(yDimName);
      
      if (timeCoordVar != null) {
         tVarName = timeCoordVar.getShortName();
         tDimName = (reader.getDimensionNames(tVarName))[0];
         tDimLen = reader.getDimensionLength(tDimName);      
      }
      
      double scale_x = 5.588799029559623E-5;
      double offset_x = -0.15371991730803744;
      double scale_y = 5.588799029559623E-5;
      double offset_y = -0.15371991730803744;
      
      HDFArray obj = (HDFArray) reader.getArrayAttribute(xVarName, "scale_factor");
      if (obj != null) {
         scale_x = ((double[]) obj.getArray())[0];
      }
      obj = (HDFArray) reader.getArrayAttribute(xVarName, "add_offset");
      if (obj != null) {
         offset_x = ((double[]) obj.getArray())[0];
      }

      obj = (HDFArray) reader.getArrayAttribute(yVarName, "scale_factor");
      if (obj != null) {
         scale_y = ((double[]) obj.getArray())[0];
      }
      obj = (HDFArray) reader.getArrayAttribute(yVarName, "add_offset");
      if (obj != null) {
         offset_y = ((double[]) obj.getArray())[0];
      }
      
      obj = (HDFArray) reader.getArrayAttribute(projVar.getShortName(), "longitude_of_projection_origin");
      if (obj.getType().equals(Double.TYPE)) {
         subLonDegrees = ((double[]) obj.getArray())[0];
      }
      else if (obj.getType().equals(Float.TYPE)) {
         subLonDegrees = (double) ((float[]) obj.getArray())[0];
      }

      obj = (HDFArray) reader.getArrayAttribute(projVar.getShortName(), "sweep_angle_axis");
      String sweepAngleAxis = ((String[]) obj.getArray())[0];

      // TODO: Determine display north/south orientation from coord vars mapped to radians.
      double inverty = 1.0;

      if (sweepAngleAxis.equals("x")) {
         sweepAngleAxis = "GOES";
      }
      else if (sweepAngleAxis.equals("y")) {
         sweepAngleAxis = "GEOS";
      }
      sweepAngleAxis = "GEOS"; // TODO: investigate this. Seems to work better for AHI than using file indicated GOES

      GEOSTransform geosTran = new GEOSTransform(subLonDegrees, sweepAngleAxis);

      mapProj = new GEOSProjection(geosTran, 0.0, 0.0, (double)xDimLen, (double)yDimLen, 
                        scale_x, offset_x, inverty*scale_y, inverty*offset_y);
   }


   public String getName() {
      return varName;
   }
   public String getXDimName() {
      return xDimName;
   }
   public String getYDimName() {
      return yDimName;
   }
   public String getTDimName() {
      return tDimName;
   }

   public MapProjection getMapProjection() {
      return mapProj;
   }

}

class GOESGridAdapter extends MultiDimensionAdapter {

   public static String gridX_name = "GridX";
   public static String gridY_name = "GridY";

   RealType gridx = RealType.getRealType(gridX_name);
   RealType gridy = RealType.getRealType(gridY_name);
   RealType[] domainRealTypes = new RealType[2];

   int GridXLen;
   int GridYLen;

   int gridx_idx;
   int gridy_idx;
   int gridx_tup_idx;
   int gridy_tup_idx;

   MapProjection mapProj;

   double default_stride = 10;

   public static HashMap getEmptySubset() {
     HashMap<String, double[]> subset = new HashMap<String, double[]>();
     subset.put(gridY_name, new double[3]);
     subset.put(gridX_name, new double[3]);
     return subset;
   }

   public static HashMap<String, Object> getEmptyMetadataTable() {
     HashMap<String, Object> metadata = new HashMap<String, Object>();
     metadata.put(array_name, null);
     metadata.put(gridX_name, null);
     metadata.put(gridY_name, null);
     metadata.put(scale_name, null);
     metadata.put(offset_name, null);
     metadata.put(fill_value_name, null);
     metadata.put(range_name, null);
     return metadata;
   }

   public GOESGridAdapter(MultiDimensionReader reader, HashMap metadata, MapProjection mapProj, double default_stride) {
     super(reader, metadata);
     this.mapProj = mapProj;
     this.default_stride = default_stride;
     this.init();
   }

   private void init() {
     HashMap metadata = (HashMap) getMetadata();

     gridx_idx = getIndexOfDimensionName((String)metadata.get(gridX_name));
     GridXLen = getDimensionLengthFromIndex(gridx_idx);

     gridy_idx = getIndexOfDimensionName((String)metadata.get(gridY_name));
     GridYLen = getDimensionLengthFromIndex(gridy_idx);

     int[] lengths = new int[2];

     if (gridy_idx < gridx_idx) {
       domainRealTypes[0] = gridx;
       domainRealTypes[1] = gridy;
       lengths[0] = GridXLen;
       lengths[1] = GridYLen;
       gridy_tup_idx = 1;
       gridx_tup_idx = 0;
     }
     else {
       domainRealTypes[0] = gridy;
       domainRealTypes[1] = gridx;
       lengths[0] = GridYLen;
       lengths[1] = GridXLen;
       gridy_tup_idx = 0;
       gridx_tup_idx = 1;
     }

     lengths[gridy_tup_idx] = GridYLen;
     lengths[gridx_tup_idx] = GridXLen;
     
     try {
        setRangeProcessor(new RangeProcessor(getReader(), metadata));
     }
     catch (Exception e) {
        System.out.println("RangeProcessor failed to create.");
     }
     
   }

   public Set makeDomain(Object subset) throws Exception {
     double[] first = new double[2];
     double[] last = new double[2];
     int[] length = new int[2];

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
       // invert south to north (GEOS) orientation (for GOES)
       if (name.equals("GridY")) { //TODO: need to a general way to handle this. Check GEOSInfo?
          double tmp = coords[0];
          coords[0] = GridYLen - coords[1];
          coords[1] = GridYLen - tmp;
       }
     }

     mapProj = new GEOSProjection((GEOSProjection)mapProj, first[0], first[1], last[0]-first[0], last[1]-first[1]);
     RealTupleType domainTupType = new RealTupleType(domainRealTypes[0], domainRealTypes[1], mapProj, null);
     //TODO: need to handle this properly GOES: North to South, GEOS: South to North
     //Linear2DSet domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], first[1], last[1], length[1]);
     Linear2DSet domainSet = new Linear2DSet(domainTupType, first[0], last[0], length[0], last[1], first[1], length[1]);

     return domainSet;
   }

   public HashMap getDefaultSubset() {
     HashMap subset = GOESGridAdapter.getEmptySubset();

     double[] coords = (double[])subset.get(gridY_name);
     coords[0] = 0.0;
     coords[1] = GridYLen - 1;
     coords[2] = default_stride;
     subset.put(gridY_name, coords);

     coords = (double[])subset.get(gridX_name);
     coords[0] = 0.0;
     coords[1] = GridXLen - 1 ;
     coords[2] = default_stride;
     subset.put(gridX_name, coords);

     return subset;
  }
}
