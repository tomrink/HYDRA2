package edu.wisc.ssec.hydra;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.ButtonGroup;
import javax.swing.JList;
import javax.swing.SwingWorker;
import javax.swing.JSplitPane;
import javax.swing.JScrollPane;
import javax.swing.filechooser.FileSystemView;

import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeExpansionListener;
import javax.swing.tree.DefaultTreeCellRenderer;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.WindowEvent;
import java.awt.event.MouseEvent;
import java.awt.Point;
import java.awt.Dimension;
import java.awt.Cursor;

import java.io.File;
import java.io.PrintStream;
import java.io.FileOutputStream;

import java.util.ArrayList;
import java.util.HashMap;

public class DataBrowser extends HydraDisplay implements ActionListener, TreeSelectionListener, TreeExpansionListener {

   public static String version = "3.6.6";

   private static DataBrowser instance = null;

   JFileChooser fc;

   JFrame frame = null;

   JComponent guiPanel; 

   JSplitPane splitPane;

   JComboBox windowSelect;

   JComboBox actionType;

   JList dataSourceList;

   JMenuBar menuBar;

   JCheckBoxMenuItem regionMatch = null;
   
   JCheckBoxMenuItem parallelCompute = null;

   Point frameLoc = null;

   DefaultMutableTreeNode root;
   DefaultTreeModel rootModel;
   JTree rootTree;

   JTree datasetTree;
   DefaultMutableTreeNode datasetsNode;
   DefaultTreeModel treeModel;

   DefaultMutableTreeNode userNode;

   JComponent geoTimeSelect;

   HashMap<DefaultMutableTreeNode, Hydra> datasetToHydra = new HashMap<DefaultMutableTreeNode, Hydra>();

   HashMap<DefaultMutableTreeNode, TreePath> datasetToDefaultPath = new HashMap<DefaultMutableTreeNode, TreePath>();

   //HashMap<DefaultMutableTreeNode, JComponent> datasetToDefaultComp = new HashMap<DefaultMutableTreeNode, JComponent>();
   HashMap<DefaultMutableTreeNode, PreviewSelection> datasetToDefaultComp = new HashMap<DefaultMutableTreeNode, PreviewSelection>();

   Hydra hydra = null;

   DefaultMutableTreeNode selectedLeafNode = null;

   DefaultMutableTreeNode selectedNode = null;

   FormulaSelection formulaSelection = null;

   int cnt = 0;
   boolean first = true;

   public static PreviewDisplay previewDisplay = null;


   public DataBrowser() {

      instance = this;
      //Create a file chooser
      fc = new JFileChooser();
      fc.setMultiSelectionEnabled(true);
      fc.setAcceptAllFileFilterUsed(false);

      guiPanel = buildGUI();

      buildMenuBar();

      formulaSelection = new FormulaSelection();

      String title = "HYDRA "+version;
      frame = Hydra.createAndShowFrame(title, guiPanel, menuBar, new Dimension(568, 360));
      frame.addWindowListener(this);
      frameLoc = frame.getLocation();
   }

   public JComponent buildGUI() {

      userNode = new DefaultMutableTreeNode("Combinations");

      datasetsNode = new DefaultMutableTreeNode("Datasets");
      treeModel = new DefaultTreeModel(datasetsNode);
      datasetTree = new JTree(treeModel);
      datasetTree.setShowsRootHandles(true);
      DefaultTreeCellRenderer render = new DefaultTreeCellRenderer();
      render.setLeafIcon(null);
      render.setClosedIcon(null);
      render.setOpenIcon(null);
      datasetTree.setCellRenderer(render);
      datasetTree.addTreeSelectionListener(this);
      datasetTree.addTreeExpansionListener(this);


      root = new DefaultMutableTreeNode("root");
      root.add(datasetsNode);
      root.add(userNode);
      rootModel = new DefaultTreeModel(root);

      rootTree = new JTree(rootModel);
      rootTree.setShowsRootHandles(true);
      rootTree.setRootVisible(false);
      render = new DefaultTreeCellRenderer();
      render.setLeafIcon(null);
      render.setClosedIcon(null);
      render.setOpenIcon(null);
      rootTree.setCellRenderer(render);
      rootTree.addTreeSelectionListener(this);
      rootTree.addTreeExpansionListener(this);
      rootTree.addMouseListener(this);


      JScrollPane treeScrollPane = new JScrollPane(rootTree);
   
      geoTimeSelect = new JPanel();
      splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, treeScrollPane, geoTimeSelect);
      splitPane.setDividerLocation(184);

