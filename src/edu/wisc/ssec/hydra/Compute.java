package edu.wisc.ssec.hydra;

import ucar.unidata.data.DataSelection;

import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.Cursor;

import visad.Data;


public class Compute implements SelectionListener {
 
   JComponent gui;

   int activeIndex;
   Operand[] operands;
   int numOperands;
   Object[] operators;
   int numOperators;

   JFrame frame;
   String title;

   public DataBrowser dataBrowser;

   public Compute(int numOperands, int numOperators, String title) {
      this.numOperands = numOperands;
      this.numOperators = numOperators;
      this.title = title;

      operands = new Operand[numOperands];
      for (int k=0; k<numOperands; k++) {
         operands[k] = new Operand();
      }

      if (numOperators > 0) {
         operators = new Object[numOperators];
         for (int k=0; k<numOperators; k++) {
            operators[k] = new String();
         }
      }

      gui = buildGUI();
      gui.add(makeActionComponent());

      SelectionAdapter.addSelectionListenerToAll(this);
   }

   public Compute(int numOperands, String title) {
      this(numOperands, 0, title);
   }

   /** This is for manual initialization and cloning
    *  Note: does not call addSelectionListenerToAll
    */
   public Compute() {
   }

   public JComponent buildGUI() {
      return null;
   }

   public Data compute() throws Exception {
      return null;
   }

   public void createDisplay(Data data, int mode, int windowNumber) throws Exception {
   }

   void setCursorToWait() {
      frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
   }

   void setCursorToDefault() {
      frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   }

   public String getOperationName() {
      return null;
   }

   public JComponent makeActionComponent() {
      JButton create = new JButton("Create");
      class MyListener implements ActionListener {
         Compute compute;

         public MyListener(Compute compute) {
            this.compute = compute;
         }

         public void actionPerformed(ActionEvent e) {
             /** new stuff, incomplete */
             LeafInfo info = new LeafInfo(compute.clone(), getOperationName(), 0);
             DefaultMutableTreeNode node = new DefaultMutableTreeNode(info);
             dataBrowser.addUserTreeNode(node);
         }
      };
      create.addActionListener(new MyListener(this));

      JPanel panel = new JPanel();
      panel.add(create);
      return panel;
   }

   public void show(int x, int y, String title) {
      frame = Hydra.createAndShowFrame(title, gui);
      frame.setLocation(x,y);
      final Compute compute = this;
      frame.addWindowListener(new WindowAdapter() {
          public void windowClosing(WindowEvent e) {
             SelectionAdapter.removeSelectionListenerFromAll(compute);
          }
        }
      );

   }

   public void show(int x, int y) {
      show(x,y,title);
   }

   public void selectionPerformed(SelectionEvent e) {
      Operand operand = operands[activeIndex];

      if (!e.fromCompute) {
         operand.dataSource = e.getDataSource();
         operand.selection = e.getSelection();
         operand.dataChoice = e.getSelection().getSelectedDataChoice();

         DataSelection dataSelection = new DataSelection();
         operand.selection.applyToDataSelection(dataSelection);
         operand.dataSelection = dataSelection;
         operand.compute = null;
      }
      else {
         operand = new Operand();
         operand.dataSource = e.compute.operands[0].dataSource;
         operand.selection = e.compute.operands[0].selection;
         operand.dataChoice = e.compute.operands[0].dataChoice;
         operand.compute = e.compute;
         operands[activeIndex] = operand;
      }

      operand.name = e.getName();
      operand.isEmpty = false;
   
      updateUI(e);
   }

   public void updateUI(SelectionEvent e) {
   }

   public void setActive(int idx) {
      activeIndex = idx;
   }

   public Compute copy(Compute compute) {
      compute.numOperands = numOperands;
      compute.operands = new Operand[numOperands];
      for (int k=0; k<numOperands; k++) {
         compute.operands[k] = operands[k].clone();
      }

      compute.numOperators = numOperators;
      compute.operators = new Object[numOperators];
      for (int k=0; k<numOperators; k++) {
         compute.operators[k] = operators[k];
      }

      return compute;
   }

   public Compute clone() {
      return null;
   }
}
