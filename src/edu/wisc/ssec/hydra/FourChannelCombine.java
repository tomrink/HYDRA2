package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataSource;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;

import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;

import edu.wisc.ssec.adapter.MultiSpectralDataSource;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.ReprojectSwath;

import visad.Data;
import visad.FieldImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.RealType;
import visad.Linear2DSet;
import visad.VisADException;
import visad.georef.MapProjection;
import java.rmi.RemoteException;

import visad.CoordinateSystem;
import visad.RealTupleType;
import visad.SetType;

public class FourChannelCombine extends Compute {

   JLabel[] colorComponents;
   JComponent[] operandComponents;

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
   
   JTextField multplrA;
   JTextField multplrB;
   JTextField multplrC;
   JTextField multplrD;

   FlatField swathImage;
   FlatField result;

   boolean needResample = false;
   boolean needReproject = true;
   
   String dateTimeStr = null;
   
   public FourChannelCombine() {
   }

   public FourChannelCombine(DataBrowser dataBrowser) {
      super(4, 3, "Band Math");
      this.dataBrowser = dataBrowser;
      operators[0] = operationAB;
      operators[1] = operationCD;
      operators[2] = operationLR;
   }

   public JComponent buildGUI() {
      JTextArea textPanel = new JTextArea("Select items in main window to update target (bold box) operand.\n"+"Target operand advances automatically, but can be manually selected.");
      JPanel outerPanel = new JPanel(new GridLayout(4,1));

      LineBorder blackBorder = new LineBorder(Color.black);
      LineBorder blackBorder3 = new LineBorder(Color.black, 3);

      JPanel panel = new JPanel(new FlowLayout());
      textPanel.setBackground(panel.getBackground());
      colorComponents = new JLabel[numOperands];
      borders = new LineBorder[numOperands];
      borders3 = new LineBorder[numOperands];

      operandEnabled = new boolean[numOperands];
      final String[] compNames = new String[numOperands];

      for (int k=0; k<colorComponents.length; k++) {
         JLabel label = new JLabel();
         borders[k] = blackBorder;
         borders3[k] = blackBorder3;
         compNames[k] = "           ";
         operandEnabled[k] = true;
         label.setText(compNames[k]);
         label.setBorder(borders[k]);
         colorComponents[k] = label;

         label.addMouseListener(new java.awt.event.MouseAdapter() {
               public void mouseClicked(java.awt.event.MouseEvent e) {
                  for (int k=0; k<colorComponents.length; k++) {
                     if (e.getComponent() == colorComponents[k]) {
                        setActive(k);
                     }
                  }
               }
             }
          );
      }
      colorComponents[activeIndex].setBorder(borders3[activeIndex]);

      operandEnabled[2] = false;
      operandEnabled[3] = false;
 
      String[] operations = new String[] {"-", "+", "/", "*", " "};
      operandA = colorComponents[0];
      operandB = colorComponents[1];
      comboAB = new JComboBox(operations);
      comboAB.setSelectedIndex(0);
      comboAB.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
              operationAB = (String) comboAB.getSelectedItem();
              operators[0] = operationAB;
           }
      });

      operandC = colorComponents[2];
      operandD = colorComponents[3];
      operations = new String[] {"+", "-", "*", "/", " "};
      comboCD = new JComboBox(operations);
      comboCD.setSelectedIndex(4);
      comboCD.addActionListener(new ActionListener() {
           public void actionPerformed(final ActionEvent e) {
              operationCD = (String) comboCD.getSelectedItem();
              operators[1] = operationCD;
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
              operators[2] = operationLR;
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
      panel.add(new JLabel("a*"));
      panel.add(operandA);
      panel.add(comboAB);
      panel.add(new JLabel("b*"));
      panel.add(operandB);
      panel.add(new JLabel(")"));

      panel.add(comboLR);

      //Right
      panel.add(new JLabel("("));
      panel.add(new JLabel("c*"));
      panel.add(operandC);
      panel.add(comboCD);
      panel.add(new JLabel("d*"));
      panel.add(operandD);
      panel.add(new JLabel(")"));
      
      JPanel panel3 = new JPanel(new FlowLayout());
      JLabel lblA = new JLabel("a=");
      JLabel lblB = new JLabel("b=");
      JLabel lblC = new JLabel("c=");
      JLabel lblD = new JLabel("d=");
      multplrA = new JTextField("1", 2);
      multplrB = new JTextField("1", 2);
      multplrC = new JTextField("1", 2);
      multplrD = new JTextField("1", 2);
      panel3.add(lblA);
      panel3.add(multplrA);
      panel3.add(lblB);
      panel3.add(multplrB);
      panel3.add(lblC);
      panel3.add(multplrC);
      panel3.add(lblD);
      panel3.add(multplrD);
      
      outerPanel.add(textPanel);
      outerPanel.add(panel);
      outerPanel.add(panel3);

      return outerPanel;
   }

   public void disableOperand(int idx) {
       colorComponents[idx].setText("           ");
       operands[idx].setEmpty();
       operandEnabled[idx] = false;
   }

   public void enableOperand(int idx) {
       operandEnabled[idx] = true;
       setActive(idx);
   }

   public void setActive(int idx) {
      if (!operandEnabled[idx]) return;

      super.setActive(idx);

      for (int k=0; k<colorComponents.length; k++) {
         if (k == activeIndex) {
            colorComponents[k].setBorder(borders3[k]);
         }
         else {
            colorComponents[k].setBorder(borders[k]);
         }
      }
   }

   public void updateUI(SelectionEvent e) {
      String name = e.getName();
      colorComponents[activeIndex].setText(name);
      int next = activeIndex;
      while (true) {
         next = ((next+1) % numOperands);
         if (operandEnabled[next]) {
            setActive(next);
            break;
         }
      }
   }

   public Data compute() throws Exception {
       Operand operandA = operands[0];
       Operand operandB = operands[1];
       Operand operandC = operands[2];
       Operand operandD = operands[3];

       operationAB = (String)operators[0];
       operationCD = (String)operators[1];
       operationLR = (String)operators[2];

       FlatField fldA = null;
       FlatField fldB = null;
       FlatField fldC = null;
       FlatField fldD = null;

       String nameA = null;
       String nameB = null;
       String nameC = null;
       String nameD = null;
       
       float fltA = 1f;
       float fltB = 1f;
       float fltC = 1f;
       float fltD = 1f;
       
       try {
          fltA = Float.valueOf(multplrA.getText());      
       }
       catch (Exception e) {
          e.printStackTrace();
       }
       try {
          fltB = Float.valueOf(multplrB.getText());
       }
       catch (Exception e) {
           e.printStackTrace();
       }
       try {
          fltC = Float.valueOf(multplrC.getText());
       }
       catch (Exception e) {
          e.printStackTrace();
       }
       try {
          fltD = Float.valueOf(multplrD.getText());
       }
       catch  (Exception e) {
          e.printStackTrace();
       }
       
       needResample = false;
       Linear2DSet commonGrid = null; // Grid to which operand data is resampled (if necessary)
       float nadirResolution;
     
       fldA = (FlatField) operandA.getData();
       if (fltA != 1f) {
          fldA = multiply(fltA, fldA);
       }
       Linear2DSet setA = (Linear2DSet) fldA.getDomainSet();
       nameA = operandA.getName();
       nadirResolution = DataSource.getNadirResolution(operandA.dataSource, operandA.dataChoice);
       needReproject = DataSource.getDoReproject(operandA.dataSource, operandA.dataChoice);
       dateTimeStr = (String) operandA.dataSource.getProperty(Hydra.dateTimeStr);
       
       boolean needReproA;
       boolean needReproB;
       boolean needReproC;
       boolean needReproD;
       
       boolean needResmplA;
       boolean needResmplB;
       boolean needResmplC;
       boolean needResmplD;
       
       if (!operandB.isEmpty()) {
          fldB = (FlatField) operandB.getData();
          if (fltB != 1f) {
             fldA = multiply(fltB, fldB);
          }
          Linear2DSet setB = (Linear2DSet) fldB.getDomainSet();
          nameB = operandB.getName();
          needResample = !((operandA.dataSource == operandB.dataSource) && (setA.equals(setB)));
          float res = DataSource.getNadirResolution(operandB.dataSource, operandB.dataChoice);
          needReproB = DataSource.getDoReproject(operandB.dataSource, operandB.dataChoice);
          if (res > nadirResolution) nadirResolution = res;
       }
       if (!operandC.isEmpty()) {
          fldC = (FlatField) operandC.getData();
          if (fltC != 1f) {
             fldC = multiply(fltC, fldC);
          }
          Linear2DSet setC = (Linear2DSet) fldC.getDomainSet();
          nameC = operandC.getName();
          needResample = !((operandA.dataSource == operandC.dataSource) && (setA.equals(setC)));
          float res = DataSource.getNadirResolution(operandC.dataSource, operandC.dataChoice);
          needReproC = DataSource.getDoReproject(operandC.dataSource, operandC.dataChoice);
          if (res > nadirResolution) nadirResolution = res;
       }
       if (!operandD.isEmpty()) {
          fldD = (FlatField) operandD.getData();
          if (fltD != 1f) {
             fldD = multiply(fltD, fldD);
          }
          Linear2DSet setD = (Linear2DSet) fldD.getDomainSet();
          nameD = operandD.getName();
          needResample = !((operandA.dataSource == operandD.dataSource) && (setA.equals(setD)));
          float res = DataSource.getNadirResolution(operandD.dataSource, operandD.dataChoice);
          needReproD = DataSource.getDoReproject(operandD.dataSource, operandD.dataChoice);
          if (res > nadirResolution) nadirResolution = res;
       }

       String operName;

       if (needResample) {
          float[][] corners = MultiSpectralData.getLonLatBoundingCorners(fldA.getDomainSet());
          MapProjection mp = Hydra.getSwathProjection(corners);
          commonGrid = Hydra.makeGrid(mp, corners, nadirResolution);

          int mode = Hydra.getReprojectMode();

          if (DataSource.getReduceBowtie(operandA.dataSource, operandA.dataChoice)) {
             String sensorName = DataSource.getSensorName(operandA.dataSource, operandA.dataChoice);
             Hydra.reduceSwathBowtie(fldA, sensorName);
          }
          fldA = ReprojectSwath.swathToGrid(commonGrid, fldA, mode);

          if (fldB != null) {
             if (DataSource.getReduceBowtie(operandB.dataSource, operandB.dataChoice)) {
                String sensorName = DataSource.getSensorName(operandB.dataSource, operandB.dataChoice);
                Hydra.reduceSwathBowtie(fldB, sensorName);
             }
             fldB = ReprojectSwath.swathToGrid(commonGrid, fldB, mode);
          }
          if (fldC != null) {
             if (DataSource.getReduceBowtie(operandC.dataSource, operandC.dataChoice)) {
                String sensorName = DataSource.getSensorName(operandC.dataSource, operandC.dataChoice);
                Hydra.reduceSwathBowtie(fldC, sensorName);
             }
             fldC = ReprojectSwath.swathToGrid(commonGrid, fldC, mode);
          }
          if (fldD != null) {
             if (DataSource.getReduceBowtie(operandD.dataSource, operandD.dataChoice)) {
                String sensorName = DataSource.getSensorName(operandD.dataSource, operandD.dataChoice);
                Hydra.reduceSwathBowtie(fldD, sensorName);
             }
             fldD = ReprojectSwath.swathToGrid(commonGrid, fldD, mode);
          }
       }


       FieldImpl fldAB = null;
       if (operationAB == "-") {
          fldAB = (FieldImpl) fldA.subtract(fldB);
       }
       else if (operationAB == "+") {
          fldAB = (FieldImpl) fldA.add(fldB);
       }
       else if (operationAB == "/") {
          fldAB = (FieldImpl) fldA.divide(fldB);
       }
       else if (operationAB == "*") {
          fldAB = (FieldImpl) fldA.multiply(fldB);
       }
       else if (operationAB == " ") {
          fldAB = fldA;
       }

       FieldImpl fldCD = null;
       if (!operandD.isEmpty) {
          if (operationCD == "-") {
             fldCD = (FieldImpl) fldC.subtract(fldD);
          }
          else if (operationCD == "+") {
             fldCD = (FieldImpl) fldC.add(fldD);
          }
          else if (operationCD == "*") {
             fldCD = (FieldImpl) fldC.multiply(fldD);
          }
          else if (operationCD == "/") {
             fldCD = (FieldImpl) fldC.divide(fldD);
          }
       }
       else if (!operandC.isEmpty) {
          fldCD = fldC;
       }

       FlatField fld = (FlatField) fldAB;

       if (fldAB != null && fldCD != null) {
          if (operationLR == "-") {
             fld = (FlatField) fldAB.subtract(fldCD);
          }
          else if (operationLR == "+") {
             fld = (FlatField) fldAB.add(fldCD);
          }
          else if (operationLR == "*") {
             fld = (FlatField) fldAB.multiply(fldCD);
          }
          else if (operationLR == "/") {
             fld = (FlatField) fldAB.divide(fldCD);
          }
       }

       operName = getOperationName();

       fld = Hydra.cloneButRangeType(RealType.getRealType(operName), fld, false);

       if (!needResample) { // if already resampled, don't resample again
          swathImage = null; // not swath domain
       }
       else {
          swathImage = fld;
       }
       result = fld;

       return fld;
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
         String txt = multplrA.getText().trim();
         float flt = Float.valueOf(txt);
         if (flt != 1f) {
            nameA = txt+"*"+nameA;
         }
         operName = nameA;
      } 

      String nameB = null;
      if (!operandB.isEmpty()) {
         nameB = operandB.getName();
         String txt = multplrB.getText().trim();
         float flt = Float.valueOf(txt);
         if (flt != 1f) {         
            nameB = txt+"*"+nameB;
         }
         nameAB = nameA+operationAB+nameB;
         operName = nameAB;
      }

      String nameC = null;
      if (!operandC.isEmpty()) {
         nameC = operandC.getName();
         String txt = multplrC.getText().trim();
         float flt = Float.valueOf(txt);
         if (flt != 1f) {         
            nameC = txt+"*"+nameC;
         }
         operName = "["+operName+"]"+operationLR+nameC;

         if (!operandD.isEmpty()) {
            String nameD = operandD.getName();
            txt = multplrD.getText().trim();
            flt = Float.valueOf(txt);
            if (flt != 1f) {            
               nameD = txt+"*"+nameD;
            }
            nameCD = nameC+operationCD+nameD;
            operName = "["+nameAB+"]"+operationLR+"["+nameCD+"]";
         }
      }
      
      operName = operName.trim();
      operName = operName.replace('.', ',');

      return operName;
   }

   public void createDisplay(Data data, int mode, int windowNumber) throws Exception {
      FlatField fld = (FlatField) data;
      
      MapProjection mp;
      if (!needResample && needReproject) {
         Operand operandA = operands[0];
         float nadirResolution = DataSource.getNadirResolution(operandA.dataSource, operandA.dataChoice);
         float[][] corners = MultiSpectralData.getLonLatBoundingCorners(fld.getDomainSet());
         mp = Hydra.getSwathProjection(corners);
         Linear2DSet grd = Hydra.makeGrid(mp, corners, nadirResolution);
         if (DataSource.getReduceBowtie(operandA.dataSource, operandA.dataChoice)) {
            String sensorName = DataSource.getSensorName(operandA.dataSource, operandA.dataChoice);
            Hydra.reduceSwathBowtie(fld, sensorName);
         }
         fld = ReprojectSwath.swathToGrid(grd, fld, Hydra.getReprojectMode());
      }
      else {
         mp = Hydra.getDataProjection(fld);
      }

      String name = ((RealType)((FunctionType)fld.getType()).getRange()).getName();

      if (mode == 0 || ImageDisplay.getTarget() == null) {
         HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.grayTable, name, dateTimeStr);
         if (swathImage != null) {
            Hydra.displayableToImage.put(imageDsp, swathImage);
         }
         ImageDisplay iDisplay = new ImageDisplay(imageDsp, mp, windowNumber);
      }
      else if (mode == 1) {
         ImageDisplay.getTarget().updateImageData(fld, Hydra.grayTable, mp, name, dateTimeStr);
      }
      else if (mode == 2) {
         fld = Hydra.makeFlatFieldWithUniqueRange(fld);
         HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(fld, null, Hydra.grayTable, name, dateTimeStr);
         if (swathImage != null) {
            Hydra.displayableToImage.put(imageDsp, swathImage);
         }
         ImageDisplay.getTarget().addOverlayImage(imageDsp);
      }
   }

   public FourChannelCombine clone() {
      FourChannelCombine clone = new FourChannelCombine();
      copy(clone);
      clone.multplrA = new JTextField(multplrA.getText());
      clone.multplrB = new JTextField(multplrB.getText());
      clone.multplrC = new JTextField(multplrC.getText());
      clone.multplrD = new JTextField(multplrD.getText());
      return clone;
   }

   public void selectionPerformed(SelectionEvent e) {
      // Don't accept if operand is a computation itself
      if (e.fromCompute) {
         return;
      }
      else {
         super.selectionPerformed(e);
      }
   }
   
   public static FlatField multiply(float fval, FlatField fltFld) throws Exception  {
      FlatField newFF = new FlatField((FunctionType)fltFld.getType(), fltFld.getDomainSet());
      float[][] values = fltFld.getFloats();
      for (int t=0; t<values.length; t++) {
         for (int i=0; i<values[t].length; i++) {
            values[t][i] *= fval;
         }
      }
      newFF.setSamples(values, false);
      return newFF;
   }
}
