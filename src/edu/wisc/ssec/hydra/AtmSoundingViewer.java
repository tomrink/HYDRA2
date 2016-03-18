package edu.wisc.ssec.hydra;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenuBar;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.JTextField;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

import java.awt.Component;
import java.awt.BorderLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowAdapter;
import java.awt.geom.Rectangle2D;
import java.awt.Dimension;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;

import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.SwathSoundingData;
import edu.wisc.ssec.adapter.SpectrumAdapter;

import visad.*;
import visad.georef.LatLonTuple;
import visad.georef.MapProjection;
import java.rmi.RemoteException;

import ucar.unidata.util.ColorTable;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DirectDataChoice;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButtonMenuItem;


public class AtmSoundingViewer extends HydraDisplay {

    AtmSoundingDisplay atmSoundingDsp = null;
    JComponent channelSelectComp = null;
    MultiSpectralData multiSpectData = null;
    SwathSoundingData swathSoundingData = null;
    ImageDisplay imgDisplay = null;
    DataChoice dataChoice = null;
    List dataChoices = null;

    String sourceDescription = null;
    String dateTimeStamp = null;
    String fldName = null;
    String baseDescription = null;
    int dataSourceId;

    JMenuBar menuBar = null;

    JFrame frame = null;

    Hydra hydra = null;
    DataBrowser dataBrowser = null;

