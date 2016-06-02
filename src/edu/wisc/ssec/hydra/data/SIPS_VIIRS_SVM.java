package edu.wisc.ssec.hydra.data;

import java.io.File;
import edu.wisc.ssec.adapter.SwathAdapter;


public class SIPS_VIIRS_SVM extends SIPS_VIIRS_DataSource {
   

   public SIPS_VIIRS_SVM(File directory) {
     this(directory.listFiles());
   }

   public SIPS_VIIRS_SVM(File[] files) {
      super(files);
   }

   void init() throws Exception {
      
      String groupName = "observation_data/";
      String geoGroupName = "geolocation_data/";
      String[] emisBandNames = new String[] {"M12", "M13", "M14", "M15", "M16"};
      String[] reflBandNames = new String[] {"M01", "M02", "M03", "M04", "M05", "M06", "M07", "M08", "M09", "M10", "M11"};
      
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
         String radLUTname = groupName+emisBandNames[k]+"_radiance_lut";
         String btLUTname = groupName+emisBandNames[k]+"_brightness_temperature_lut";
        
         SwathAdapter adapter = buildEmisAdapter("number_of_pixels", "number_of_lines", arrayName, rangeName,
                 "number_of_pixels", "number_of_lines", geoGroupName+"longitude", geoGroupName+"latitude",
                 null, null, null, "_FillValue", btLUTname);
         
         setDataChoice(adapter, nAdapters, rangeName);
         nAdapters++;
      }
      
      bandNames = new String[] {
         "M01", "M02", "M03", "M04", "M05",
         "M06", "M07", "M08", "M09", "M10",
         "M11", "M12", "M13", "M14", "M15",
         "M16"
      };
      
      centerWavelength = new float[] {
         0.412f, 0.445f, 0.488f, 0.555f, 0.672f,
         0.746f, 0.865f, 1.240f, 1.378f, 1.61f, 2.250f, 3.700f, 4.050f, 8.550f, 10.763f, 12.013f
      };
   
   }
   
   public float getNadirResolution(DataChoice choice) {
      return 770f;
   }

}