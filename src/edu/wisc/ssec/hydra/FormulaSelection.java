package edu.wisc.ssec.hydra;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;

public class FormulaSelection extends SelectionAdapter {

   FormulaSelection() {
      super();
   }

   public void applyToDataSelection(DataSelection select) {
   }

   public int applyToDataSelection(DataChoice choice, DataSelection select) {
      return -1;
   }

   public DataChoice getSelectedDataChoice() {
      return null;
   }

   public void fireSelectionEvent(Compute compute, String name) {
      // Don't notify Tools of RGBComposite selection
      if (compute instanceof RGBComposite) {
         return;
      }
      SelectionEvent e = new SelectionEvent(this, compute, name);
      for (int k=0; k<selectionListeners.size(); k++) {
         selectionListeners.get(k).selectionPerformed(e);
      }
   }

   public Object getLastSelectedLeafPath() {
      return null;
   }

   public Object getLastSelectedComp() {
      return null;
   }
}
