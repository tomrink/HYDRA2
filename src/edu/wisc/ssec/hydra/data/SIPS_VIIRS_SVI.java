package edu.wisc.ssec.hydra.data;

import java.io.File;
import edu.wisc.ssec.adapter.SwathAdapter;


public class SIPS_VIIRS_SVI extends SIPS_VIIRS_DataSource {
   

   public SIPS_VIIRS_SVI(File directory) {
     this(directory.listFiles());
   }

   public SIPS_VIIRS_SVI(File[] files) {
      super(files);
   }

   void init() throws Exception {
      
      String groupName = "observation_data/";
      String geoGroupName = "geolocation_data/";
      String[] emisBandNames = new String[] {"I04", "I05"};
      String[] reflBandNames = new String[] {"I01", "I02", "I03"};
      
      int nAdapters = 0;
      
      for (int k=0; k<reflBandNames.length; k++) {
         String arrayName = groupName+reflBandNames[k];
         String rangeName = reflBandNames[k];
         
         SwathAdapter adapter = buildReflAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                 "number_of_pixels", "number_of_lines", geoGroupName+"longitude", geoGroupName+"latitude", null, null, null, "_FillValue");
         
         setDataChoice(adapter, nAdapters, rangeName);
         nAdapters++;
      }
      
      for (int k=0; k<emisBandNames.length; k++) {
         String arrayName = groupName+emisBandNames[k];
         String rangeName = emisBandNames[k];
         String btLUTname = groupName+emisBandNames[k]+"_brightness_temperature_lut";
        
         SwathAdapter adapter = buildEmisAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                 "number_of_pixels", "number_of_lines", geoGroupName+"longitude", geoGroupName+"latitude",
                 null, null, null, "_FillValue", btLUTname);
         
         setDataChoice(adapter, nAdapters, rangeName);
         nAdapters++;
      }
      
      bandNames = new String[] {
         "I01", "I02", "I03", "I04", "I05"
      };
      
      centerWavelength = new float[] {
         0.640f, 0.856f, 1.610f, 3.740f, 11.450f
      };
   }
   
   public float getNadirResolution() {
      return 380f;
   }

}