package edu.wisc.ssec.adapter;

import visad.*;

import java.util.StringTokenizer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ATMS_SDR_Utility {
    
    static float[] ch7limbCorrect;
    static float[] ch8limbCorrect;
    static float[] ch9limbCorrect;
    static float[] ch10limbCorrect;
    
    static String chan7name = "/edu/wisc/ssec/adapter/resources/atms/limbcoef7.txt";
    static String chan8name = "/edu/wisc/ssec/adapter/resources/atms/limbcoef8.txt";
    static String chan9name = "/edu/wisc/ssec/adapter/resources/atms/limbcoef9.txt";
    static String chan10name = "/edu/wisc/ssec/adapter/resources/atms/limbcoef10.txt";

    public static float[] applyLimbCorrection(float[] values, float chan, int fovStart, int fovStop) {
        
        if (ch7limbCorrect == null) {
            try {
               ch7limbCorrect = readValues(chan7name);
               ch8limbCorrect = readValues(chan8name);
               ch9limbCorrect = readValues(chan9name);
               ch10limbCorrect = readValues(chan10name);
            }
            catch (IOException ioex) {
                ioex.printStackTrace();
            }
        }
        
        float[] limbCorrect = null;
        
        if (chan == 7.0) {
            limbCorrect = ch7limbCorrect;
        }
        else if (chan == 8.0) {
            limbCorrect = ch8limbCorrect;
        }
        else if (chan == 9.0) {
            limbCorrect = ch9limbCorrect;
        }
        else if (chan == 10.0) {
            limbCorrect = ch10limbCorrect;
        }
        else {
            return values;
        }
        
        float[] new_values = new float[values.length];
        int nFovs = (fovStop-fovStart) + 1;
        int nscans = values.length/nFovs;
        
        for (int j=0; j<nscans; j++) {
            for (int i=0; i<nFovs; i++) {
                int k = j*nFovs + i;
                new_values[k] = values[k] - limbCorrect[i+fovStart];
            }
        }
        
        return new_values;
    }
    
    static float[] readValues(String filename) throws IOException {
        Object obj = new Object();
        InputStream ios = obj.getClass().getResourceAsStream(filename);
        BufferedReader ancillaryReader = new BufferedReader(new InputStreamReader(ios));

        float[] values = new float[96];
        int cnt = 0;
        while (true) {
          String line = ancillaryReader.readLine();
          if (line == null) break;
          if (line.startsWith("!")) continue;
          StringTokenizer strTok = new StringTokenizer(line);
          String[] tokens = new String[strTok.countTokens()];
          int tokCnt = 0;
          while (strTok.hasMoreElements()) {
            tokens[tokCnt++] = strTok.nextToken();
          }
          values[cnt] = Float.valueOf(tokens[0]);
          cnt++;
        }
        ios.close();
        return values;
    }
}
