package edu.wisc.ssec.hydra;

import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JComponent;
import javax.swing.ImageIcon;
import javax.swing.SwingUtilities;
import javax.swing.JDialog;
import javax.swing.JCheckBox;
import javax.swing.border.LineBorder;
import java.awt.FlowLayout;
import java.awt.Color;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.DisplayableData;
import visad.VisADException;
import java.rmi.RemoteException;


public class Depiction {

   ImageDisplay imageDisplay;
   DisplayMaster dspMaster;
   DepictionControl clrCntrl;
   DisplayableData imageDsp;
   String name;
   String longName;
   String dateTimeStr;
   String tooltip = "options for this image";
   JComponent parentComponent;
   final javax.swing.JLabel show = new javax.swing.JLabel();
   final javax.swing.JLabel remove = new javax.swing.JLabel();
   final ImageIcon delIc = new ImageIcon(getClass().getResource("/resources/icons/delete.png"));

   JDialog dialog = null;

   JComponent component;

   // This depiction can not be removed by the user
   boolean isRemovable = true;

   boolean isMask = false;

   JCheckBox visToggle = new JCheckBox();
   LineBorder blackBorder3 = new LineBorder(Color.black,3); // visible
   LineBorder blackBorder = new LineBorder(Color.black); // not visible

   boolean visibleOK = true;

   boolean isVisible = true;
   

   Depiction(DisplayMaster dspMaster, DisplayableData imageDsp, String name, boolean isRemovable, boolean isMask) {
      this(dspMaster, imageDsp, null, name, isRemovable, isMask);
   }

   Depiction(DisplayMaster dspMaster, DisplayableData imageDsp, DepictionControl clrCntrl, String name, boolean isRemovable, boolean isMask) {
     this.dspMaster = dspMaster;
     this.clrCntrl = clrCntrl;
     this.imageDsp = imageDsp;
     this.name = name;
     this.isRemovable = isRemovable;
     this.isMask = isMask;
     if (clrCntrl != null) { // Not all Depictions have controls.
        clrCntrl.setDepiction(this);
     }
   }

   Depiction(DisplayMaster dspMaster, DisplayableData imageDsp, DepictionControl clrCntrl, String name, boolean isRemovable) {
     this(dspMaster, imageDsp, clrCntrl, name, isRemovable, false);
   }

   Depiction(DisplayMaster dspMaster, DisplayableData imageDsp, DepictionControl clrCntrl, String name) {
     this(dspMaster, imageDsp, clrCntrl, name, true, false);
   }

   public JComponent doMakeComponent() {
       JPanel panel = new JPanel(new FlowLayout());

       visToggle = new javax.swing.JCheckBox();
       visToggle.setSelected(true);
       visToggle.setToolTipText("toggle visibility");
       visToggle.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent e) {
                if (e.getStateChange() == ItemEvent.DESELECTED) {
                   visibleOK = false;
                   toggleVisible(false);
                } else {
                   visibleOK = true;
                   toggleVisible(true);
                }
            }
          }
       );
       panel.add(visToggle);

       show.setToolTipText(tooltip);
       show.setBorder(blackBorder3);
       show.setText(name);
       panel.add(show);

       if (clrCntrl != null) {
          dialog = new javax.swing.JDialog();
          dialog.setContentPane(clrCntrl.doMakeContents());
          dialog.validate();

          show.addMouseListener(new java.awt.event.MouseAdapter() {
               public void mouseClicked(java.awt.event.MouseEvent e) {
                  dialog.setLocation(show.getLocationOnScreen());
                  dialog.setVisible(true);
                  dialog.setSize(dialog.getPreferredSize());
               }
             }
          );
       }

       remove.setToolTipText("remove from display");
       remove.setIcon(delIc);
       remove.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent e) {
               remove();
            }
          }
       );
       if (isRemovable) {
          panel.add(remove);
       }

       component = panel;
       return panel;
   }

   public void setName(String newName) {
     this.name = newName;
     this.show.setText(name);
   }

   public void setLongName(String longName) {
      this.longName = longName;
   }

   public String getLongName() {
      return longName;
   }

   public String getName() {
     return name;
   }
   
   public void setDateTimeStr(String dateTime) {
       this.dateTimeStr = dateTime;
   }
   
   public String getDateTimeStr() {
       return dateTimeStr;
   }

   public void setPopupName(String popupName) {
      tooltip = popupName;
   }

   public void setParentComponent(JComponent parent) {
      parentComponent = parent;
   }

   public void remove() {
      try {
         dspMaster.removeDisplayable(imageDsp);
         Hydra.displayableToImage.remove(imageDsp);
         imageDisplay.removeDepiction(this);
         parentComponent.remove(component);
         SwingUtilities.getWindowAncestor(parentComponent).validate();
         if (dialog != null) {
            dialog.setVisible(false);
         }
      }
      catch (VisADException e) {
         e.printStackTrace();
      }
      catch (RemoteException e) {
         e.printStackTrace();
      }

      destroy();
   }

   public DisplayableData getDisplayableData() {
      return this.imageDsp;
   }

   public void setPropertyChangeListener(ImageDisplay imageDisplay) {
      this.imageDisplay = imageDisplay;
   }

   public void setVisible(boolean visible) {
      if (isVisible == visible) return; // nothing to do

      isVisible = visible;

      if (visible) {
         show.setBorder(blackBorder3);
      }
      else {
         show.setBorder(blackBorder);
      }
      try {
          imageDsp.setVisible(visible);
      }
      catch (Exception e) {
          e.printStackTrace();
      }
   }

   public boolean getVisible() {
      return isVisible;
   }

   private void toggleVisible(boolean on) {
      try {
         if (imageDisplay != null) {
            imageDisplay.depictionVisibilityChanged(on, this);
         }
      }
      catch (Exception e) {
         e.printStackTrace();
      }
   }

   public boolean getVisibleOK() {
      return visibleOK;
   }
   
   public DepictionControl getDepictionControl() {
       return clrCntrl;
   }

   public void setDialogVisible(boolean on) {
      if (dialog != null) {
         dialog.setVisible(on);
      }
   }

   public void destroy() {
      if (clrCntrl != null) {
         clrCntrl.destroy();
      }
      Hydra.displayableToImage.remove(imageDsp);
      imageDisplay = null;
      if (dialog != null) {
         dialog.dispose();
      }
   }
}
