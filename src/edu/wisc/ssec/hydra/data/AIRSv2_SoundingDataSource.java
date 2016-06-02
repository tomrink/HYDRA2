package edu.wisc.ssec.hydra.data;

import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;
import edu.wisc.ssec.hydra.data.DataChoice;
import java.io.File;
import java.util.HashMap;
import java.util.Hashtable;
import edu.wisc.ssec.adapter.SwathSoundingData;
import edu.wisc.ssec.adapter.NetCDFFile;
import edu.wisc.ssec.adapter.MultiDimensionSubset;


public class AIRSv2_SoundingDataSource extends AtmSoundingDataSource {


   public AIRSv2_SoundingDataSource(File directory) {
     this(directory.listFiles());
   }

   public AIRSv2_SoundingDataSource(File[] files) {
      super(files);
   }

   void init(File file) throws Exception {
      NetCDFFile reader = new NetCDFFile(file.getAbsolutePath());

      double[] dvals = {0.0050, 0.0161, 0.0384, 0.0769, 0.1370, 0.2244, 0.3454, 0.5064, 0.7140, 0.9753, 1.2972, 1.6872, 2.1526, 2.7009, 3.3398, 4.0770, 4.9204, 5.8776, 6.9567, 8.1655, 9.5119, 11.0038, 12.6492, 14.4559, 16.4318, 18.5847, 20.9224, 23.4526, 26.1829, 29.1210, 32.2744, 35.6505, 39.2566, 43.1001, 47.1882, 51.5278, 56.1260, 60.9895, 66.1253, 71.5398, 77.2396, 83.2310, 89.5204, 96.1138, 103.0172, 110.2366, 117.7775, 125.6456, 133.8462, 142.3848, 151.2664, 160.4959, 170.0784, 180.0183, 190.3203, 200.9887, 212.0277, 223.4415, 235.2338, 247.4085, 259.9691, 272.9191, 286.2617, 300.0000, 314.1369, 328.6753, 343.6176, 358.9665, 374.7241, 390.8926, 407.4738, 424.4698, 441.8819, 459.7118, 477.9607, 496.6298, 515.7200, 535.2322, 555.1669, 575.5248, 596.3062, 617.5112, 639.1398, 661.1920, 683.6673, 706.5654, 729.8857, 753.6275, 777.7897, 802.3714, 827.3713, 852.7880, 878.6201, 904.8659, 931.5236, 958.5911, 986.0666, 1013.9476, 1042.2319, 1070.9170, 1100.0000 };

      float[] vals = new float[101];
      for (int k=0; k<vals.length; k++) {
         vals[k] = (float) dvals[k];
      }
      
      SwathSoundingData dataTA = buildAdapter(reader, "XTrack", "Track", "Level", "Plevs", vals, "TAir", "Temperature", "GeoXTrack", "GeoTrack",
                                   "Longitude", "Latitude", new String[] {"Level", "Track", "XTrack"}, new String[] {"GeoTrack", "GeoXTrack"}, new String[] {"GeoTrack", "GeoXTrack"}, "missing_value");
      

      HashMap subset = dataTA.getDefaultSubset();
      DataSelection dataSel = new MultiDimensionSubset(subset);
      DataChoice dataChoice = new DataChoice(this, "Temp", null);
      dataChoice.setDataSelection(dataSel);
      myDataChoices.add(dataChoice);
      mySoundingDatas.add(dataTA);

      SwathSoundingData dataWV = buildAdapter(reader, "XTrack", "Track", "Level", "Plevs", vals, "H2OMMR", "WV", "GeoXTrack", "GeoTrack",
                                   "Longitude", "Latitude", new String[] {"Level", "Track", "XTrack"}, new String[] {"GeoTrack", "GeoXTrack"}, new String[] {"GeoTrack", "GeoXTrack"}, "missing_value");
      dataWV.setDataRange(new float[] {0, 20});
      subset = dataWV.getDefaultSubset();
      dataSel = new MultiDimensionSubset(subset);
      dataChoice = new DataChoice(this, "WV", null);
      dataChoice.setDataSelection(dataSel);
      myDataChoices.add(dataChoice);
      mySoundingDatas.add(dataWV);

      SwathSoundingData dataO3 = buildAdapter(reader, "XTrack", "Track", "Level", "Plevs", vals, "O3VMR", "O3", "GeoXTrack", "GeoTrack",
                                   "Longitude", "Latitude", new String[] {"Level", "Track", "XTrack"}, new String[] {"GeoTrack", "GeoXTrack"}, new String[] {"GeoTrack", "GeoXTrack"}, "missing_value");
      
      dataO3.setDataRange(new float[] {0, 20});
      subset = dataO3.getDefaultSubset();
      dataSel = new MultiDimensionSubset(subset);
      dataChoice = new DataChoice(this, "O3", null);
      dataChoice.setDataSelection(dataSel);
      myDataChoices.add(dataChoice);
      mySoundingDatas.add(dataO3);
   }

}
