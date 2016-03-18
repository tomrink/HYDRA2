package edu.wisc.ssec.hydra;

import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;


import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.SpectrumAdapter;
import edu.wisc.ssec.adapter.MultiSpectralDataSource;


import java.util.List;
import java.awt.event.*;
import javax.swing.*;
import java.util.*;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import visad.FlatField;

public class MultiSpectralSelection extends SelectionAdapter {
    public static final String PROP_CHAN = "selectedchannel";

    private JComponent list;
    private JComponent outerPanel;

    List dataChoices = null;
    String[] dataChoiceNames = null;

    JComponent geoTimeSelect = null;

    boolean initDone = false;

    DataSourceImpl dataSource = null;

    JComponent[] geoTimeSelectComps = null;

    PreviewSelection[] previewSelects = null;
    PreviewSelection preview = null;

    int selectedIdx = 0;

    float wavenumber = 0;
    int channelIndex = 0;
    String bandName;
    TreePath firstPath;
    DefaultMutableTreeNode root;

    DataBrowser dataBrowser;

    private int dataSourceId;

    TreePath lastSelectedLeafPath = null;

    public MultiSpectralSelection(MultiSpectralDataSource dataSource, DataChoice choice) {
        this(dataSource, choice, null);
    }

    public MultiSpectralSelection(MultiSpectralDataSource dataSource, DataChoice choice, Hydra hydra) {
        super(dataSource);

        this.dataSource = dataSource;

        dataChoices = dataSource.getDataChoices();
        dataChoiceNames = new String[dataChoices.size()];
        geoTimeSelectComps = new JComponent[dataChoices.size()];
        previewSelects = new PreviewSelection[dataChoices.size()];
        for (int k=0; k<dataChoiceNames.length; k++) {
          dataChoiceNames[k] = ((DataChoice)dataChoices.get(k)).getName();
        }

        geoTimeSelect = makeGeoTimeSelect(getSelectedDataChoice(), selectedIdx);
        geoTimeSelectComps[selectedIdx] = geoTimeSelect;

        dataBrowser = hydra.getDataBrowser();
        list = buildTreeSelectionComponent(dataBrowser.getBrowserTree(), hydra.toString());

        dataBrowser.addDataSetTree(root, firstPath, hydra, previewSelects[selectedIdx]);
    }

