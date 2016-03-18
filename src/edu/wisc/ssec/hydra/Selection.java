package edu.wisc.ssec.hydra;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;

import java.util.ArrayList;

public interface Selection {

   public abstract void applyToDataSelection(DataSelection select);

   public abstract int applyToDataSelection(DataChoice choice, DataSelection select);

   public abstract DataChoice getSelectedDataChoice();

   public abstract ArrayList<SelectionListener> getSelectionListeners();

   public abstract void removeSelectionListener(SelectionListener listener);
 
   public abstract void addSelectionListener(SelectionListener listener);

   public abstract void setDataSourceId(int id);

   public abstract String getSelectedName();
  
}
