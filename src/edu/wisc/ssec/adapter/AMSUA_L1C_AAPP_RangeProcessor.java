package edu.wisc.ssec.adapter;

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class AMSUA_L1C_AAPP_RangeProcessor extends RangeProcessor {

        String channelName;
        float irradiance = Float.NaN;
        boolean reflective = true;
        float scale;

        double C1 = 1.191044E-5;  // (mW/(m2.sr.cm-4))
        double C2 = 1.4387869;  // (K/cm-1)

        float alpha = 1f;
        float beta = 0f;
        float wnc = Float.NaN;
        float wnc3 = Float.NaN;
        
        float[] offsets;
        float[] slopes;

        public AMSUA_L1C_AAPP_RangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
              super(reader, metadata);
        }

        public float[] processRange(int[] values, HashMap subset) {
            
           float[] fltValues = new float[values.length];

            for (int k=0; k<values.length; k++) {
               
               if (values[k] >= 0 ) {
                  fltValues[k] = values[k]*0.01f;
               }
               else {
                  fltValues[k] = Float.NaN;
               }
           }
           return fltValues;
        }
}
