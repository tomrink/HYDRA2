package edu.wisc.ssec.hydra;

import java.net.URL;
import java.io.*;
import java.util.StringTokenizer;


public class WorldDataBank extends SimpleBoundaryAdapter {
    
    BufferedReader reader = null;
    
    public WorldDataBank(URL url) throws Exception {
        super(url);
    }
    
    protected void openSource() throws IOException {
        Object obj = new Object();
        InputStream ios = url.openStream();
        reader = new BufferedReader(new InputStreamReader(ios));
    }
    
    protected void readHeader() throws EOFException, IOException {
        String line = reader.readLine();
        if (line == null) {
            throw new EOFException();
        }
        StringTokenizer strTok = new StringTokenizer(line);
        String[] tokens = new String[strTok.countTokens()];
        int tokCnt = 0;
        while (strTok.hasMoreElements()) {
           tokens[tokCnt++] = strTok.nextToken();
        }
        
        numPtsPolygon = Integer.valueOf(tokens[5]);
        
        return;
    }
    
    protected void readPolygonPoints() throws EOFException, IOException {
        float[][] latlon = new float[2][numPtsPolygon];
        
        for (int t=0; t<numPtsPolygon; t++) {
            String line = reader.readLine();
            StringTokenizer strTok = new StringTokenizer(line);
            String[] tokens = new String[strTok.countTokens()];
            int tokCnt = 0;
            while (strTok.hasMoreElements()) {
               tokens[tokCnt++] = strTok.nextToken();
            }
            latlon[0][t] = Float.valueOf(tokens[0]);
            latlon[1][t] = Float.valueOf(tokens[1]);
        }
        
        polygons.add(latlon);
        
        return;
    }
    
    protected boolean skip() {
        return false;
    }
    
    protected void cleanup() throws IOException {
        reader.close();
    }
    
}
