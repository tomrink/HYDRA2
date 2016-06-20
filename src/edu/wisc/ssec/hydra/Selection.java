package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;

import java.util.ArrayList;

public interface Selection {

   public abstract void applyToDataSelection(DataSelection select);

   public abstract int applyToDataSelection(DataChoice choice, DataSelection select);

   public abstract DataChoice getSelectedDataChoice();

   public abstract ArrayList<SelectionListener> getSelectionListeners();

   public abstract void removeSelectionListener(SelectionListener listener);
 
   public abstract void addSelectionListener(SelectionListener listener);

   public abstract String getSelectedName();
   
   public abstract void setSelected(Object obj);
}
