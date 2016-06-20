package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataSelection;

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
   
   public void setSelected(Object obj) {
   }
}
