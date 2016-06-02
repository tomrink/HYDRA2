package edu.wisc.ssec.hydra;

import java.io.*;
import java.net.URL;


public class GSHHG extends SimpleBoundaryAdapter {

   final int numHdrRecords = 11;
   int[] header;

   int polygonID;
   int level;
   float west;
   float east;
   float south;
   float north;
   float area;

   DataInputStream dis;

   public GSHHG(URL url) throws Exception {
      super(url);
   }
   
   protected void init() {
       header = new int[numHdrRecords];
   }
   
   protected void openSource() throws IOException {
      dis = new DataInputStream(
                       new BufferedInputStream(
                               url.openStream()));
   }
   
   public void cleanup() throws IOException {
      dis.close();
   }

  protected void readHeader() throws EOFException, IOException {
     /* read header */
     for (int t=0; t<numHdrRecords; t++) {
        header[t] = dis.readInt();
     }
     polygonID = header[0];
     numPtsPolygon = header[1];

     int flag = header[2];
     level = (flag & 255);
     //System.out.println((flag & 255));
     //System.out.println(((flag >> 8) & 255));
     //System.out.println(((flag >> 16) & 1));
     //System.out.println(((flag >> 24) & 1));

     west = header[3]/(float)1E6;
     east = header[4]/(float)1E6;
     south = header[5]/(float)1E6;
     north = header[6]/(float)1E6;

     int m = flag >> 26; // dynamic scale
     float scale = (float) Math.pow(10.0, (double)m);
     area = header[7]/scale;

     return;
  }

  protected void readPolygonPoints() throws EOFException, IOException {
     float[][] latlon = new float[2][numPtsPolygon];

     for (int t=0; t<numPtsPolygon; t++) {
        float lon = dis.readInt()/(float)1E6;
        float lat = dis.readInt()/(float)1E6;

        latlon[0][t] = lat;
        if (lon > 180f) lon -= 360f;
        latlon[1][t] = lon;
     }
     polygons.add(latlon);

     return;
  }

  protected boolean skip() throws EOFException, IOException {
     if ( ((level == 1) && (area < 110f)) ||
          ((level == 2) && (area < 1400f)) ||
          (level > 2) ) 
     {
        dis.skipBytes(4*(numPtsPolygon*2));
        return true;
     }
     return false;
  }

}
