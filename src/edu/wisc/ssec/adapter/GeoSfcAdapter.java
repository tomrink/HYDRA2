package edu.wisc.ssec.adapter;

import java.util.HashMap;
import visad.Linear2DSet;
import visad.Set;


public abstract class GeoSfcAdapter extends MultiDimensionAdapter {
   
   public GeoSfcAdapter(MultiDimensionReader reader, HashMap metadata) {
      super(reader, metadata);
   }

   @Override
   public HashMap getDefaultSubset() {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }

   @Override
   public Set makeDomain(Object subset) throws Exception {
      throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   }
   
   public abstract Navigation getNavigation();
   
   public abstract void setDomainSet(Linear2DSet dset);
   
   public abstract Set getDatasetDomain();
   
}
