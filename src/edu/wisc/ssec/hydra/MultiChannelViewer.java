package edu.wisc.ssec.hydra;

import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Point;
import java.awt.event.WindowEvent;

import java.util.HashMap;

import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.ATMS_SDR_Utility;

import visad.*;
import visad.georef.LatLonTuple;
import visad.georef.MapProjection;
import java.rmi.RemoteException;

import ucar.unidata.util.ColorTable;
import edu.wisc.ssec.hydra.data.DataChoice;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;


public class MultiChannelViewer extends HydraDisplay {

    MultiSpectralDisplay multiSpectDsp = null;
    JComponent channelSelectComp = null;
    MultiSpectralData multiSpectData = null;
    ImageDisplay imgDisplay = null;
    DataChoice dataChoice = null;

    Hydra hydra = null;
    DataBrowser dataBrowser = null;
    String sourceDescription = null;
    String dateTimeStamp = null;
    String fldName = null;
    String baseDescription = null;
    int dataSourceId;

    JMenuBar menuBar = null;

    JFrame frame = null;

    float initXmapScale;

    public MultiChannelViewer(Hydra hydra, DataChoice dataChoice, String sourceDescription, String dateTimeStamp, int windowNumber, int dataSourceId) throws Exception {
        this(hydra, dataChoice, sourceDescription, dateTimeStamp, windowNumber, dataSourceId, Float.NaN, null, null);
    }

    public MultiChannelViewer(Hydra hydra, DataChoice dataChoice, String sourceDescription, String dateTimeStamp, int windowNumber, int dataSourceId,
                              float initWaveNumber, float[] xMapRange, float[] yMapRange) throws Exception {
         this.hydra = hydra;
         this.dataBrowser = hydra.getDataBrowser();
         this.dataChoice = dataChoice;
         this.sourceDescription = sourceDescription;
         this.dateTimeStamp = dateTimeStamp;
         this.dataSourceId = dataSourceId;

         multiSpectDsp = new MultiSpectralDisplay((DataChoice) dataChoice, initWaveNumber, xMapRange, yMapRange);
         multiSpectDsp.processor = this;
         initXmapScale = (float) multiSpectDsp.getXmapScale();
         multiSpectDsp.showChannelSelector();
         final MultiSpectralDisplay msd = multiSpectDsp;

         FlatField image = msd.getImageData();
         image = reproject(image);

         MapProjection mapProj = Hydra.getDataProjection(image);

         ColorTable clrTbl = Hydra.invGrayTable;

         HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(image, null, clrTbl, fldName, dateTimeStamp, sourceDescription);

         baseDescription = sourceDescription+" "+dateTimeStamp;
         String str = baseDescription+" "+Float.toString(multiSpectDsp.getWaveNumber())+" cm-1";

         if (windowNumber > 0) {
            imgDisplay = new ImageDisplay(imageDsp, mapProj, windowNumber, false);
         }
         else {
            imgDisplay = new ImageDisplay(imageDsp, mapProj, false);
         }
         imgDisplay.onlyOverlayNoReplace = true;
         final ImageDisplay iDisplay = imgDisplay;
         iDisplay.getDepiction().setName(Float.toString(multiSpectDsp.getWaveNumber()));

         multiSpectData = multiSpectDsp.getMultiSpectralData();

         channelSelectComp = doMakeChannelSelectComponent(multiSpectData.hasBandNames());

         final MultiSpectralData data = msd.getMultiSpectralData();

         ReadoutProbe probe = iDisplay.getReadoutProbe();
         DataReference probeLocationRef = probe.getEarthLocationRef();
         DataReference spectrumRef = msd.getSpectrumRef();
         spectrumRef.setData(data.getSpectrum((LatLonTuple)probeLocationRef.getData()));
         ProbeLocationChange probeChangeListnr = new ProbeLocationChange(probeLocationRef, spectrumRef, data);

         DataReference probeLocationRefB = (iDisplay.addReadoutProbe(Color.cyan, 0.08, 0.08)).getEarthLocationRef();
         DataReference spectrumRefB = new DataReferenceImpl("spectrumB");
         spectrumRefB.setData(msd.getMultiSpectralData().getSpectrum((LatLonTuple)probeLocationRefB.getData()));
         msd.addRef(spectrumRefB, Color.cyan);
         ProbeLocationChange probeChangeListnrB = new ProbeLocationChange(probeLocationRefB, spectrumRefB, data);

         JComponent comp = doMakeComponent();

         menuBar = buildMenuBar();

         String title;
         if (windowNumber > 0) {
            title = "Window "+windowNumber;
         }
         else {
            title = sourceDescription+" "+dateTimeStamp;
         }
         frame = Hydra.createAndShowFrame(title, comp, menuBar,
                                  new Dimension(480,760), new Point(220, 0));
         frame.addWindowListener(this);
         iDisplay.setParentFrame(frame);
    }

