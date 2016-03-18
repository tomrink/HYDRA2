package edu.wisc.ssec.adapter;

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class MHS_xxx_L1_RangeProcessor extends RangeProcessor {

        String channelName;
        float irradiance = Float.NaN;
        boolean reflective = true;
        float scale;

        double C1 = 1.191062E-5;  // (mW/(m2.sr.cm-4))
        double C2 = 1.4387863;  // (K/cm-1)

        float A = Float.NaN;
        float B = Float.NaN;
        float gamma = Float.NaN;
        float gamma3 = Float.NaN;

        public MHS_xxx_L1_RangeProcessor(MultiDimensionReader reader, String channelName) throws Exception {
           this.channelName = channelName;
              int idxGamma = 0;
              int idxA = 0;
              int idxB = 0;

              float gScale = 1;
              float aScale = 1;
              float bScale = 1;

              if (channelName.equals("CH1")) {
                 scale = 1E07f;
                 idxGamma = 55;
                 idxA = 56;
                 idxB = 57;
                 gScale = 1E06f;
                 aScale = 1E06f;
                 bScale = 1E06f;
              }
              else if (channelName.equals("CH2")) {
                 scale = 1E07f;
                 idxGamma = 58;
                 idxA = 59;
                 idxB = 60;
                 gScale = 1E06f;
                 aScale = 1E06f;
                 bScale = 1E06f;
              }
              else if (channelName.equals("CH3")) {
                 scale = 1E07f;
                 idxGamma = 61;
                 idxA = 62;
                 idxB = 63;
                 gScale = 1E06f;
                 aScale = 1E06f;
                 bScale = 1E06f;
              }
              else if (channelName.equals("CH4")) {
                 scale = 1E07f;
                 idxGamma = 64;
                 idxA = 65;
                 idxB = 66;
                 gScale = 1E06f;
                 aScale = 1E06f;
                 bScale = 1E06f;
              }
              else if (channelName.equals("CH5")) {
                 scale = 1E07f;
                 idxGamma = 67;
                 idxA = 68;
                 idxB = 69;
                 gScale = 1E06f;
                 aScale = 1E06f;
                 bScale = 1E06f;
              }

              int[] intVal =
                 reader.getIntArray("U-MARF/EPS/MHSx_xxx_1B/METADATA/GIADR/GIADR_RADIANCE_MHS_L1_ARRAY_000001",
                      new int[] {0, idxGamma}, new int[] {1,1}, new int[] {1,1});

              gamma = intVal[0]/gScale;
              gamma3 = gamma*gamma*gamma;

              intVal =
                 reader.getIntArray("U-MARF/EPS/MHSx_xxx_1B/METADATA/GIADR/GIADR_RADIANCE_MHS_L1_ARRAY_000001",
                      new int[] {0, idxA}, new int[] {1,1}, new int[] {1,1});

              A = intVal[0]/aScale;

              intVal =
                 reader.getIntArray("U-MARF/EPS/MHSx_xxx_1B/METADATA/GIADR/GIADR_RADIANCE_MHS_L1_ARRAY_000001",
                      new int[] {0, idxB}, new int[] {1,1}, new int[] {1,1});

              B = intVal[0]/bScale;

        }

        public float[] processRange(int[] values, HashMap subset) {
           float[] fltValues = new float[values.length];

            for (int k=0; k<values.length; k++) {
               float R = values[k]/scale;
               float BT = (float) (C2*gamma/(java.lang.Math.log(1.0 + ((C1*gamma3)/R))));

               fltValues[k] = A + B*BT;
           }

           return fltValues;
        }
}
