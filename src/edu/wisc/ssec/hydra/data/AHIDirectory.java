package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.hydra.Hydra;
import java.io.File;
import java.util.List;
import visad.VisADException;
import visad.Data;
import visad.FlatField;
import java.rmi.RemoteException;
import ucar.unidata.util.ColorTable;


public class AHIDirectory extends GEOSDirectory {

   public AHIDirectory(File directory) {
     this(directory.listFiles());
   }

   public AHIDirectory(File[] files) {
      super(files);
      
      nadirResolution = new float[] {
           1000f, 1000f,  500f, 1000f,
           2000f, 2000f, 2000f, 2000f,
           2000f, 2000f, 2000f, 2000f,
           2000f, 2000f, 2000f, 2000f
      };
      
      bandNames =  new String[] {
           "B01", "B02", "B03", "B04", "B05",
           "B06", "B07", "B08", "B09", "B10",
           "B11", "B12", "B13", "B14", "B15", "B16"
      };  

      centerWavelength = new float[] {
           0.47f, 0.51f, 0.64f, 0.86f, 1.6f, 2.3f, 3.9f, 6.2f, 6.9f, 7.3f,
           8.6f, 9.6f, 10.4f, 11.2f, 12.4f, 13.3f
      }; 
      
      category = new DataGroup[] {
         cat1KM, cat1KM, catHKM, cat1KM, 
         cat2KM, cat2KM, cat2KM, cat2KM,
         cat2KM, cat2KM, cat2KM, cat2KM,
         cat2KM, cat2KM, cat2KM, cat2KM
      };
   
      default_stride = new int[] {
         20, 20, 40, 20,
         10, 10, 10, 10,
         10, 10, 10, 10,
         10, 10, 10, 10      
      };
      
      targetList.add("albedo");
      targetList.add("brightness_temp");      
      
      init(files);      
   }


   @Override
   public String getDescription() {
     return "H08 AHI";
   }
   
   boolean fileBelongsToThis(String filename) {
      if (filename.startsWith("HS_H08") && filename.endsWith(".nc")) {
         return true;
      }
      return false;
   }   

   
   @Override
   public ColorTable getDefaultColorTable(DataChoice choice) {
      ColorTable clrTbl = Hydra.grayTable;
      String name = choice.getName();
      if ( name.equals("B07") || name.equals("B08") || name.equals("B09") || name.equals("B10") || name.equals("B11") || name.equals("B12") || name.equals("B13") || name.equals("B14") || name.equals("B15") || name.equals("B16") ) {
        clrTbl = Hydra.invGrayTable;
      }
      
      return clrTbl;
   }
   
   @Override
   Data postProcess(DataChoice choice, Data data) throws VisADException, RemoteException {
       if (choice.getName().equals("brightness_temp")) {      
          float[][] rngVals = ((FlatField)data).getFloats(false);
          for (int k=0; k<rngVals[0].length; k++) {
             float fval = rngVals[0][k];
             if (fval < 150f || fval > 340f) {
                rngVals[0][k] = Float.NaN;
             }
          }
       }
       return data;
   }

}
