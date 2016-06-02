package edu.wisc.ssec.hydra;

import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;



import visad.Data;
import visad.FieldImpl;
import visad.FlatField;
import visad.RealType;
import visad.FunctionType;
import visad.georef.MapProjection;


public class FourOperandCombine extends Compute {

   JLabel[] colorComponents;
   JLabel[] operandComponents;

   LineBorder[] borders;
   LineBorder[] borders3;

   JComponent operandA;
   JComponent operandB;
   JComboBox comboAB;
   JComponent operandC;
   JComponent operandD;
   JComboBox comboCD;
   JComboBox comboLR;
   String operationAB = "-";
   String operationCD = " ";
   String operationLR = " ";
   boolean[] operandEnabled;

   int activeIndex;
   
   String dateTimeStr = null;

   public FourOperandCombine() {
   }

   public FourOperandCombine(DataBrowser dataBrowser, Operand[] operands) {
      this.dataBrowser = dataBrowser;
      this.operands = operands;
      this.numOperands = 4;
      this.numOperators = 3;
      this.operators = new Object[numOperators];
      for (int k=0; k<numOperators; k++) {
         this.operators[k] = new String();
      }
   }


   public JComponent buildGUI() {
      LineBorder blackBorder = new LineBorder(Color.black);
      LineBorder blackBorder3 = new LineBorder(Color.black,3);
      LineBorder redBorder = new LineBorder(Color.red,2);
      LineBorder magentaBorder = new LineBorder(Color.magenta,2);
      LineBorder orangeBorder = new LineBorder(Color.orange,2);
      LineBorder blueBorder = new LineBorder(Color.blue,2);

      JPanel panel = new JPanel(new FlowLayout());
      operandComponents = new JLabel[numOperands];
      borders = new LineBorder[] {redBorder, magentaBorder, orangeBorder, blueBorder};

      operandEnabled = new boolean[numOperands];
      final String[] compNames = new String[numOperands];

      for (int k=0; k<numOperands; k++) {
         JLabel label = new JLabel();
         compNames[k] = "           ";
         operandEnabled[k] = true;
         label.setText(compNames[k]);
         label.setBorder(borders[k]);
         operandComponents[k] = label;

         label.addMouseListener(new java.awt.event.MouseAdapter() {
               public void mouseClicked(java.awt.event.MouseEvent e) {
                  for (int k=0; k<operandComponents.length; k++) {
                     if (e.getComponent() == operandComponents[k]) {
                        //setActive(k);
                     }
                  }
               }
             }
          );
      }

      operandEnabled[2] = false;
      operandEnabled[3] = false;
 
      String[] operations = new String[] {"-", "+", "/", "*"};
      operandA = operandComponents[0];
      operandB = operandComponents[1];
      comboAB = new JComboBox(operations);
      comboAB.setSelectedIndex(0);
      comboAB.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
              operationAB = (String) comboAB.getSelectedItem();
           }
      });

      operandC = operandComponents[2];
      operandD = operandComponents[3];
      operations = new String[] {"+", "-", "*", "/", " "};
      comboCD = new JComboBox(operations);
      comboCD.setSelectedIndex(4);
      comboCD.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
              operationCD = (String) comboCD.getSelectedItem();
              if (operationCD == " ") {
                 disableOperand(3);
              }
              else {
                 enableOperand(3);
              }
           }
      });

      operations = new String[] {"/", "+", "-", "*", " "};
      comboLR = new JComboBox(operations);
      comboLR.setSelectedIndex(4);
      comboLR.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
              operationLR = (String) comboLR.getSelectedItem();
              if (operationLR == " ") {
                 disableOperand(2);
                 disableOperand(3);

                 comboCD.setSelectedItem(" ");
              }
              else {
                 enableOperand(2);
              }
           }
      });

      //Left
      panel.add(new JLabel("("));
      panel.add(operandA);
      panel.add(comboAB);
      panel.add(operandB);
      panel.add(new JLabel(")"));

      //panel.add(new JLabel("/"));
      panel.add(comboLR);

      //Right
      panel.add(new JLabel("("));
      panel.add(operandC);
      panel.add(comboCD);
      panel.add(operandD);
      panel.add(new JLabel(")"));

      return panel;
   }

   public void disableOperand(int idx) {
       operandComponents[idx].setText("           ");
       operands[idx].setEmpty();
       operandEnabled[idx] = false;
       operands[idx].disable();
   }

   public void enableOperand(int idx) {
       operandEnabled[idx] = true;
       setActive(idx);
       operands[idx].enable();
   }

   public void updateOperandComp(int idx, Object obj) {
       operandComponents[idx].setText((String) obj);
       operands[idx].name = (String) obj;
   }

   public void setActive(int idx) {
      if (!operandEnabled[idx]) return;
      activeIndex = idx;
   }

   public Data compute() throws Exception {
       Operand operandA = operands[0];
       Operand operandB = operands[1];
       Operand operandC = operands[2];
       Operand operandD = operands[3];
       
       
       dateTimeStr = (String) operandA.dateTimeStr;

     
       FieldImpl fldA = (FieldImpl) operandA.getData();
       FieldImpl fldB = (FieldImpl) operandB.getData();

       String nameA = operandA.getName();
       String nameB = operandB.getName();
       nameA = nameA.trim();
       nameB = nameB.trim();
       String operName;

       FieldImpl fldAB = null;
       String nameAB = null;
       if (operationAB == "-") {
          fldAB = (FieldImpl) fldA.subtract(fldB);
          nameAB = nameA+"-"+nameB;
       }
       else if (operationAB == "+") {
          fldAB = (FieldImpl) fldA.add(fldB);
          nameAB = nameA+"+"+nameB;
       }
       else if (operationAB == "/") {
          fldAB = (FieldImpl) fldA.divide(fldB);
          fldAB = Hydra.infiniteToNaN(fldAB);
          nameAB = nameA+"divide"+nameB;
       }
       else if (operationAB == "*") {
          fldAB = (FieldImpl) fldA.multiply(fldB);
          nameAB = nameA+"*"+nameB;
       }
       nameAB = nameAB;
       operName = nameAB;


       FieldImpl fldC = null;
       FieldImpl fldD = null;
       FieldImpl fldCD = null;
       String nameCD = null;
       if (!operandD.isEmpty) {
          fldD = (FieldImpl) operandD.getData();
          fldC = (FieldImpl) operandC.getData();
          String nameC = operandC.getName();
          String nameD = operandD.getName();
          nameC = nameC.trim();
          nameD = nameD.trim();
          if (operationCD == "-") {
             fldCD = (FieldImpl) fldC.subtract(fldD);
             nameCD = nameC+"-"+nameD;
          }
          else if (operationCD == "+") {
             fldCD = (FieldImpl) fldC.add(fldD);
             nameCD = nameC+"+"+nameD;
          }
          else if (operationCD == "*") {
             fldCD = (FieldImpl) fldC.multiply(fldD);
             nameCD = nameC+"*"+nameD;
          }
          else if (operationCD == "/") {
             fldCD = (FieldImpl) fldC.divide(fldD);
             fldCD = Hydra.infiniteToNaN(fldCD);
             nameCD = nameC+"divide"+nameD;
          }
          nameCD = nameCD;
       }
       else if (!operandC.isEmpty) {
          fldC = (FieldImpl) operandC.getData();
          fldCD = fldC;
          nameCD = operandC.getName();
       }

       FlatField fld = (FlatField) fldAB;
       if (fldAB != null && fldCD != null) {
          if (operationLR == "-") {
             fld = (FlatField) fldAB.subtract(fldCD);
             operName = nameAB+"-"+nameCD;
          }
          else if (operationLR == "+") {
             fld = (FlatField) fldAB.add(fldCD);
             operName = nameAB+"-"+nameCD;
          }
          else if (operationLR == "*") {
             fld = (FlatField) fldAB.multiply(fldCD);
             operName = nameAB+"*"+nameCD;
          }
          else if (operationLR == "/") {
             fld = (FlatField) fldAB.divide(fldCD);
             fld = (FlatField) Hydra.infiniteToNaN(fld);
             operName = nameAB+"divide"+nameCD;
          }
       }

       // have to replace decimal with comma - VisAD doesn't '.' or ' ' in RealType names
       String noDotName = operName.replace(".", ",");
       fld = Hydra.cloneButRangeType(RealType.getRealType(noDotName), fld, false);

       fld = reproject(fld);
       return fld;
   }

   public FlatField reproject(FlatField swath) throws Exception {
      return swath;
   }

   public FourOperandCombine clone() {
      FourOperandCombine clone = new FourOperandCombine();
      copy(clone);
      return clone;
   }

   public String getOperationName() { // what to call this?
      Operand operandA = operands[0];
      Operand operandB = operands[1];
      Operand operandC = operands[2];
      Operand operandD = operands[3];

      String operName = null;
      String nameAB = null;
      String nameCD = null;

      String nameA = null;
      if (!operandA.isEmpty()) {
         nameA = operandA.getName();
         operName = nameA;
      }

      String nameB = null;
      if (!operandB.isEmpty()) {
         nameB = operandB.getName();
         nameAB = nameA+operationAB+nameB;
         operName = nameAB;
      }

      String nameC = null;
      if (!operandC.isEmpty()) {
         nameC = operandC.getName();
         operName = "["+operName+"]"+operationLR+nameC;

         if (!operandD.isEmpty()) {
            String nameD = operandD.getName();
            nameCD = nameC+operationCD+nameD;
            operName = "["+nameAB+"]"+operationLR+"["+nameCD+"]";
         }
      }

      return operName;
   }

   public void createDisplay(Data data, int mode, int windowNumber) throws Exception {
       FlatField fld = (FlatField) data;
       String name = ((RealType)((FunctionType)fld.getType()).getRange()).getName();
       name = name.replace(",",".");
       MapProjection mp = Hydra.getDataProjection(fld);
       
       if (mode == 0 || ImageDisplay.getTarget() == null) {
          HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.grayTable, name, dateTimeStr, null);
          ImageDisplay iDisplay = new ImageDisplay(imageDsp, mp, windowNumber);
       }
       else if (mode == 1) {
          ImageDisplay.getTarget().updateImageData(fld, Hydra.grayTable, mp, name, dateTimeStr);
       }
       else if (mode == 2) {
          fld = Hydra.makeFlatFieldWithUniqueRange(fld);
          HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.grayTable, name, dateTimeStr, null);
          ImageDisplay.getTarget().addOverlayImage(imageDsp);
      }
   }
}
