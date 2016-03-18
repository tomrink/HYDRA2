package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataSource;
import ucar.unidata.data.DataSourceImpl;
import ucar.unidata.data.DataChoice;
import ucar.unidata.data.DataSelection;

import java.util.List;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import java.util.*;

import javax.swing.JTree;
import javax.swing.SwingWorker;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeModel;

import visad.FlatField;

public class BasicSelection extends SelectionAdapter {
    private JLabel picture;
    public  DefaultMutableTreeNode root;
    private JComponent list;

    private JTree browserTree;

    List dataChoices = null;
    String[] dataChoiceNames = null;
    String[] dataChoiceDescs = null;

    PreviewDisplay previewDisplay;

    JFrame frame = null;

    boolean initDone = false;

    DataSourceImpl dataSource = null;

    PreviewSelection[] previewSelects = null;

    int selectedIdx = 0;
    int defaultIdx = 0;

    DataBrowser dataBrowser;
    TreePath firstPath;

    private int dataSourceId;

    private String sourceDescription;

    TreePath lastSelectedLeafPath = null;

    public BasicSelection(DataSourceImpl dataSource, DataChoice choice) {
       this(dataSource, choice, null);
    }

    public BasicSelection(DataSourceImpl dataSource, DataChoice choice, final Hydra hydra) {
        super(dataSource);

        this.dataSource = dataSource;
        dataBrowser = hydra.getDataBrowser();
        sourceDescription = hydra.getSourceDescription();

        dataChoices = dataSource.getDataChoices();
        dataChoiceNames = new String[dataChoices.size()];
        dataChoiceDescs = new String[dataChoices.size()];
        previewSelects = new PreviewSelection[dataChoices.size()];
        for (int k=0; k<dataChoiceNames.length; k++) {
          dataChoiceNames[k] = ((DataChoice)dataChoices.get(k)).getName();
          dataChoiceDescs[k] = DataSource.getDescription(dataSource, (DataChoice)dataChoices.get(k));
        }
        selectedIdx = DataSource.getDefaultChoice(dataSource, dataChoices);
        defaultIdx = selectedIdx;

        browserTree = dataBrowser.getBrowserTree();
        buildTreeSelectionComponent(hydra.toString());

        SwingUtilities.invokeLater(new Runnable() {
           public void run() {
              makeGeoTimeSelect(getSelectedDataChoice(), selectedIdx);
              previewDisplay = DataBrowser.getPreviewDisplay();
              dataBrowser.addDataSetTree(root, firstPath, hydra, previewSelects[defaultIdx]);
           }
        });


        final JTree tree = browserTree;
        final Object thisObj = this;
        MouseListener ml = new MouseAdapter() {
        public void mousePressed(MouseEvent e) {
            int selRow = tree.getRowForLocation(e.getX(), e.getY());
            TreePath selPath = tree.getPathForLocation(e.getX(), e.getY());
            if(selRow != -1) {
              if(e.getClickCount() == 1) {
                  DefaultMutableTreeNode node = (DefaultMutableTreeNode) tree.getLastSelectedPathComponent();

                  if (node == null) return;

                  Object nodeInfo = node.getUserObject();
                  if (node.isLeaf()) {
                     LeafInfo leaf = (LeafInfo)nodeInfo;
                     if (leaf.source == thisObj) {
                        selectedIdx = leaf.index;
                        lastSelectedLeafPath = selPath;
                        updateSelectionDisplay(selectedIdx);
                        //now done in updateSelectionDisplay to ensure select comps are ready
                        //fireSelectionEvent();
                     }
                  }
                  else {
                     NodeInfo info = (NodeInfo)nodeInfo;
                     if (info.source == thisObj) {
                        previewSelects[defaultIdx].updateBoxSelector();
                     }
                  }
               }
               else if(e.getClickCount() == 2) {
                  //pass
               }
            }
          }
        };
        tree.addMouseListener(ml);
    }

    public void buildTreeSelectionComponent(String topName) {
        DefaultMutableTreeNode top = new DefaultMutableTreeNode(new NodeInfo(this, topName));
        root = top;
   
        for (int k=0; k<dataChoiceNames.length; k++) {
            DefaultMutableTreeNode leafNode = 
                  new DefaultMutableTreeNode(new LeafInfo(this, dataChoiceNames[k], dataChoiceDescs[k], k));
            top.add(leafNode);
            if (k == selectedIdx) {
               firstPath = new TreePath(new Object[] {top, leafNode});
            }
        }
    }

    protected void updateSelectionDisplay(int idx) {
       if (previewSelects[idx] == null) {
          dataBrowser.setCursorToWait();
          makeGeoTimeSelect((DataChoice) dataChoices.get(idx), idx);
          dataBrowser.updateSpatialTemporalSelectionComponent(previewSelects[idx]);
          dataBrowser.setCursorToDefault();
          fireSelectionEvent();
       }
       else {
          previewSelects[idx].updateBoxSelector();
          try {
             previewDisplay.updateFrom(previewSelects[idx]);
             dataBrowser.updateSpatialTemporalSelectionComponent(previewSelects[idx]);
          } catch (Exception e) {
             e.printStackTrace();
          }
          fireSelectionEvent();
       }
    }

    void makeGeoTimeSelect(DataChoice choice, int idx) {
       try {
          FlatField image = PreviewSelection.makePreviewImage(dataSource, choice, sourceDescription);
          PreviewSelection preview = new PreviewSelection(choice, image, null);
          previewSelects[idx] = preview;
       }
       catch (Exception e) {
          System.out.println(e);
          e.printStackTrace();
       }
    }

    public int applyToDataSelection(DataChoice dataChoice, DataSelection dataSelection) {
       int index = -1;
       for (int k=0; k<dataChoices.size(); k++) {
           if (((DataChoice)dataChoices.get(k)) == dataChoice) {
              index = k;
           }
       }
       previewSelects[index].applyToDataSelection(dataSelection);
       return index;
    }

    public void applyToDataSelection(DataSelection dataSelection) {
       previewSelects[selectedIdx].applyToDataSelection(dataSelection);
    }

    public DataChoice getSelectedDataChoice() {
      return (DataChoice) dataChoices.get(selectedIdx);
    }

    public void setDataSourceId(int dataSourceId) {
       this.dataSourceId = dataSourceId;
    }

    public String getSelectedName() {
       String str = dataSourceId+":"+dataChoiceNames[selectedIdx];
       return str;
    }

    public Object getLastSelectedLeafPath() {
       return lastSelectedLeafPath;
    }

    public Object getLastSelectedComp() {
       return previewSelects[selectedIdx];
    }
}
