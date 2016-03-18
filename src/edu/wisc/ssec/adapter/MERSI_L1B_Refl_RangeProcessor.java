package edu.wisc.ssec.adapter;

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class MERSI_L1B_Refl_RangeProcessor extends RangeProcessor {

        int offset;

        float[][] coeffs = new float[3][19];

        public MERSI_L1B_Refl_RangeProcessor(MultiDimensionReader reader, int offset, String productName) throws Exception {
           this.offset = offset;

           if (productName.equals("FY3C_MERSI")) {
           float[] fltArray = reader.getFloatArray("Calibration/VIS_Cal_Coeff", new int[] {0,0}, new int[] {19,3}, new int[] {1,1});
           for (int k=0; k<19; k++) {
              int i = k*3;
              coeffs[0][k] = fltArray[i];
              coeffs[1][k] = fltArray[i+1];
              coeffs[2][k] = fltArray[i+2];
           }
           }


        }

        public MERSI_L1B_Refl_RangeProcessor(MultiDimensionReader reader, int offset) throws Exception {
           this.offset = offset;

           double[] dblArray = (double[]) reader.getGlobalAttribute("VIR_Cal_Coeff").getArray();
           for (int k=0; k<19; k++) {
              int i = k*3;
              coeffs[0][k] = (float) dblArray[i];
              coeffs[1][k] = (float) dblArray[i+1];
              coeffs[2][k] = (float) dblArray[i+2];
           }
        }

        public float[] processRange(short[] values, HashMap subset) {
           float[] fltValues = new float[values.length];
 
           int channelIndex = 0;
           double[] dblA = (double[]) subset.get(SpectrumAdapter.channelIndex_name);
           if (dblA != null) {
              channelIndex = (int) dblA[0];
           }

           float k0 = coeffs[0][offset+channelIndex];
           float k1 = coeffs[1][offset+channelIndex];
           float k2 = coeffs[2][offset+channelIndex];

           for (int k=0; k<values.length; k++) {
              short val = values[k];

              float refl = k0 + k1*val + k2*val*val;

              fltValues[k] = 0.01f*refl;
           }

           return fltValues;
        }
}
