package edu.wisc.ssec.adapter;

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class HIRS_L1_NCDF_RangeProcessor extends RangeProcessor {

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

        public HIRS_L1_NCDF_RangeProcessor(MultiDimensionReader reader, HashMap metadata, String channelName) throws Exception {
              super(reader, metadata);
              this.channelName = channelName;
              int idx = 0;

              float aScale = 1;
              float bScale = 1;

              if (channelName.equals("1")) {
                 idx = 0;
              }
              else if (channelName.equals("2")) {
                 idx = 1;
              }
              else if (channelName.equals("3")) {
                 idx = 2;
              }
              else if (channelName.equals("4")) {
                 idx = 3;
              }
              else if (channelName.equals("5")) {
                 idx = 4;
              }
              else if (channelName.equals("6")) {
                 idx = 5;
              }
              else if (channelName.equals("7")) {
                 idx = 6;
              }
              else if (channelName.equals("8")) {
                 idx = 7;
              }
              else if (channelName.equals("9")) {
                 idx = 8;
              }
              else if (channelName.equals("10")) {
                 idx = 9;
              }
              else if (channelName.equals("11")) {
                 idx = 10;
              }
              else if (channelName.equals("12")) {
                 idx = 11;
              }
              else if (channelName.equals("13")) {
                 idx = 12;
              }
              else if (channelName.equals("14")) {
                 idx = 13;
              }
              else if (channelName.equals("15")) {
                 idx = 14;
              }
              else if (channelName.equals("16")) {
                 idx = 15;
              }
              else if (channelName.equals("17")) {
                 idx = 16;
              }
              else if (channelName.equals("18")) {
                 idx = 17;
              }
              else if (channelName.equals("19")) {
                 idx = 18;
              }


              int[] intVal =
                 reader.getIntArray("wnc", new int[] {idx}, new int[] {1}, new int[] {1});

              wnc = intVal[0]*1E-5f;
              wnc3 = wnc*wnc*wnc;

              intVal =
                 reader.getIntArray("alpha", new int[] {idx}, new int[] {1}, new int[] {1});
              alpha = intVal[0]/1E6f;

              intVal =
                 reader.getIntArray("beta", new int[] {idx}, new int[] {1}, new int[] {1});
              beta = intVal[0]/1E6f;
        }

        public float[] processRange(int[] values, HashMap subset) {
           float[] fltValues = super.processRange(values, subset);

            for (int k=0; k<values.length; k++) {
               float R = fltValues[k];
               float BT = (float) (C2*wnc/(java.lang.Math.log(1.0 + ((C1*wnc3)/R))));

               fltValues[k] = (BT-beta)/alpha;
           }

           return fltValues;
        }
        
        public float getWaveNumber() {
           return wnc;
        }
}
