package edu.wisc.ssec.hydra.data;

import java.io.File;
import edu.wisc.ssec.adapter.SwathAdapter;


public class SIPS_VIIRS_DNB extends SIPS_VIIRS_DataSource {
   

   public SIPS_VIIRS_DNB(File directory) {
     this(directory.listFiles());
   }

   public SIPS_VIIRS_DNB(File[] files) {
      super(files);
   }

   void init() throws Exception {
      
      String groupName = "observation_data/";
      String geoGroupName = "geolocation_data/";
      String[] arrayNames = new String[] {"DNB_observations"};
      String[] rangeNames = new String[] {"DNB"};
      
      int nAdapters = 0;
      
      for (int k=0; k<arrayNames.length; k++) {
         String arrayName = groupName+arrayNames[k];
         String rangeName = rangeNames[k];
         
         SwathAdapter adapter = buildAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                 "number_of_pixels", "number_of_lines", geoGroupName+"longitude", geoGroupName+"latitude", null, null, null, "_FillValue");
         
         setDataChoice(adapter, nAdapters, rangeName);
         nAdapters++;
      }
 
      
      bandNames = new String[] {
         "DNB"
      };
      
      centerWavelength = new float[] {
         0.70f
      };
   
   }
   
   public float getNadirResolution() {
      return 770f;
   }

}