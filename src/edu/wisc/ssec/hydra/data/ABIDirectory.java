package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.hydra.Hydra;
import java.io.File;
import java.util.List;
import visad.VisADException;
import visad.Data;
import visad.FlatField;
import java.rmi.RemoteException;
import ucar.unidata.util.ColorTable;


public class ABIDirectory extends GEOSDirectory {

   public ABIDirectory(File directory) {
     this(directory.listFiles());
   }

   public ABIDirectory(File[] files) {
      super(files);
      
      nadirResolution = new float[] {
           1000f,  500f, 1000f, 1000f,
           2000f, 2000f, 2000f, 2000f,
           2000f, 2000f, 2000f, 2000f,
           2000f, 2000f, 2000f, 2000f
      };
      
      bandNames =  new String[] {
           "C01", "C02", "C03", "C04", "C05",
           "C06", "C07", "C08", "C09", "C10",
           "C11", "C12", "C13", "C14", "C15", "C16"
      };        

      centerWavelength = new float[] {
           0.47f, 0.64f, 0.86f, 1.37f, 1.61f, 2.25f, 3.9f, 6.19f, 6.95f, 7.34f,
           8.5f, 9.61f, 10.35f, 11.2f, 12.3f, 13.3f
      }; 
      
      category = new DataGroup[] {
         cat1KM, catHKM, cat1KM, cat1KM,
         cat2KM, cat2KM, cat2KM, cat2KM,
         cat2KM, cat2KM, cat2KM, cat2KM,
         cat2KM, cat2KM, cat2KM, cat2KM
      };
   
      default_stride = new int[] {
         20, 40, 20, 20,
         10, 10, 10, 10,
         10, 10, 10, 10,
         10, 10, 10, 10      
      };
      
      targetList.add("CMI");
      
      unpack = true;
      
      init(files);      
   }


   @Override
   public String getDescription() {
     return "GOES-16 ABI";
   }
   
   boolean fileBelongsToThis(String filename) {
      if (filename.contains("ABI-L2-CMIP") && filename.endsWith(".nc")) {
         return true;
      }
      return false;
   }   

   
   @Override
   public ColorTable getDefaultColorTable(DataChoice choice) {
      ColorTable clrTbl = Hydra.grayTable;
      String name = choice.getName();
      if ( name.equals("C07") || name.equals("C08") || name.equals("C09") || name.equals("C10") || name.equals("C11") || name.equals("C12") || name.equals("C13") || name.equals("C14") || name.equals("C15") || name.equals("C16") ) {
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