    public MultiChannelViewer cloneMcV() {
        MultiChannelViewer mcv = null;
        try {
           mcv = new MultiChannelViewer(this.hydra, this.dataChoice, this.sourceDescription, this.dateTimeStamp, 0, this.dataSourceId,
                                        this.multiSpectDsp.getWaveNumber(), 
                                        this.multiSpectDsp.getXmapRange(), this.multiSpectDsp.getYmapRange());
        }
        catch (Exception e) {
           e.printStackTrace();
        }
        return mcv;
    }

    public MultiSpectralDisplay getMultiSpectralDisplay() {
       return multiSpectDsp;
    }

    public JFrame getFrame() {
       return frame;
    }

    public void windowClosing(WindowEvent e) {
       imgDisplay.windowClosing(e);
    }

    public JComponent doMakeComponent() {
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                       multiSpectDsp.getDisplayComponent(), imgDisplay.getComponent());
        if (SwingUtilities.isEventDispatchThread()) {
           imgDisplay.getButtonPanel().add(channelSelectComp);
           splitPane.setDividerLocation(200);
        }
        else {
           try {
              SwingUtilities.invokeAndWait(new Runnable() {
                  public void run() {
                     imgDisplay.getButtonPanel().add(channelSelectComp);
                     splitPane.setDividerLocation(200);
                  }
              });
           } 
           catch (Exception e) {
              e.printStackTrace();
           }
        }

