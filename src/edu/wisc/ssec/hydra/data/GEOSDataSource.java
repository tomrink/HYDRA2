package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.adapter.GOESGridAdapter;
import edu.wisc.ssec.hydra.GEOSProjection;
import edu.wisc.ssec.hydra.GEOSTransform;

import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.HDFArray;
import edu.wisc.ssec.adapter.MultiDimensionAdapter;
import edu.wisc.ssec.adapter.MultiDimensionSubset;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;
import java.rmi.RemoteException;

import ucar.nc2.Variable;
import ucar.nc2.Attribute;

import visad.Data;
import visad.VisADException;
import visad.georef.MapProjection;

public class GEOSDataSource extends DataSource {

  NetCDFFile reader;
  ArrayList<Variable> projVarList = new ArrayList<>();
  ArrayList<Variable> varsWithProj = new ArrayList<>();
  HashMap<String, Variable> projXCoordVars = new HashMap<>();
  HashMap<String, Variable> projYCoordVars = new HashMap<>();
  HashMap<String, Variable> timeCoordVars = new HashMap<>();

  private ArrayList<GOESGridAdapter> adapters = new ArrayList<>();
  
  double default_stride = 10;
  
  boolean unpack = false;
  
  String dateTimeStamp;
  
  public GEOSDataSource(File file) {
     this(file, 10);
  }
  
  public GEOSDataSource(File file, double default_stride) {
     this(file, default_stride, false);
  }

  public GEOSDataSource(File file, double default_stride, boolean unpack) {
    
    this.default_stride = default_stride;
    this.dateTimeStamp = DataSource.getDateTimeStampFromFilename(file.getName());
    this.unpack = unpack;
    
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
              String[] vrDimsName = reader.getDimensionNames(name);
              if (vrDimsName != null && vrDimsName.length > 0) {
                 String coordDimName = vrDimsName[0];
                 if (dimNames[k].equals(coordDimName)) {
                    varX = vr;
                    break;
                 }
              }
           }
           
           itr = projYCoordVars.keySet().iterator();
           while (itr.hasNext()) {
              Object key = itr.next();
              Variable vr = projYCoordVars.get(key);
              String name = vr.getShortName();
              String[] vrDimsName = reader.getDimensionNames(name);
              if (vrDimsName != null && vrDimsName.length > 0) {
                 String coordDimName = vrDimsName[0];
                 if (dimNames[k].equals(coordDimName)) {
                    varY = vr;
                    break;
                 }
              }   
           }
           
           itr = timeCoordVars.keySet().iterator();
           while (itr.hasNext()) {
              Object key = itr.next();
              Variable vr = timeCoordVars.get(key);
              String name = vr.getShortName();
              String[] vrDimsName = reader.getDimensionNames(name);
              if (vrDimsName != null && vrDimsName.length > 0) {
                 String coordDimName = vrDimsName[0];
                 if (dimNames[k].equals(coordDimName)) {
                    varT = vr;
                    break;
                 }
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
            if (unpack) {
               metadata.put("unpack", "true");
            }

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
      
      double scale_x=Double.NaN;
      double offset_x=Double.NaN;
      double scale_y=Double.NaN;
      double offset_y=Double.NaN;
      
      HDFArray obj = (HDFArray) reader.getArrayAttribute(xVarName, "scale_factor");
      if (obj != null) {
         if (obj.getType().equals(Double.TYPE)) {
            scale_x = ((double[]) obj.getArray())[0];
         }
         else if (obj.getType().equals(Float.TYPE)) {
            scale_x = ((float[]) obj.getArray())[0];
         }
      }
      obj = (HDFArray) reader.getArrayAttribute(xVarName, "add_offset");
      if (obj != null) {
         if (obj.getType().equals(Double.TYPE)) {
            offset_x = ((double[]) obj.getArray())[0];
         }
         else if (obj.getType().equals(Float.TYPE)) {
            offset_x = ((float[]) obj.getArray())[0];
         }         
      }

      obj = (HDFArray) reader.getArrayAttribute(yVarName, "scale_factor");
      if (obj != null) {
         if (obj.getType().equals(Double.TYPE)) {
            scale_y = ((double[]) obj.getArray())[0];
         }
         else if (obj.getType().equals(Float.TYPE)) {
            scale_y = ((float[]) obj.getArray())[0];
         }         
      }
      obj = (HDFArray) reader.getArrayAttribute(yVarName, "add_offset");
      if (obj != null) {
         if (obj.getType().equals(Double.TYPE)) {
            offset_y = ((double[]) obj.getArray())[0];
         }
         else if (obj.getType().equals(Float.TYPE)) {
            offset_y = ((float[]) obj.getArray())[0];
         }                  
      }
      
      if (Double.isNaN(scale_x) || Double.isNaN(offset_x) || Double.isNaN(scale_y) || Double.isNaN(offset_y)) {
         throw new Exception("problem retrieving navigation scale/offset");
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

      // ABI, AHI scan North to South so invert so that FGF (1,1) goes from SW -> NW in display space 
      // because defaultMapArea can only understand (1,1) at display coordinates (-1,-1). This assumes
      // intermediate coordinate system (radians, radians) maps increasing SW to NE.
      double inverty = 1.0; // already inverted
      if (scale_y < 0 && offset_y > 0) {
         inverty = -1.0;
      }

      if (sweepAngleAxis.equals("x")) {
         sweepAngleAxis = "GOES";
      }
      else if (sweepAngleAxis.equals("y")) {
         sweepAngleAxis = "GEOS";
      }

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