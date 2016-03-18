package edu.wisc.ssec.adapter;

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class HIRS_L1C_RangeProcessor extends RangeProcessor {

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

        public HIRS_L1C_RangeProcessor(MultiDimensionReader reader, HashMap metadata) throws Exception {
              super(reader, metadata);

           offsets = (float[]) reader.getGlobalAttribute("HIRS_temprad_offset").getArray();
           slopes = (float[]) reader.getGlobalAttribute("HIRS_temprad_slope").getArray();
        }

        public float[] processRange(int[] values, HashMap subset) {
           //float[] fltValues = super.processRange(values, subset);
            
           float[] fltValues = new float[values.length];
           double[] coords = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
           boolean multiChan = (coords[1] - coords[0] + 1.0) > 1;
           int channelIndex = (int) coords[0];
           
           float beta = offsets[channelIndex];
           float alpha = slopes[channelIndex];

            for (int k=0; k<values.length; k++) {

               if (multiChan) {
                   beta = offsets[k];
                   alpha = slopes[k];
               }
               
               if (values[k] >= 0 ) {
                  fltValues[k] = (values[k]*0.01f - beta)/alpha;
               }
               else {
                  fltValues[k] = Float.NaN;
               }
           }

           return fltValues;
        }
}