        return splitPane;
    }

    public JMenuBar buildMenuBar() {
       JMenuBar menuBar = imgDisplay.getMenuBar();

       JMenu hlpMenu = new JMenu("Help");
       hlpMenu.getPopupMenu().setLightWeightPopupEnabled(false);

       hlpMenu.add(new JTextArea("Spectrum Display:\n   Zoom (rubber band): SHFT+DRAG \n   Reset: CNTL+CLCK"));

       menuBar.add(hlpMenu);

       // get the tools JMenu
       JMenu tools = menuBar.getMenu(0);
       JMenuItem fourChannelCombine = new JMenuItem("FourChannelCombine");
       fourChannelCombine.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 String cmd = e.getActionCommand();
                 if (cmd.equals("doFourChannelCombine")) {
                     doChannelCombine();
                 }
             }
       });
       fourChannelCombine.setActionCommand("doFourChannelCombine");
       tools.add(fourChannelCombine);

       JMenu viewMenu = new JMenu("Window");
       viewMenu.getPopupMenu().setLightWeightPopupEnabled(false);
       JMenuItem newItem = new JMenuItem("New");
       newItem.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 try {
                    cloneMcV();
                 }
                 catch (Exception exc) {
                 exc.printStackTrace();
                 }
             }
       });
       viewMenu.add(newItem);
       menuBar.add(viewMenu);
       
       // get the Settings Menu
       JMenu settingsMenu = menuBar.getMenu(1);
       JMenu spectMenu = new JMenu("Spectrum");
       spectMenu.getPopupMenu().setLightWeightPopupEnabled(false);
       JMenu backGroundClr = new JMenu("Background Color");
       JRadioButtonMenuItem white = new JRadioButtonMenuItem("white", false);
       JRadioButtonMenuItem black = new JRadioButtonMenuItem("black", true);
       ButtonGroup bg = new ButtonGroup();
       bg.add(black);
       bg.add(white);
       white.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               try {
                  multiSpectDsp.setBackground(Color.white);
               } catch (Exception exc) {
                  exc.printStackTrace();
               }
           }
       });
       white.setActionCommand("white");
        
       black.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               try {
                  multiSpectDsp.setBackground(Color.black);
               } catch (Exception exc) {
                  exc.printStackTrace();
               }
           }
       });
       black.setActionCommand("black");
        
       backGroundClr.add(black);
       backGroundClr.add(white);
        
       spectMenu.add(backGroundClr);
       settingsMenu.add(spectMenu);

       
       return menuBar;
    }

    public void doChannelCombine() {
       final String idA = "A";
       final String idB = "B";
       final String idC = "C";
       final String idD = "D";
       final MyOperand operandA = new MyOperand(multiSpectDsp, idA, dataSourceId, dateTimeStamp);
       final MyOperand operandB = new MyOperand(multiSpectDsp, idB, dataSourceId, dateTimeStamp);
       final MyOperand operandC = new MyOperand(multiSpectDsp, idC, dataSourceId, dateTimeStamp);
       final MyOperand operandD = new MyOperand(multiSpectDsp, idD, dataSourceId, dateTimeStamp);

       //final FourOperandCombine widget = new FourOperandCombine(dataBrowser, new Operand[] {operandA, operandB, operandC, operandD});
       Object obj = null;
       if (sourceDescription.contains("CrIS SDR")) {
          obj = new CrIS_SDR_FourOperandCombine(dataBrowser, new Operand[] {operandA, operandB, operandC, operandD});
       }
       else {
          obj = new FourOperandCombine(dataBrowser, new Operand[] {operandA, operandB, operandC, operandD});
       }
       final FourOperandCombine widget = (FourOperandCombine) obj;
       
       JComponent gui = widget.buildGUI();
       gui.add(widget.makeActionComponent());

       operandA.isEmpty = false;
       operandB.isEmpty = false;

       try {
           multiSpectDsp.setInactive();
           multiSpectDsp.hideChannelSelector();

           Gridded1DSet set = multiSpectDsp.getDomainSet();
           float lo = set.getLow()[0];
           float hi = set.getHi()[0];
           float fac = initXmapScale/multiSpectDsp.getXmapScale();
           float off = 0.04f*(hi - lo)*fac;

           float val = multiSpectDsp.getWaveNumber();
           float valA = val+off;
           valA = (valA>hi) ? (2*hi - valA) : valA;
           operandA.waveNumber = valA;
           multiSpectDsp.createSelector(idA, Color.red, valA);
           widget.updateOperandComp(0, Float.toString(valA)); 
           multiSpectDsp.addSelectorListener(idA, new SelectorListener(widget, operandA, 0));

           float valB = val-off;
           valB = (valB<lo) ? 2*lo-valB : valB;
           operandB.waveNumber = valB;
           multiSpectDsp.createSelector(idB, Color.magenta, valB);
           widget.updateOperandComp(1, Float.toString(valB));
           multiSpectDsp.addSelectorListener(idB, new SelectorListener(widget, operandB, 1));

           float valC = val+2*off;
           valC = (valC>hi) ? (2*hi - valC) : valC;
           multiSpectDsp.createSelector(idC, Color.orange, valC);
           multiSpectDsp.addSelectorListener(idC, new SelectorListener(widget, operandC, 2));
           multiSpectDsp.setSelectorVisible(idC, false);

           float valD = val-2*off;
           valD = (valD<lo) ? 2*lo-valD : valD;
           multiSpectDsp.createSelector(idD, Color.blue, valD);
           multiSpectDsp.addSelectorListener(idD, new SelectorListener(widget, operandD, 3));
           multiSpectDsp.setSelectorVisible(idD, false);

           multiSpectDsp.showChannelSelector();

           multiSpectDsp.setActive();
       }
       catch (Exception exc) {
          exc.printStackTrace();
       }


       JFrame frame = Hydra.createAndShowFrame("FourChannelCombine", gui);
       frame.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
               multiSpectDsp.removeSelector(idA);
               multiSpectDsp.removeSelector(idB);
               multiSpectDsp.removeSelector(idC);
               multiSpectDsp.removeSelector(idD);
            }
          }
       );

       Point pt = getFrame().getLocation();
       frame.setLocation(pt.x,pt.y-60);
    }


    public JComponent doMakeMultiBandSelectComponent() {
         Object[] allBands = multiSpectData.getBandNames().toArray();
         final HashMap emisBandMap = multiSpectData.getBandNameMap();
         final JComboBox bandSelectComboBox = new JComboBox(allBands);
         bandSelectComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                 String bandName = (String)bandSelectComboBox.getSelectedItem();
                 if (bandName == null)
                    return;

                 Float waveNum = (Float) emisBandMap.get(bandName);
                 if (multiSpectDsp.setWaveNumber(waveNum.floatValue())) {
                     FlatField image = multiSpectDsp.getImageData();
                     image = reproject(image);
                     imgDisplay.updateImageData(image);
                     imgDisplay.getDepiction().setName(waveNum.toString());
                 }

            }
         });
         bandSelectComboBox.setSelectedItem(multiSpectData.init_bandName); 

         PropertyChangeListener listener = new PropertyChangeListener () {
            public void propertyChange(PropertyChangeEvent event) {
                float waveNumber = multiSpectDsp.getWaveNumber();
                try {
                  bandSelectComboBox.setSelectedIndex(multiSpectData.getChannelIndexFromWavenumber(waveNumber));
                  imgDisplay.getDepiction().setName(Float.toString(waveNumber));
                } catch (Exception e) {
                  e.printStackTrace();
                }
            }
         };
         multiSpectDsp.setListener(listener);

         return bandSelectComboBox;
    }

    public JComponent doMakeHyperSpectralSelectComponent() {
         final JTextField wavenumbox = new JTextField(Float.toString(multiSpectDsp.getWaveNumber()), 5);
         wavenumbox.addActionListener(new ActionListener() {
              public void actionPerformed(ActionEvent e) {
                  String tmp = wavenumbox.getText().trim();
                  if (multiSpectDsp.setWaveNumber(Float.valueOf(tmp))) {
                     String txt = Float.toString(multiSpectDsp.getWaveNumber());
                     wavenumbox.setText(txt);
                     FlatField image = multiSpectDsp.getImageData();
                     image = reproject(image);
                     imgDisplay.updateImageData(image, baseDescription+" "+txt+" cm-1");
                     imgDisplay.getDepiction().setName(txt);
                  }
                  else {
                     wavenumbox.setText(Float.toString(multiSpectDsp.getWaveNumber()));
                  }
              }
         });

         PropertyChangeListener listener = new PropertyChangeListener () {
            public void propertyChange(PropertyChangeEvent event) {
                FlatField image = multiSpectDsp.getImageData();
                image = reproject(image);
                float waveNumber = multiSpectDsp.getWaveNumber();
                String txt = Float.toString(waveNumber);
                wavenumbox.setText(txt);
                imgDisplay.updateImageData(image, baseDescription+" "+txt+" cm-1");
                imgDisplay.getDepiction().setName(txt);
            }
         };
         multiSpectDsp.setListener(listener);

         return wavenumbox;
    }

    public JComponent doMakeChannelSelectComponent(boolean comboBoxSelect) {
       if (comboBoxSelect) {
          return doMakeMultiBandSelectComponent();
       }
       else {
          return doMakeHyperSpectralSelectComponent();
       }
    }

    FlatField reproject(FlatField image) {
       try {
          if (sourceDescription.contains("CrIS SDR")) {
             image = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(image);
          }
       }
       catch (Exception e) {
          e.printStackTrace();
       }

       return image;
    }
    
    FlatField processRange(FlatField image, Float waveNum) {
       try {
          if (sourceDescription.contains("ATMS")) {
              float[][] range = image.getFloats(false);
              Linear2DSet dset = (Linear2DSet) image.getDomainSet();
              Linear1DSet fovSet = dset.getLinear1DComponent(0);
              int fovStart = (int) fovSet.getFirst();
              int fovStop = (int) fovSet.getLast();
              float[] new_range = ATMS_SDR_Utility.applyLimbCorrection(range[0], waveNum, fovStart, fovStop);
              image.setSamples(new float[][] {new_range}, false);
          }
       }
       catch (Exception e) {
          e.printStackTrace();
       }
       
       return image;
    }

}

