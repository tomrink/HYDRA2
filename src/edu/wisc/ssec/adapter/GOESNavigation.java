package edu.wisc.ssec.adapter;

import edu.wisc.ssec.hydra.GEOSTransform;
import visad.CoordinateSystem;
import visad.Linear2DSet;

/**
 *
 * @author rink
 */
public class GOESNavigation implements Navigation {
   
   GEOSTransform geosTrans;
   double scale_x;
   double offset_x;
   double scale_y;
   double offset_y;
   
   
   public GOESNavigation(GEOSTransform geosTrans, double scale_x, double offset_x, double scale_y, double offset_y) {
      this.geosTrans = geosTrans;
      this.scale_x = scale_x;
      this.offset_x = offset_x;
      this.scale_y = scale_y;
      this.offset_y = offset_y;
   }

   @Override
   public CoordinateSystem getVisADCoordinateSystem(Linear2DSet domain, Object subset) throws Exception {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public double[] getEarthLocOfDataCoord(int[] coord) throws Exception {
      return geosTrans.elemLineToEarth(coord[0], coord[1], scale_x, offset_x, scale_y, offset_y);
   }
   
}
