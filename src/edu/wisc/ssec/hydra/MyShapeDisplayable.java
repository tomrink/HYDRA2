package edu.wisc.ssec.hydra;

import ucar.visad.display.ShapeDisplayable;
import visad.DataRenderer;
import visad.VisADException;
import visad.bom.ScreenLockedRendererJ3D;
import visad.VisADGeometryArray;

import java.rmi.RemoteException;

public class MyShapeDisplayable extends ShapeDisplayable {
   VisADGeometryArray array = null;

   public MyShapeDisplayable(String name, VisADGeometryArray array) throws VisADException, RemoteException {
      super(name, array);
      this.array = array;
   }

   protected DataRenderer getDataRenderer() throws VisADException {
      return new ScreenLockedRendererJ3D();
   }

}
