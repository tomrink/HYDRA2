package edu.wisc.ssec.adapter;

import java.util.HashMap;
import java.util.ArrayList;
import visad.util.Util;

public class MERSI_L1B_Emis_RangeProcessor extends RangeProcessor {

        double C1 = 1.1910659/100000;
        double C2 = 1.438833;

        double B = 1.0103;
        double A = -1.8521;

        double freq = 875.1379;
        double freq3 = freq*freq*freq;

        public MERSI_L1B_Emis_RangeProcessor() throws Exception {
        }

        public float[] processRange(short[] values, HashMap subset) {
           float[] fltValues = new float[values.length];

           for (int k=0; k<values.length; k++) {
              double rad = values[k]/100.0;

              double BT = C2*freq/Math.log((C1*freq3/rad) + 1);

              BT = BT*B + A;

              fltValues[k] = (float) BT;
           }

           return fltValues;
        }
}
