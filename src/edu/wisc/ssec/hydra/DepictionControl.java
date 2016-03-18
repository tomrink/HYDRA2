package edu.wisc.ssec.hydra;

import java.awt.Container;

public interface DepictionControl {

   public abstract Container doMakeContents();

   public abstract void destroy();

   public abstract void reset();
   
   public abstract double[][] getDataRange();
   
   public abstract void setDepiction(Depiction depict);
   
   public abstract Depiction getDepiction();

}