      JPanel outerPanel = new JPanel();
      outerPanel.setLayout(new BorderLayout());

      outerPanel.add(splitPane, BorderLayout.CENTER);
      JComponent actionComp = makeActionComponent();
      outerPanel.add(actionComp, BorderLayout.SOUTH);

      
      return outerPanel;
   }

   public JTree getBrowserTree() {
      return this.rootTree;
   }

   public static PreviewDisplay getPreviewDisplay() {
      if (previewDisplay == null) {
         try {
            previewDisplay = new PreviewDisplay();
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      return previewDisplay;
   }

   //public void addDataSetTree(final DefaultMutableTreeNode node, TreePath firstPath, Hydra hydra, final JComponent comp) {
   public void addDataSetTree(final DefaultMutableTreeNode node, TreePath firstPath, Hydra hydra, final PreviewSelection comp) {
       datasetToHydra.put(node, hydra);
       Object[] nodes = firstPath.getPath();
       TreePath selectedPath = new TreePath(root);
       selectedPath = selectedPath.pathByAddingChild(datasetsNode);
       for (int k=0; k<nodes.length; k++) {
          selectedPath = selectedPath.pathByAddingChild(nodes[k]);
       }

       final TreePath finalPath = selectedPath;
       datasetToDefaultPath.put(node, selectedPath);
       datasetToDefaultComp.put(node, comp);
       SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               rootModel.insertNodeInto(node, datasetsNode, 0);
               rootTree.setSelectionPath(finalPath);
               rootTree.scrollPathToVisible(finalPath);
               updateSpatialTemporalSelectionComponent(comp);
           }
       });
   }

   public void addUserTreeNode(final DefaultMutableTreeNode node) {

      TreePath selectedPath = new TreePath(root);
      selectedPath = selectedPath.pathByAddingChild(userNode);
      selectedPath = selectedPath.pathByAddingChild(node);

       final TreePath finalPath = selectedPath;
       SwingUtilities.invokeLater(new Runnable() {
           public void run() {
               rootModel.insertNodeInto(node, userNode, 0);
               rootTree.setSelectionPath(finalPath);
               rootTree.scrollPathToVisible(finalPath);
           }
       });
   }

   /* keep for now, replaced by below
   public void updateSpatialTemporalSelectionComponent(JComponent comp) {
      if (splitPane.getRightComponent() != comp) {
         splitPane.setRightComponent(comp);
      }
   }
   */

   public void updateSpatialTemporalSelectionComponent(PreviewSelection comp) {
      if (first) {
         try {
            previewDisplay.updateFrom(comp);
            splitPane.setRightComponent(previewDisplay.doMakeContents());
            previewDisplay.draw();
            first = false;
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
      else {
         try {
            previewDisplay.updateFrom(comp);
         }
         catch (Exception e) {
            e.printStackTrace();
         }
      }
   }

   public void buildMenuBar() {
      menuBar = new JMenuBar();

      JMenu fileMenu = new JMenu("File");
      fileMenu.getPopupMenu().setLightWeightPopupEnabled(false);
      menuBar.add(fileMenu);

      JMenuItem openFile = new JMenuItem("File(s)");
      openFile.addActionListener(this);
      openFile.setActionCommand("OpenFile");

      JMenuItem openDir = new JMenuItem("VIIRS directory");
      openDir.addActionListener(this);
      openDir.setActionCommand("OpenDirV");
      
      JMenuItem openDirA = new JMenuItem("AHI directory");
      openDirA.addActionListener(this);
      openDirA.setActionCommand("OpenDirA");

      JMenuItem openRemote = new JMenuItem("Remote");
      openRemote.addActionListener(this);
      openRemote.setActionCommand("OpenRemote");

      JMenuItem exitItem = new JMenuItem("Exit");
      exitItem.addActionListener(this);
      exitItem.setActionCommand("Exit");

      fileMenu.add(openFile);
      fileMenu.add(openDir);
      fileMenu.add(openDirA);
      // not yet, fileMenu.add(openRemote);
      fileMenu.add(exitItem);


      JMenu editMenu = new JMenu("Edit");
      editMenu.getPopupMenu().setLightWeightPopupEnabled(false);
      menuBar.add(editMenu);

      JMenuItem remove = new JMenuItem("Remove Dataset");
      remove.addActionListener(this);
      remove.setActionCommand("RemoveDataset");
      editMenu.add(remove);

      remove = new JMenuItem("Remove Combination");
      remove.addActionListener(this);
      remove.setActionCommand("RemoveFormula");
      editMenu.add(remove);


      JMenu toolsMenu = new JMenu("Tools");
      toolsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
      JMenuItem rgb = new JMenuItem("RGB Composite");
      rgb.addActionListener(this);
      rgb.setActionCommand("doRGB");
      toolsMenu.add(rgb);
      JMenuItem fourChannelCombine = new JMenuItem("Band Math");
      fourChannelCombine.addActionListener(this);
      fourChannelCombine.setActionCommand("doFourChannelCombine");
      toolsMenu.add(fourChannelCombine);
      
      menuBar.add(toolsMenu);

      JMenu settingsMenu = new JMenu("Settings");
      settingsMenu.getPopupMenu().setLightWeightPopupEnabled(false);
      regionMatch = new JCheckBoxMenuItem("region matching", true);
      regionMatch.addActionListener(this);
      regionMatch.setActionCommand("regionMatch");
      settingsMenu.add(regionMatch);
      
      JMenu reprojectMode = new JMenu("Reproject Mode");
      JRadioButtonMenuItem mode0 = new JRadioButtonMenuItem("nearest", true);
      JRadioButtonMenuItem mode2 = new JRadioButtonMenuItem("bilinear", false);
      ButtonGroup bg = new ButtonGroup();
      bg.add(mode0);
      bg.add(mode2);
      mode0.addActionListener(this);
      mode0.setActionCommand("nearest");
      mode2.addActionListener(this);
      mode2.setActionCommand("bilinear");
      reprojectMode.add(mode0);
      reprojectMode.add(mode2);
      
      settingsMenu.add(reprojectMode);
      
      parallelCompute = new JCheckBoxMenuItem("parallel compute", Hydra.getDoParallel());
      parallelCompute.addActionListener(this);
      parallelCompute.setActionCommand("doParallel");
      settingsMenu.add(parallelCompute);
      
      menuBar.add(settingsMenu);
   }

    public void valueChanged(TreeSelectionEvent e) {
        DefaultMutableTreeNode node = 
            (DefaultMutableTreeNode) ((JTree)rootTree).getLastSelectedPathComponent();

        if (node == null) return;

        if (node.isLeaf()) {
           selectedLeafNode = node;
           selectedNode = null;
           hydra = datasetToHydra.get(node.getParent());


           // TODO: this block of code modifies the display action gui for the multiChannelView
           // Need to think about a generalized approach.  Note: hydra will be null for combinations
           // hence the check.
           if (hydra != null) {
              if (hydra.multiDisplay) {
                 windowSelect.setSelectedIndex(0);
                 windowSelect.setEnabled(false);
                 actionType.setEnabled(false);
              }
              else {
                  windowSelect.setEnabled(true);
                  if (windowSelect.getSelectedIndex() > 0) {
                     actionType.setEnabled(true);
                  }
              }
           }
           else {   
              if (windowSelect.getSelectedIndex() == 0) {
                 if (windowSelect.getItemCount() == 1) {
                    actionType.setEnabled(false);
                 }
                 else {
                    windowSelect.setEnabled(true);
                 }
              }
              else {
                 windowSelect.setEnabled(true);
                 actionType.setEnabled(true);
              }
           }
           // ---------------------------------


        }
        else {
           selectedNode = node;
           selectedLeafNode = null;
           updateSpatialTemporalSelectionComponent(datasetToDefaultComp.get(node));
        }
    }

    public void treeCollapsed(TreeExpansionEvent e) {
    }

    public void treeExpanded(TreeExpansionEvent e) {
    }

    public void mousePressed(MouseEvent e) {
       int selRow = rootTree.getRowForLocation(e.getX(), e.getY());
       if(selRow != -1) {
          if(e.getClickCount() == 1) {
             DefaultMutableTreeNode node = (DefaultMutableTreeNode) rootTree.getLastSelectedPathComponent();

             if (node == null) return;

             // only for Formulas, return otherwise
             if (node.getParent() != userNode) return;

             Object nodeInfo = node.getUserObject();
             if (node.isLeaf()) {
                LeafInfo leaf = (LeafInfo)nodeInfo;
                formulaSelection.fireSelectionEvent((Compute)leaf.source, leaf.name);
             }
             else {
                NodeInfo info = (NodeInfo)nodeInfo;
            }
         }
         else if(e.getClickCount() == 2) {
           //pass
         }
      }
    }

    public void actionPerformed(ActionEvent e) {
       String cmd = e.getActionCommand();
       if (e.getActionCommand().equals("OpenFile")) {
          fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
          int returnVal = fc.showOpenDialog(frame);
          if (returnVal == JFileChooser.APPROVE_OPTION) {
             File[] files = fc.getSelectedFiles();
             filesSelected(files);
          }
       }
       else if (cmd.startsWith("OpenDir")) {
          fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
          int returnVal = fc.showOpenDialog(frame);
          if (returnVal == JFileChooser.APPROVE_OPTION) {
             File file = fc.getSelectedFile();
             Class ds = null;
             try {
                switch (cmd) {
                   case "OpenDirV":
                      ds = Class.forName("edu.wisc.ssec.hydra.data.VIIRSDataSource");
                      break;
                   case "OpenDirA":
                      ds = Class.forName("edu.wisc.ssec.hydra.data.AHIDirectory");
                      break;
                }
             }
             catch (Exception exc) {
                exc.printStackTrace();
             }
             directorySelected(file, ds);
          }
       }
       else if (cmd.equals("RemoveDataset")) {
          if (selectedNode == null || selectedNode == datasetsNode) {
             return;
          }
          datasetToHydra.remove(selectedNode);
          datasetToDefaultPath.remove(selectedNode);
          datasetToDefaultComp.remove(selectedNode);
          rootModel.removeNodeFromParent(selectedNode);
          int cnt = rootModel.getChildCount(datasetsNode);
          if (cnt >= 1) {
             DefaultMutableTreeNode node = (DefaultMutableTreeNode) datasetsNode.getChildAt(0);
             NodeInfo nInfo = (NodeInfo)node.getUserObject();
             TreePath path = (TreePath)((SelectionAdapter)nInfo.source).getLastSelectedLeafPath();
             if (path == null) {
                path = datasetToDefaultPath.get(node);
             }
             rootTree.setSelectionPath(path);
             rootTree.scrollPathToVisible(path);
             PreviewSelection comp = (PreviewSelection)((SelectionAdapter)nInfo.source).getLastSelectedComp();
             updateSpatialTemporalSelectionComponent(comp);
             comp.updateBoxSelector();
          }
          else {
             updateSpatialTemporalSelectionComponent(null);
          }
       }
       else if (cmd.equals("RemoveFormula")) {
          if (selectedLeafNode == userNode) {
             return;
          }
          rootModel.removeNodeFromParent(selectedLeafNode);
          int cnt  = rootModel.getChildCount(userNode);
          if (cnt >= 1) {
          }
       }
       else if (cmd.equals("OpenRemote")) {
       }
       else if (cmd.equals("Exit")) {
          java.lang.System.exit(0);
       }
       else if (cmd.equals("doRGB")) {
          (new RGBComposite(this)).show(frameLoc.x+600, frameLoc.y);
       }
       else if (cmd.equals("doFourChannelCombine")) {
          (new FourChannelCombine(this)).show(frameLoc.x+600, frameLoc.y);
       }
       else if (cmd.equals("regionMatch")) {
          if (regionMatch.getState()) {
             Hydra.setRegionMatching(true);
          }
          else {
             Hydra.setRegionMatching(false);
          }
       }
       else if (cmd.equals("nearest")) {
          Hydra.setReprojectMode(0);
       }
       else if (cmd.equals("bilinear")) {
          Hydra.setReprojectMode(2);
       }
       else if (cmd.equals("doParallel")) {
          if (parallelCompute.getState()) {
             Hydra.setDoParallel(true);
          }
          else {
             Hydra.setDoParallel(false);
          }
       }
    }

    public void filesSelected(File[] files) {
       setCursorToWait();
       Hydra hydra = new Hydra(this);
     
       class Task extends SwingWorker<String, Object> {
          Hydra hydra;
          File[] files;

          public Task(File[] files, Hydra hydra) {
             this.files = files;
             this.hydra = hydra;
          }

          public String doInBackground() {
             this.hydra.dataSourceSelected(files);
             return "done";
          }

          protected void done() {
             setCursorToDefault();
          }
       }

       (new Task(files, hydra)).execute();
    }

    public void directorySelected(File dir, Class ds) {
       setCursorToWait();
       Hydra hydra = new Hydra(this);

       class Task extends SwingWorker<String, Object> {
           File dir;
           Hydra hydra;
           Class ds;

           public Task(File dir, Hydra hydra, Class ds) {
               this.dir = dir;
               this.hydra = hydra;
               this.ds = ds;
           }

           public String doInBackground() {
               this.hydra.dataSourceSelected(dir, ds);
               return "done";
           }

           protected void done() {
               setCursorToDefault();
           }
       }

       (new Task(dir, hydra, ds)).execute();
    }

    public void windowClosing(WindowEvent e) {
       java.lang.System.exit(0);
    }

    public Point getLocation() {
        return frame.getLocation();
    }

   public static DataBrowser getInstance() {
      return instance;
   }

   public void windowRemoved(int windowNumber) {
       WindowItem item = null;
       int cnt = windowSelect.getItemCount() - 1;
       for (int k=0; k<cnt; k++) {
          item = (WindowItem) windowSelect.getItemAt(k+1);
          if (item.windowNumber == windowNumber) {
             windowSelect.removeItem(item);
             break;
          }
      }
      // reset to position 1 so that New is not the final selection if windows remain
      if (windowSelect.getSelectedIndex() == 0 && windowSelect.getItemCount() > 1) {
         windowSelect.setSelectedIndex(1);
      }
   }

   public JComponent makeActionComponent() {

      JPanel actionPanel = new JPanel(new FlowLayout());

      actionType = new JComboBox(new String[] {"Replace", "Overlay"}); 
      actionType.setEnabled(false);
      
      windowSelect = new JComboBox(new String[] {"New"});
      windowSelect.setSelectedIndex(0);
      windowSelect.setEnabled(false);
      windowSelect.addActionListener(new ActionListener() {
          public void actionPerformed(ActionEvent e) {
             if (windowSelect.getSelectedItem().equals("New")) {
                actionType.setSelectedIndex(0);
                actionType.setEnabled(false);
             }
             else {
                ArrayList imgDspList = ImageDisplay.getImageDisplayList();
                for (int k=0; k<imgDspList.size(); k++) {
                   ImageDisplay imgDsp = (ImageDisplay)imgDspList.get(k);
                   imgDsp.setIsTarget(false);
                }
                int targetIndex = windowSelect.getSelectedIndex();
                WindowItem item = (WindowItem) windowSelect.getSelectedItem();
                for (int k=0; k<imgDspList.size(); k++) {
                   ImageDisplay imgDsp = (ImageDisplay)imgDspList.get(k);
                   if (item.windowNumber == imgDsp.getWindowNumber()) {
                      imgDsp.setIsTarget(true);
                      imgDsp.toFront();
                      if (imgDsp.onlyOverlayNoReplace) {
                         actionType.setSelectedIndex(1);
                         actionType.setEnabled(false);
                      }
                      else {
                         actionType.setEnabled(true);
                      }
                      break;
                   }
                }
             }
          }
      });

      JButton displayButton = new JButton("Display");
      displayButton.addActionListener(new ActionListener() {
         public void actionPerformed(ActionEvent e) {
            setCursorToWait();

            class Task extends SwingWorker<String, Object> {
                int mode = 0;
                int windowNumber = 1;
                boolean imageCreated = false;

                public String doInBackground() {
                   if (windowSelect.getSelectedItem().equals("New")) {
                      mode = 0;
                      windowNumber = 1;
                      int cnt = windowSelect.getItemCount() - 1;
                      if (cnt == 0) {
                         windowNumber = 1;
                      }
                      for (int k=0; k<cnt; k++) {
                          WindowItem item = (WindowItem) windowSelect.getItemAt(k+1);
                          if ((item.windowNumber > 1) && (k == 0)) {
                             windowNumber = 1;
                             break;
                          }
                          WindowItem itemNext = (WindowItem) windowSelect.getItemAt(k+2);
                          windowNumber = item.windowNumber+1;
                          if (itemNext != null) {
                             if ((itemNext.windowNumber - item.windowNumber) > 1) {
                                break;
                             }
                          }
                      }
                   }
                   else {
                      String action = (String) actionType.getSelectedItem();
                      if (action.equals("Replace")) {
                         mode = 1;
                      } 
                      else if (action.equals("Overlay")) {
                         mode = 2;
                      }
                      
                      if (hydra != null && hydra.multiDisplay) {
                          int num = windowSelect.getItemCount();
                          WindowItem item = (WindowItem) windowSelect.getItemAt(num-1);
                          windowNumber = item.windowNumber+1;
                      }
                   }
                   
                   // check if a selected leaf node references an instance of Compute
                   if (selectedLeafNode != null) {
                      LeafInfo leafInfo = (LeafInfo) selectedLeafNode.getUserObject();
                      Object source = leafInfo.source;
                      if (source instanceof Compute) {
                         try {
                            Compute compute = (Compute) source;
                            compute.createDisplay(compute.compute(), mode, windowNumber);
                            imageCreated = true;
                         }
                         catch (Exception e) {
                            e.printStackTrace();
                         }
                      }
                      else {
                         imageCreated = hydra.createImageDisplay(mode, windowNumber);
                      }
                   }

                   return "done";
                }

                protected void done() {
                   setCursorToDefault();

                   if (!imageCreated) {
                      return;
                   }

                   if ((mode == 0) || (hydra != null && hydra.multiDisplay)) {
                      if (hydra != null && !hydra.multiDisplay) {
                         if (!windowSelect.isEnabled()) {
                            windowSelect.setEnabled(true);
                            actionType.setEnabled(true);
                         }
                      }
                      else if (hydra == null) {
                         windowSelect.setEnabled(true);
                         actionType.setEnabled(true);
                      }
                      WindowItem item = new WindowItem();
                      item.windowNumber = windowNumber;
                      windowSelect.insertItemAt(item, windowNumber);
                      windowSelect.setSelectedItem(item);
                   }
                }
            }

            (new Task()).execute();
         }

      });

      actionPanel.add(displayButton);
      actionPanel.add(windowSelect);
      actionPanel.add(actionType);

      return actionPanel;
   }

   public DefaultMutableTreeNode getSelectedLeafNode() {
      return selectedLeafNode;
   }

   public void setCursorToWait() {
      frame.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
   }
  
   public void setCursorToDefault() {
      frame.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
   }

   public static void main(String[] args) throws Exception {
      
      FileSystemView fsv = FileSystemView.getFileSystemView();
      File homeDir = fsv.getHomeDirectory();
      
      try {
         PrintStream prntStrm = new PrintStream(new FileOutputStream(new File(homeDir, "hydraout.txt"), false));
         System.setOut(prntStrm);
         System.setErr(prntStrm);
      }
      catch (Exception e) { // Just in case we can't open the log file.
         e.printStackTrace();
         System.out.println("Could not open hydraout.txt in: "+homeDir);
      }
       
      previewDisplay = getPreviewDisplay();
      DataBrowser dataBrowser = new DataBrowser();

      dataBrowser.setCursorToWait();
      Hydra.initializeMapBoundaries();
      Hydra.initializeColorTables();
      previewDisplay.init();
      dataBrowser.setCursorToDefault();
   }

   class WindowItem {
       int windowNumber;
       public String toString() {
           return "Window #"+windowNumber;
       }
   }

}