    public AtmSoundingViewer(DataChoice dataChoice, String sourceDescription, String dateTimeStamp, int windowNumber, int dataSourceId) throws Exception {
         this.dataChoice = dataChoice;
         this.sourceDescription = sourceDescription;
         this.dateTimeStamp = dateTimeStamp;
         this.dataSourceId = dataSourceId;

         atmSoundingDsp = new AtmSoundingDisplay((DirectDataChoice) dataChoice);
         atmSoundingDsp.showChannelSelector();
         final AtmSoundingDisplay msd = atmSoundingDsp;

         FlatField image = msd.getImageData();
         image = reproject(image);

         MapProjection mapProj = Hydra.getDataProjection(image);

         ColorTable clrTbl = Hydra.invGrayTable;

         HydraRGBDisplayable imageDsp = Hydra.makeImageDisplayable(image, null, clrTbl, fldName, dateTimeStamp);

         //baseDescription = sourceDescription+" "+dateTimeStamp;
         //String str = baseDescription+" "+Float.toString(multiSpectDsp.getWaveNumber())+" cm-1";
         String str = null;
         if (windowNumber > 0) {
            imgDisplay = new ImageDisplay(imageDsp, mapProj, windowNumber, false);
         }
         else {
            imgDisplay = new ImageDisplay(imageDsp, mapProj, false);
         }

         imgDisplay.onlyOverlayNoReplace = true;
         final ImageDisplay iDisplay = imgDisplay;

         swathSoundingData = atmSoundingDsp.getSwathSoundingData();

         channelSelectComp = doMakeChannelSelectComponent();

         final SwathSoundingData data = swathSoundingData;

         ReadoutProbe probe = iDisplay.getReadoutProbe();
         DataReference probeLocationRef = probe.getEarthLocationRef();
         DataReference spectrumRef = msd.getSpectrumRef();
         spectrumRef.setData(data.getSounding((LatLonTuple)probeLocationRef.getData()));
         ProbeLocationChange probeChangeListnr = new ProbeLocationChange(probeLocationRef, spectrumRef, data);

         DataReference probeLocationRefB = (iDisplay.addReadoutProbe(Color.cyan, 0.08, 0.08)).getEarthLocationRef();
         DataReference spectrumRefB = new DataReferenceImpl("spectrumB");
         spectrumRefB.setData(msd.getSwathSoundingData().getSounding((LatLonTuple)probeLocationRefB.getData()));
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
                                  new Dimension(760,480), new Point(220, 0));
         frame.addWindowListener(this);
         iDisplay.setParentFrame(frame);
    }

    public void setDataChoices(List dataChoices) {
        this.dataChoices = dataChoices;
    }

    public JFrame getFrame() {
       return frame;
    }

    public void windowClosing(WindowEvent e) {
       imgDisplay.windowClosing(e);
    }

    public JComponent doMakeComponent() {
        final JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                       imgDisplay.getComponent(), atmSoundingDsp.getDisplayComponent());
        if (SwingUtilities.isEventDispatchThread()) {
             imgDisplay.getControlPanel().add(channelSelectComp);
             splitPane.setDividerLocation(400);
        }
        else {
            try {
               SwingUtilities.invokeAndWait(new Runnable() {
                  public void run() {
                     imgDisplay.getControlPanel().add(channelSelectComp);
                     splitPane.setDividerLocation(400);
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
                     //doChannelCombine();
                 }
             }
       });
       fourChannelCombine.setActionCommand("doFourChannelCombine");
       //tools.add(fourChannelCombine);

       JMenu paramMenu = new JMenu("Parameter");
       paramMenu.getPopupMenu().setLightWeightPopupEnabled(false);

       JMenuItem tempItem = new JMenuItem("Temperature");
       tempItem.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 try {
                   AtmSoundingViewer mcv = new AtmSoundingViewer((DataChoice)dataChoices.get(0), sourceDescription, dateTimeStamp, -1, dataSourceId);
                   // TODO: this sucks
                   mcv.setDataChoices(dataChoices);
                 }
                 catch (Exception exc) {
                   exc.printStackTrace();
                 }
             }
       });
       paramMenu.add(tempItem);

       JMenuItem wvItem = new JMenuItem("WaterVapor");
       wvItem.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 try {
                   AtmSoundingViewer mcv = new AtmSoundingViewer((DataChoice)dataChoices.get(1), sourceDescription, dateTimeStamp, -1, dataSourceId);
                   // TODO: this sucks
                   mcv.setDataChoices(dataChoices);
                 }
                 catch (Exception exc) {
                   exc.printStackTrace();
                 }
             }
       });
       paramMenu.add(wvItem);

       JMenuItem o3Item = new JMenuItem("Ozone");
       o3Item.addActionListener(new ActionListener() {
             public void actionPerformed(ActionEvent e) {
                 try {
                   AtmSoundingViewer mcv = new AtmSoundingViewer((DataChoice)dataChoices.get(2), sourceDescription, dateTimeStamp, -1, dataSourceId);
                   // TODO: this sucks
                   mcv.setDataChoices(dataChoices);
                 }
                 catch (Exception exc) {
                   exc.printStackTrace();
                 }
             }
       });
       paramMenu.add(o3Item);
      
       menuBar.add(paramMenu);
       
       // get the Settings Menu
       JMenu settingsMenu = menuBar.getMenu(1);
       JMenu soundMenu = new JMenu("Sounding");
       soundMenu.getPopupMenu().setLightWeightPopupEnabled(false);
       JMenu backGroundClr = new JMenu("Background Color");
       JRadioButtonMenuItem white = new JRadioButtonMenuItem("white", false);
       JRadioButtonMenuItem black = new JRadioButtonMenuItem("black", true);
       ButtonGroup bg = new ButtonGroup();
       bg.add(black);
       bg.add(white);
       white.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               try {
                  atmSoundingDsp.setBackground(Color.white);
               } catch (Exception exc) {
                  exc.printStackTrace();
               }
           }
       });
       white.setActionCommand("white");
        
       black.addActionListener(new ActionListener() {
           public void actionPerformed(ActionEvent e) {
               try {
                  atmSoundingDsp.setBackground(Color.black);
               } catch (Exception exc) {
                  exc.printStackTrace();
               }
           }
       });
       black.setActionCommand("black");
        
       backGroundClr.add(black);
       backGroundClr.add(white);
        
       soundMenu.add(backGroundClr);
       settingsMenu.add(soundMenu);       


       return menuBar;
    }

    /*
    public void doChannelCombine() {
       final String idA = "A";
       final String idB = "B";
       final String idC = "C";
       final String idD = "D";
       final MyOperand operandA = new MyOperand(multiSpectDsp, idA);
       final MyOperand operandB = new MyOperand(multiSpectDsp, idB);
       final MyOperand operandC = new MyOperand(multiSpectDsp, idC);
       final MyOperand operandD = new MyOperand(multiSpectDsp, idD);

       final FourOperandCombine widget = new FourOperandCombine(new Operand[] {operandA, operandB, operandC, operandD});
       JComponent gui = widget.buildGUI();

       try {
           float val = multiSpectDsp.getWaveNumber();
           float valA = val+40f;
           multiSpectDsp.createSelector(idA, Color.red, valA);
           widget.updateOperandComp(0, Float.toString(valA)); 
           multiSpectDsp.addSelectorListener(idA, new SelectorListener(widget, operandA, 0));

           float valB = val-40f;
           multiSpectDsp.createSelector(idB, Color.magenta, valB);
           widget.updateOperandComp(1, Float.toString(valB));
           multiSpectDsp.addSelectorListener(idB, new SelectorListener(widget, operandB, 1));

           float valC = val+80f;
           multiSpectDsp.createSelector(idC, Color.orange, valC);
           multiSpectDsp.addSelectorListener(idC, new SelectorListener(widget, operandC, 2));
           multiSpectDsp.setSelectorVisible(idC, false);

           float valD = val-80f;
           multiSpectDsp.createSelector(idD, Color.blue, valD);
           multiSpectDsp.addSelectorListener(idD, new SelectorListener(widget, operandD, 3));
           multiSpectDsp.setSelectorVisible(idD, false);
       }
       catch (Exception exc) {
          exc.printStackTrace();
       }

       JButton button = new JButton("Display");
       button.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
             try {
                 Data data = widget.compute();
                 widget.createDisplay(data);
             }
             catch (Exception exc) {
                 exc.printStackTrace();
             }
         }
      });
      gui.add(button);

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
    */


    public JComponent doMakeMultiBandSelectComponent() {
         final float[] levels = swathSoundingData.getSoundingLevels();
         String[] levelNames = new String[levels.length];
         for (int k=0; k<levelNames.length; k++) {
            levelNames[k] = Float.toString(levels[k]); 
         }
         final JComboBox comboBox = new JComboBox(levelNames);
         comboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                 int idx = comboBox.getSelectedIndex();
                 String levelName = (String) comboBox.getSelectedItem();
                 if (atmSoundingDsp.setWaveNumber(levels[idx])) {
                     FlatField image = atmSoundingDsp.getImageData();
                     image = reproject(image);
                     imgDisplay.updateImageData(image);
                     imgDisplay.getDepiction().setName(levelName);
                 }

            }
         });

         try {
           comboBox.setSelectedIndex(swathSoundingData.getLevelIndexFromLevel(swathSoundingData.init_level)); 
         }
         catch (Exception e) {
           e.printStackTrace();
         }

         PropertyChangeListener listener = new PropertyChangeListener () {
            public void propertyChange(PropertyChangeEvent event) {
                float waveNumber = atmSoundingDsp.getWaveNumber();
                try {
                  comboBox.setSelectedIndex(swathSoundingData.getLevelIndexFromLevel(waveNumber));
                } catch (Exception e) {
                  e.printStackTrace();
                }
            }
         };
         atmSoundingDsp.setListener(listener);

         return comboBox;
    }

    public JComponent doMakeChannelSelectComponent() {
       return doMakeMultiBandSelectComponent();
    }

    FlatField reproject(FlatField image) {
       try {
          if (sourceDescription.contains("CrIS")) {
             image = edu.wisc.ssec.adapter.CrIS_SDR_Utility.reprojectCrIS_SDR_swath(image);
          }
       }
       catch (Exception e) {
          e.printStackTrace();
       }

       return image;
    }


