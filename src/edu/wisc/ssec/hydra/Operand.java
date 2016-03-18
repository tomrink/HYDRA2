package edu.wisc.ssec.hydra;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;

import visad.Data;
import visad.VisADException;
import java.rmi.RemoteException;

public class Operand {
       boolean isEmpty = true;

       DataSourceImpl dataSource;
       DataSelection dataSelection;
       DataChoice dataChoice;
       Selection selection;
       String name;
       String dateTimeStr;

       Compute compute;

       public Operand() {
       }

       public Data getData() throws Exception {
          Data data = null;
          if (compute != null) {
             data = compute.compute();
          }
          else {
             update();
             data = dataSource.getData(dataChoice, null, dataSelection, null);
          }
          return data;
       }

       //TODO: This is needed in case the region selection was changed with the same
       //      same set of components above.  A lot of this  complexity is to maintain
       //      backward compatibility. It uses the saved DataSelection created as the
       //      user selects the input components.
       public void update() {
          selection.applyToDataSelection(dataChoice, dataSelection);
       }

       public boolean isEmpty() {
          return isEmpty;
       }

       public void setEmpty() {
          dataSource = null;
          dataSelection = null;
          dataChoice = null;
          selection = null;
          isEmpty = true;
       }

       public void disable() {
       }

       public void enable() {
       }

       public String getName() {
          return name;
       }

       public Operand clone() {
          Operand clone = new Operand();

          clone.isEmpty = this.isEmpty;
          clone.dataSource = this.dataSource;
          clone.dataSelection = this.dataSelection;
          clone.dataChoice = this.dataChoice;
          clone.selection  = this.selection;
          clone.name  = this.name;
          clone.compute = this.compute;
          
          return clone;
       }
}
