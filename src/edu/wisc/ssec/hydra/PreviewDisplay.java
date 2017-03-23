package edu.wisc.ssec.hydra;

import edu.wisc.ssec.adapter.SubsetRubberBandBox;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.HydraContext;

import ucar.unidata.view.geoloc.MapProjectionDisplayJ3D;
import ucar.unidata.view.geoloc.MapProjectionDisplay;

import ucar.visad.display.DisplayMaster;
import ucar.visad.display.LineDrawing;

import visad.*;

import java.rmi.RemoteException;

import java.util.HashMap;

import java.awt.Color;

import java.awt.BorderLayout;

import javax.swing.JComponent;

import javax.swing.JPanel;

public class PreviewDisplay {

   MapProjectionDisplayJ3D mapProjDsp;

   DisplayMaster dspMaster;

   LineDrawing boxDsp = null;

   LineDrawing outlineDsp = null;

   HydraRGBDisplayable imageDsp;

   CoordinateSystem displayCS = null;

   HydraContext hydraContext;

   float[][] clrTbl = Hydra.invGrayTable.getTable();

   SubsetRubberBandBox rbb;

   boolean isLL;

   FlatField image;

   double[] x_coords = new double[2];
   double[] y_coords = new double[2];

   boolean hasSubset = true;

   boolean previewVisible = true;

   HashMap<PreviewSelection, SubsetRubberBandBox> rbb_s = new HashMap<PreviewSelection, SubsetRubberBandBox>();

   public PreviewDisplay() throws VisADException, RemoteException {
       this.mapProjDsp = new MapProjectionDisplayJ3D(MapProjectionDisplay.MODE_2Din3D);
       this.mapProjDsp.enableRubberBanding(false);
       this.dspMaster = mapProjDsp;

       // region outline box
       outlineDsp = new LineDrawing("outline");
       outlineDsp.setColor(Color.white);
       outlineDsp.setLineWidth(1.25f);

       // current region selection box
       boxDsp = new LineDrawing("selectBox");
       boxDsp.setColor(Color.green);
       boxDsp.setLineWidth(1.25f);

       clrTbl = new float[][] {clrTbl[0], clrTbl[1], clrTbl[2], new float[256]};
       imageDsp = new HydraRGBDisplayable("image", RealType.Generic, null, clrTbl, true, null);
       imageDsp.setUseFastRendering(true);
       imageDsp.addConstantMap(new ConstantMap(0.0, Display.RenderOrderPriority));
   }

   public void init() throws VisADException, RemoteException {
       dspMaster.setDisplayInactive();
       dspMaster.addDisplayable(imageDsp);
       Hydra.addBaseMap(mapProjDsp);
       dspMaster.addDisplayable(outlineDsp);
       dspMaster.addDisplayable(boxDsp);
       dspMaster.setDisplayActive();
   }

   public void updateFrom(PreviewSelection previewSelect) throws VisADException, RemoteException {
      if (previewSelect == null) {
         imageDsp.setVisible(false);
         outlineDsp.setVisible(false);
         previewVisible = false;
         return;
      }
      mapProjDsp.setDisplayInactive();

      if (!previewVisible) {
         imageDsp.setVisible(true);
         outlineDsp.setVisible(true);
         previewVisible = true;
      }

      mapProjDsp.setMapProjection(previewSelect.sampleProjection);
      displayCS = mapProjDsp.getDisplayCoordinateSystem();
      this.image = previewSelect.image;
      this.isLL = previewSelect.isLL;
      hydraContext = previewSelect.getHydraContext();

      double min = previewSelect.imageRange.getMin();
      double max = previewSelect.imageRange.getMax();

      imageDsp.setRange(min,max);

      FunctionType ftype = (FunctionType) image.getType();
      FunctionType nftype = new FunctionType(ftype.getDomain(), RealType.Generic);
      FlatField newImage = new FlatField(nftype, image.getDomainSet());
      newImage.setSamples(image.getFloats());
      imageDsp.setData(newImage);
      imageDsp.setColorPalette(previewSelect.clrTbl);

      outlineDsp.setData(previewSelect.boxOutline);

      rbb = rbb_s.get(previewSelect);
      if (rbb != null) {
         dspMaster.removeDisplayable(rbb);
         rbb = makeSubsetRubberBandBox();
         dspMaster.addDisplayable(rbb);
      }
      else {
        rbb = makeSubsetRubberBandBox();
        dspMaster.addDisplayable(rbb);
        rbb_s.put(previewSelect, rbb);
      }

      mapProjDsp.setDisplayActive();
   }