//}

public static class ProbeLocationChange extends CellImpl {
    DataReference probeLocationRef;
    DataReference spectrumRef;
    SwathSoundingData swathSoundingData;
    boolean init = false;

    public ProbeLocationChange(DataReference probeLocationRef, DataReference spectrumRef, SwathSoundingData swathSoundingData) throws VisADException, RemoteException {
       this.probeLocationRef = probeLocationRef;
       this.spectrumRef = spectrumRef;
       this.swathSoundingData = swathSoundingData;
       this.addReference(probeLocationRef);
    }

    public synchronized void doAction() throws VisADException, RemoteException {
        if (init) {
           LatLonTuple tup = (LatLonTuple) probeLocationRef.getData();
           try {
              spectrumRef.setData(swathSoundingData.getSounding(tup));
           } catch (Exception e) {
              e.printStackTrace();
           }
        } else {
            init = true;
        }
    }
}

}


/*
class MyOperand extends Operand {
    float waveNumber;
    MultiSpectralDisplay multiSpectDsp;
    String id;

    MyOperand(MultiSpectralDisplay multiSpectDsp, String id) {
       this.multiSpectDsp = multiSpectDsp;
       this.id = id;
    }

    public Data getData() throws VisADException, RemoteException {
       waveNumber = multiSpectDsp.getSelectorValue(id);
       return multiSpectDsp.getImageDataFrom(waveNumber);
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
       return Float.toString(waveNumber);
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
*/
