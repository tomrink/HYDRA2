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
import visad.CoordinateSystem;

import visad.Data;
import visad.FlatField;
import visad.Set;
import visad.SetType;
import visad.Linear2DSet;
import visad.georef.MapProjection;
import visad.RealTupleType;
import visad.RealType;
import visad.FunctionType;
import visad.Linear1DSet;


public class RGBComposite extends Compute {

   JLabel[] colorComponents;

   LineBorder[] borders;
   LineBorder[] borders3;
   
   String dateTimeStr = null;

   public RGBComposite() {
      super(3, "RGBComposite");
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
       
       CoordinateSystem coordSysR = red.getDomainCoordinateSystem();
       CoordinateSystem coordSysG = grn.getDomainCoordinateSystem();
       CoordinateSystem coordSysB = blu.getDomainCoordinateSystem();
       boolean allGEOS = coordSysR instanceof GEOSProjection && coordSysG instanceof GEOSProjection &&
                         coordSysB instanceof GEOSProjection;
       
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
          
          boolean setsEqual = false;
          Set domSet = red.getDomainSet();
          
          if (setR.equals(setG) && setB.equals(setR)) {
             setsEqual = true;
          }
          
          int count = Hydra.getUniqueID();
          RealTupleType newRangeType = new RealTupleType(new RealType[] 
           {RealType.getRealType("redimage_"+count), RealType.getRealType("greenimage_"+count), RealType.getRealType("blueimage_"+count)});
          
          rgb = new FlatField(new FunctionType(((SetType)domSet.getType()).getDomain(), newRangeType), domSet);
          if (setsEqual) {
             rgb.setSamples(new float[][] {red.getFloats(false)[0], grn.getFloats(false)[0], blu.getFloats(false)[0]}, false);
          }
          else {
             // find lowest resolution channel
             int targetIdx = 0;
             if (grnRes > redRes) {
                targetIdx = 1;
                if (bluRes > grnRes) {
                   targetIdx = 2;
                }
             }
             else if (bluRes > redRes) {
                targetIdx = 2;
             }
             if (targetIdx == 1) domSet = grn.getDomainSet();
             if (targetIdx == 2) domSet = blu.getDomainSet(); 
             
             //reset output domain set to loweset resolution input channel's domain
             rgb = new FlatField(new FunctionType(((SetType)domSet.getType()).getDomain(), newRangeType), domSet);             
             
             if (allGEOS) {
                red = Hydra.makeGEOSRadiansDomainField(red, (GEOSProjection) coordSysR);
                grn = Hydra.makeGEOSRadiansDomainField(grn, (GEOSProjection) coordSysG);
                blu = Hydra.makeGEOSRadiansDomainField(blu, (GEOSProjection) coordSysB);
                setR = red.getDomainSet();
                setG = grn.getDomainSet();
                setB = blu.getDomainSet();
             }             
             
             FlatField[] rgbComps = new FlatField[] {red, grn, blu};
             Set[] rgbSets = new Set[] {setR, setG, setB};

             Set targetSet = setR;
             if (targetIdx == 1) targetSet = setG;
             if (targetIdx == 2) targetSet = setB;  
             int source1 = (targetIdx + 1) % 3;
             int source2 = (targetIdx + 2) % 3;
             
             float[][] vals1;
             if (!targetSet.equals(rgbSets[source1])) {
                if (allGEOS) {
                   vals1 = Hydra.goesResample(rgbComps[source1], (Linear2DSet)targetSet).getFloats(false);
                }
                else {
                   vals1 = rgbComps[source1].resample(targetSet, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS).getFloats(false);
                }
             }
             else {
                vals1 = rgbComps[source1].getFloats(false);
             }
             
             float[][] vals2;
             if (!targetSet.equals(rgbSets[source2])) {
                if (allGEOS) {
                   vals2 = Hydra.goesResample(rgbComps[source2], (Linear2DSet)targetSet).getFloats(false);
                }
                else {
                   vals2 = rgbComps[source2].resample(targetSet, Data.WEIGHTED_AVERAGE, Data.NO_ERRORS).getFloats(false);                  
                }
             }
             else {
                vals2 = rgbComps[source2].getFloats(false);
             }
             
             float[][] rgbRange = new float[3][];
             rgbRange[targetIdx] = rgbComps[targetIdx].getFloats(false)[0];
             rgbRange[source1] = vals1[0];
             rgbRange[source2] = vals2[0];
             
             rgb.setSamples(rgbRange, false);
          }
       }

       return rgb;
   }

   public void createDisplay(Data data, int mode, int windowNumber) throws Exception {
      FlatField rgb = (FlatField) data;

      MapProjection mp;
      mp = Hydra.getDataProjection(rgb);
      
      DatasetInfo dsInfo = new DatasetInfo(getOperationName(), new DataSourceInfo(dateTimeStr));

      ImageRGBDisplayable imageDsp = Hydra.makeRGBImageDisplayable(rgb, getOperationName());

      if (mode == 0 || ImageDisplay.getTarget() == null) {
         ImageDisplay iDisplay = new ImageDisplay(imageDsp, mp, windowNumber, dsInfo, false);
      }
      else if (mode == 1) {
         ImageDisplay.getTarget().updateImageRGBCompositeData(rgb, mp, getOperationName(), dsInfo);
      }
      else if (mode == 2) {
         ImageDisplay.getTarget().addOverlayImage(imageDsp, dsInfo);
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