   SubsetRubberBandBox makeSubsetRubberBandBox() throws VisADException, RemoteException {
        final SubsetRubberBandBox rbb =
            new SubsetRubberBandBox(image, ((MapProjectionDisplay)mapProjDsp).getDisplayCoordinateSystem(), 0, false);
        rbb.setColor(Color.green);
        rbb.addAction(new CellImpl() {
           boolean init = false;
           public void doAction()
                throws VisADException, RemoteException
           {
             if (!init) {
               init = true;
               return;
             }
             Gridded2DSet set = (Gridded2DSet) rbb.getData();
             float[] low = set.getLow();
             float[] hi = set.getHi();

             if (low[0] == hi[0] && low[1] == hi[1]) { // Don't update for a single point
                return;
             }
             // Negative numbers not allowed.
             if (low[0] < 0) low[0] = 0;
             if (low[1] < 0) low[1] = 0;

             x_coords[0] = low[0];
             x_coords[1] = hi[0];

             y_coords[0] = low[1];
             y_coords[1] = hi[1];

             if (hasSubset) {
               MultiDimensionSubset select = hydraContext.getMultiDimensionSubset();
               HashMap map = select.getSubset();

               double[] coords0 = (double[]) map.get("Track");
               if (coords0 == null) {
                 coords0 = (double[]) map.get("GridY");
               }
               coords0[0] = y_coords[0];
               coords0[1] = y_coords[1];
               coords0[2] = 1;

               double[] coords1 = (double[]) map.get("XTrack");
               if (coords1 == null) {
                 coords1 = (double[]) map.get("GridX");
               }
               coords1[0] = x_coords[0];
               coords1[1] = x_coords[1];
               coords1[2] = 1;

               Gridded3DSet set3D = rbb.getLastBox();
               float[][] samples = set3D.getSamples(false);
               Gridded2DSet set2D =
                    new Gridded2DSet(RealTupleType.SpatialCartesian2DTuple,
                                    new float[][] {samples[0], samples[1]}, samples[0].length);

               float[][] latlon = displayCS.fromReference(new float[][] {samples[0], samples[1]});
               Gridded2DSet set2D_lonlat =
                    new Gridded2DSet(RealTupleType.SpatialEarth2DTuple,
                        new float[][] {latlon[1], latlon[0]}, latlon[0].length);

               boxDsp.setData(set2D);

               hydraContext.setMultiDimensionSubset(new MultiDimensionSubset(map));
               hydraContext.setSelectBox(new visad.Tuple(new Data[] {set2D, set2D_lonlat}));
               HydraContext.setLastManual(hydraContext);
               Hydra.resetSharedProjection();
             }
           }
        });

        return rbb;
      }

      public JComponent doMakeContents() {
        try {
          JPanel panel = new JPanel(new BorderLayout());
          panel.add("Center", dspMaster.getDisplayComponent());
          return panel;
        }
        catch (Exception e) {
          System.out.println(e);
        }
        return null;
      }

      public void setBox(Gridded2DSet box) throws VisADException, RemoteException {
         boxDsp.setData(box);
      }

      public void draw() throws VisADException, RemoteException {
         dspMaster.draw();
      }
}
