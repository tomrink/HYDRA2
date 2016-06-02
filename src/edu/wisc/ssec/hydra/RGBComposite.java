package edu.wisc.ssec.hydra;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import java.awt.FlowLayout;
import java.awt.Color;

import edu.wisc.ssec.hydra.data.MultiSpectralDataSource;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.ReprojectSwath;

import visad.Data;
import visad.FlatField;
import visad.Set;
import visad.SetType;
import visad.Linear2DSet;
import visad.georef.MapProjection;
import visad.RealTupleType;
import visad.RealType;
import visad.FunctionType;


public class RGBComposite extends Compute {

   JLabel[] colorComponents;

   LineBorder[] borders;
   LineBorder[] borders3;
   
   String dateTimeStr = null;

   public RGBComposite() {
   }

   public RGBComposite(DataBrowser dataBrowser) {
      super(3, "RGBComposite");
      this.dataBrowser = dataBrowser;
   }

   public JComponent buildGUI() {
      LineBorder blackBorder = new LineBorder(Color.black);
      LineBorder redBorder = new LineBorder(Color.red);
      LineBorder greenBorder = new LineBorder(Color.green);
      LineBorder blueBorder = new LineBorder(Color.blue);
      LineBorder redBorder3 = new LineBorder(Color.red, 3);
      LineBorder greenBorder3 = new LineBorder(Color.green, 3);
      LineBorder blueBorder3 = new LineBorder(Color.blue, 3);

      borders = new LineBorder[] {redBorder, greenBorder, blueBorder};
      borders3 = new LineBorder[] {redBorder3, greenBorder3, blueBorder3};

      JPanel panel = new JPanel(new FlowLayout());

      final String[] compNames = {"           ", "           ", "           "};
      colorComponents = new JLabel[compNames.length];

      for (int k=0; k<colorComponents.length; k++) {
         JLabel label = new JLabel();
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
          panel.add(label);
      }
      colorComponents[activeIndex].setBorder(borders3[activeIndex]);

      return panel;
   }

   public void setActive(int idx) {
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
      setActive(((activeIndex+1) % 3));
   }

   public Data compute() throws Exception {
       Operand redOp = operands[0];
       Operand grnOp = operands[1];
       Operand bluOp = operands[2];
       
       FlatField red = (FlatField) redOp.getData();
       FlatField grn = (FlatField) grnOp.getData();
       FlatField blu = (FlatField) bluOp.getData();
       
       boolean redRepro = redOp.dataSource.getDoReproject(redOp.dataChoice);
       boolean grnRepro = grnOp.dataSource.getDoReproject(grnOp.dataChoice);
       boolean bluRepro = bluOp.dataSource.getDoReproject(bluOp.dataChoice);
       
       boolean noneRepro = !redRepro && !grnRepro && !bluRepro;
       boolean allRepro = redRepro && grnRepro && bluRepro;

       float redRes = redOp.dataSource.getNadirResolution(redOp.dataChoice);
       float grnRes = grnOp.dataSource.getNadirResolution(grnOp.dataChoice);
       float bluRes = bluOp.dataSource.getNadirResolution(bluOp.dataChoice);
       
       
       dateTimeStr = (String) operands[0].dataSource.getDateTimeStamp();
       FlatField rgb = null;
       if (allRepro) {
          if (redOp.dataSource.getReduceBowtie(redOp.dataChoice)) {
             String sensorName = redOp.dataSource.getSensorName(redOp.dataChoice);
             Hydra.reduceSwathBowtie(red, sensorName);
          } 
          if (grnOp.dataSource.getReduceBowtie(grnOp.dataChoice)) {
             String sensorName = grnOp.dataSource.getSensorName(grnOp.dataChoice);
             Hydra.reduceSwathBowtie(grn, sensorName);
          }
          if (bluOp.dataSource.getReduceBowtie(bluOp.dataChoice)) {
             String sensorName = bluOp.dataSource.getSensorName(bluOp.dataChoice);
             Hydra.reduceSwathBowtie(blu, sensorName);
          }                    
          float nadirResolution = redRes;
          float[][] corners = MultiSpectralData.getLonLatBoundingCorners(red.getDomainSet());
          MapProjection mp = MultiSpectralDataSource.getSwathProjection(red, corners);
          Linear2DSet grd = MultiSpectralDataSource.makeGrid(mp, corners, nadirResolution);
          int mode = Hydra.getReprojectMode();
          rgb = ReprojectSwath.swathToGrid(grd, new FlatField[] {red, grn, blu}, mode);
       }
       else if (noneRepro) {
          Set setR = red.getDomainSet();
          Set setG = grn.getDomainSet();
          Set setB = blu.getDomainSet();
          
          int count = Hydra.getUniqueID();
          RealTupleType newRangeType = new RealTupleType(new RealType[] 
           {RealType.getRealType("redimage_"+count), RealType.getRealType("greenimage_"+count), RealType.getRealType("blueimage_"+count)});
          
          rgb = new FlatField(new FunctionType(((SetType)setR.getType()).getDomain(), newRangeType), setR);
          if (setR.equals(setG) && setG.equals(setB)) {
             rgb.setSamples(new float[][] {red.getFloats(false)[0], grn.getFloats(false)[0], blu.getFloats(false)[0]}, false);
          }
          else {
             FlatField ffG = (FlatField) grn.resample(setR, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
             FlatField ffB = (FlatField) blu.resample(setR, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS);
             rgb.setSamples(new float[][] {red.getFloats(false)[0], ffG.getFloats(false)[0], ffB.getFloats(false)[0]}, false);
          }
       }

       return rgb;
   }

   public void createDisplay(Data data, int mode, int windowNumber) throws Exception {
      FlatField rgb = (FlatField) data;

      MapProjection mp;
      mp = Hydra.getDataProjection(rgb);

      ImageRGBDisplayable imageDsp = Hydra.makeRGBImageDisplayable(rgb, getOperationName(), dateTimeStr);

      if (mode == 0 || ImageDisplay.getTarget() == null) {
         ImageDisplay iDisplay = new ImageDisplay(imageDsp, mp, windowNumber);
      }
      else if (mode == 1) {
         ImageDisplay.getTarget().updateImageRGBCompositeData(rgb, mp, dateTimeStr);
      }
      else if (mode == 2) {
         ImageDisplay.getTarget().addOverlayImage(imageDsp);
      }
   }

   public String getOperationName() {
      return "["+operands[0].getName()+","+operands[1].getName()+","+operands[2].getName()+"]";
   }

   public RGBComposite clone() {
      RGBComposite clone = new RGBComposite();
      copy(clone);
      return clone;
   }
}