    public JComponent buildTreeSelectionComponent(final JTree tree, String rootName) {
        int cnt = 0;
        for (int k=0; k<dataChoices.size(); k++) {
           if (((MultiSpectralDataSource)dataSource).getMultiSpectralData(k) != null) cnt++;
        }
        final MultiSpectralData[] msdArray = new MultiSpectralData[cnt];
        final HashMap[] bandMapArray = new HashMap[msdArray.length];
        for (int k=0; k<msdArray.length; k++) {
           msdArray[k] = ((MultiSpectralDataSource)dataSource).getMultiSpectralData(k);
           bandMapArray[k] = msdArray[k].getBandNameMap();
        }

        Object[] allBands = null;
        float[] cntrWvln = null;
        if (msdArray.length == 2) { // combine emissive and reflective bands
           Object[] emisBands = msdArray[0].getBandNames().toArray();
           Object[] reflBands = msdArray[1].getBandNames().toArray();
           allBands = new Object[reflBands.length + emisBands.length];
           System.arraycopy(reflBands, 0, allBands, 0, reflBands.length);
           System.arraycopy(emisBands, 0, allBands, reflBands.length, emisBands.length);

           cntrWvln = new float[allBands.length];
           HashMap<String, Float> reflMap = msdArray[1].getBandNameMap();
           for (int k=0; k<reflBands.length; k++) {
              float val = reflMap.get((String)reflBands[k]).floatValue();
              cntrWvln[k] = val;
           }
           HashMap<String, Float> emisMap = msdArray[0].getBandNameMap();
           for (int k=0; k<emisBands.length; k++) {
              float val = emisMap.get((String)emisBands[k]).floatValue();
              cntrWvln[reflBands.length+k] = val;
           }
        }
        else {
           allBands = msdArray[0].getBandNames().toArray();
           cntrWvln = new float[allBands.length];
           HashMap<String, Float> bandMap = msdArray[0].getBandNameMap();
           for (int k=0; k<allBands.length; k++) {
              float val = bandMap.get((String)allBands[k]).floatValue();
              cntrWvln[k] = val;
           }
        }

        String dfltBandName = msdArray[0].init_bandName;
        bandName = dfltBandName;

        //TODO: combine this with same code in mousePressed
        HashMap<String, Float> bandMap = null;
        try {
           for (int k=0; k<bandMapArray.length; k++) {
              if (bandMapArray[k].containsKey(bandName)) {
                 bandMap = bandMapArray[k];
                 setWaveNumber(bandMap.get(bandName), bandName);
                 setChannelIndex(msdArray[k].getChannelIndexFromWavenumber(bandMap.get(bandName)));
                 setDataChoice(k);
               }
            }
         }
         catch (Exception exc) {
            System.out.println(exc);
         }


        root = new DefaultMutableTreeNode(new NodeInfo(this, rootName));

        for (int k=0; k<allBands.length; k++) {
            String bandName = (String)allBands[k];
            DefaultMutableTreeNode leafNode =
                  new DefaultMutableTreeNode(new LeafInfo(this, bandName, "("+Float.toString(cntrWvln[k])+")", k));
            root.add(leafNode);
            if (bandName.equals(dfltBandName)) {
               firstPath = new TreePath(new Object[] {root, leafNode});
            }
        }

        final Object thisObj = this;
        MouseListener ml = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            int selRow = tree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
            if(selRow != -1) {
              if(e.getClickCount() == 1) {
                  DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();
                  if (node == null) return;

                  String bandName = null;
                  Object nodeInfo = node.getUserObject();
                  if (node.isLeaf()) {
                     LeafInfo leaf = (LeafInfo)nodeInfo;
                     if (leaf.source == thisObj) {
                        lastSelectedLeafPath = selPath;
                        bandName = leaf.name;
                        dataBrowser.updateSpatialTemporalSelectionComponent(previewSelects[0]);
                     }
                  }
                  else {
                     NodeInfo info = (NodeInfo) node.getUserObject();
                     if (info.source == thisObj) {
                        dataBrowser.updateSpatialTemporalSelectionComponent(previewSelects[0]);
                        previewSelects[0].updateBoxSelector();
                     }
                  }
                  
                  if (bandName == null) return;

                    HashMap<String, Float> bandMap = null;
                    try {
                       for (int k=0; k<bandMapArray.length; k++) {
                           if (bandMapArray[k].containsKey(bandName)) {
                              bandMap = bandMapArray[k];
                              setWaveNumber(bandMap.get(bandName), bandName);
                              setChannelIndex(msdArray[k].getChannelIndexFromWavenumber(bandMap.get(bandName)));
                              setDataChoice(k);
                              fireSelectionEvent();
                           }
                       }
                    }
                    catch (Exception exc) {
                       System.out.println(exc);
                    }

                    previewSelects[0].updateBoxSelector();
               }
               else if(e.getClickCount() == 2) {
                  //pass
               }
            }
          }
        };
        tree.addMouseListener(ml);

        return tree;
    }

    public Object getLastSelectedLeafPath() {
       return lastSelectedLeafPath;
    }

    public Object getLastSelectedComp() {
       return previewSelects[0];
    }

    public JComponent getComponent() {
      return outerPanel;
    }

    JComponent makeGeoTimeSelect(DataChoice choice, int idx) {
        //- create preview image
        FlatField image = null;
        try {
          image = (FlatField) dataSource.getData(choice, null, null, null);
        }
        catch (Exception e) {
          System.out.println(e);
        }

        JComponent geoTimeSelect = null;
        try {
          PreviewSelection preview = new PreviewSelection(choice, image, null);
          previewSelects[idx] = preview;
        }
        catch (Exception e) {
          System.out.println(e);
        }

        return geoTimeSelect;
    }

    void setChannelIndex(int idx) {
      channelIndex = idx;
    }

    void setWaveNumber(float wavenumber, String bandName) {
      this.wavenumber = wavenumber;
      this.bandName = bandName;
    }

    void setDataChoice(int idx) {
      selectedIdx = idx;
      DataChoice choice = (DataChoice) dataChoices.get(idx);
    }

    public int applyToDataSelection(DataChoice choice, DataSelection dataSelection) {
       previewSelects[0].applyToDataSelection(dataSelection);
       return 0;
    }

    public void applyToDataSelection(DataSelection dataSelection) {
       previewSelects[0].applyToDataSelection(dataSelection);
       dataSelection.putProperty(PROP_CHAN, wavenumber);
       dataSelection.putProperty(SpectrumAdapter.channelIndex_name, channelIndex);
    }

    public DataChoice getSelectedDataChoice() {
      return (DataChoice) dataChoices.get(selectedIdx);
    }

    public void setDataSourceId(int dataSourceId) {
       this.dataSourceId = dataSourceId;
    }

    public String getSelectedName() {
      return dataSourceId+":"+"B"+bandName;
    }
}