class ProbeLocationChange extends CellImpl {
    DataReference probeLocationRef;
    DataReference spectrumRef;
    MultiSpectralData multiSpectData;
    boolean init = false;

    public ProbeLocationChange(DataReference probeLocationRef, DataReference spectrumRef, MultiSpectralData multiSpectData) throws VisADException, RemoteException {
       this.probeLocationRef = probeLocationRef;
       this.spectrumRef = spectrumRef;
       this.multiSpectData = multiSpectData;
       this.addReference(probeLocationRef);
    }

    public synchronized void doAction() throws VisADException, RemoteException {
        if (init) {
           LatLonTuple tup = (LatLonTuple) probeLocationRef.getData();
           try {
              spectrumRef.setData(multiSpectData.getSpectrum(tup));
           } catch (Exception e) {
              e.printStackTrace();
           }
        } else {
            init = true;
        }
    }
}

class MyOperand extends Operand {
    float waveNumber;
    MultiSpectralDisplay multiSpectDsp;
    int dataSourceId;
    String id;
    String dateTimeStr;

    MyOperand(MultiSpectralDisplay multiSpectDsp, String id, int dataSourceId, String dateTimeStr) {
       this.multiSpectDsp = multiSpectDsp;
       this.id = id;
       this.dataSourceId = dataSourceId;
       this.dateTimeStr = dateTimeStr;
    }

