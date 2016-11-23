package edu.wisc.ssec.adapter;

import visad.CoordinateSystem;
import visad.Linear2DSet;

public interface Navigation {

  public CoordinateSystem getVisADCoordinateSystem(Linear2DSet domain, Object subset) throws Exception;
  
  public double[] getEarthLocOfDataCoord(int[] coord) throws Exception ;

}


