package edu.wisc.ssec.adapter;

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class AVHR_ncdf_L1_RangeProcessor extends RangeProcessor {

        String channelName;
        float irradiance = Float.NaN;
        boolean reflective = true;
        float scale;

        double C1 = 1.191062E-5;  // (mW/(m2.sr.cm-4))
        double C2 = 1.4387863;  // (K/cm-1)

        float wnc = Float.NaN;
        float wnc3 = Float.NaN;
        float alpha = Float.NaN;
        float beta = Float.NaN;

        public AVHR_ncdf_L1_RangeProcessor(MultiDimensionReader reader, HashMap metadata, String channelName) throws Exception {
           super(reader, metadata);
           this.channelName = channelName;
           
           if (channelName.equals("CH1") || channelName.equals("CH2") || channelName.equals("CH3A")) {
              reflective = true;
              String fsolName = null;

              if (channelName.equals("CH1")) {
                 scale = 100;
                 fsolName = "channel_1_f_sol";
              }
              else if (channelName.equals("CH2")) {
                 scale = 100;
                 fsolName = "channel_2_f_sol";
              }
              else if (channelName.equals("CH3A")) {
                 scale = 10000;
                 fsolName = "channel_3a_f_sol";
              }

              irradiance = ((float[]) reader.getGlobalAttribute(fsolName).getArray())[0];
           }
           else if (channelName.equals("CH3B") || channelName.equals("CH4") || channelName.equals("CH5")) {
              reflective = false;
              String wncName = null;
              String alphaName = null;
              String betaName = null;

              if (channelName.equals("CH4")) {
                 wncName = "channel_4_wnc";
                 alphaName = "channel_4_alpha";
                 betaName = "channel_4_beta";
                 scale = 100f;
              }
              else if (channelName.equals("CH5")) {
                 wncName = "channel_5_wnc";
                 alphaName = "channel_5_alpha";
                 betaName = "channel_5_beta";
                 scale = 100f;
              }
              else if (channelName.equals("CH3B")) {
                 wncName = "channel_3b_wnc";
                 alphaName = "channel_3b_alpha";
                 betaName = "channel_3b_beta";
                 scale = 10000f;
              }
              
              wnc = ((float[]) reader.getGlobalAttribute(wncName).getArray())[0];
              wnc3 = wnc*wnc*wnc;
              
              alpha = ((float[]) reader.getGlobalAttribute(alphaName).getArray())[0];
              beta = ((float[]) reader.getGlobalAttribute(betaName).getArray())[0];
           }
        }

        public float[] processRange(short[] values, HashMap subset) {
           float[] fltValues = super.processRange(values, subset);

           if (reflective) {
              float PI = (float) java.lang.Math.PI;
              for (int k=0; k<values.length; k++) {
                 float Rad = fltValues[k];
                 fltValues[k] = (Rad*PI)/irradiance;
                 if (fltValues[k] < 0f) {
                    fltValues[k] = Float.NaN;
                 }
              }
           }
           else { // Emmissive
              for (int k=0; k<values.length; k++) {
                 float R = fltValues[k];
                 float BT = (float) ( C2*wnc/(java.lang.Math.log(1.0 + (C1*wnc3)/R)) );

                 fltValues[k] = (BT - beta)/alpha;
              }
           }

           return fltValues;
        }
}