    public Data getData() throws VisADException, RemoteException {
       //waveNumber = multiSpectDsp.getSelectorValue(id);
       FlatField data = multiSpectDsp.getImageDataFrom(waveNumber);
       //data = reproject((FlatField)data);
       return data; 
    }

    public void disable() {
       isEmpty = true;
       multiSpectDsp.setSelectorVisible(id, false);
    }

    public void enable() {
       isEmpty = false;
       try {
          waveNumber = multiSpectDsp.getSelectorValue(id);
          multiSpectDsp.setSelectorValue(id, waveNumber);
       }
       catch (Exception e) {
          e.printStackTrace();
       }
       multiSpectDsp.setSelectorVisible(id, true);
    }

    public String getName() {
       return (dataSourceId+":"+Float.toString(waveNumber)+" ");
    }

    public Operand clone() {
       MyOperand operand = new MyOperand(this.multiSpectDsp, this.id, this.dataSourceId, this.dateTimeStr);
       operand.waveNumber = this.waveNumber;
       operand.isEmpty = this.isEmpty;
       return operand;
    }
}

class CrIS_SDR_FourOperandCombine extends FourOperandCombine {

   public CrIS_SDR_FourOperandCombine(DataBrowser dataBrowser, Operand[] operands) {
      super(dataBrowser, operands);
   }

   public FlatField reproject(FlatField swath) throws Exception {
      FlatField grid = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(swath);
      return grid;
   }

   public CrIS_SDR_FourOperandCombine clone() {
      CrIS_SDR_FourOperandCombine clone = new CrIS_SDR_FourOperandCombine(this.dataBrowser, this.operands);
      copy(clone);
      return clone;
   }
}

class SelectorListener implements PropertyChangeListener {
    FourOperandCombine widget;
    MyOperand operand;
    int index;

    public SelectorListener(FourOperandCombine widget, MyOperand operand, int index) {
       this.widget = widget;
       this.operand = operand;
       this.index = index;
    }

    public void propertyChange(PropertyChangeEvent event) {
       Float flt = (Float) event.getNewValue();
       String str = flt.toString();
       widget.updateOperandComp(index, str);
       operand.waveNumber = flt.floatValue();
    }
}
