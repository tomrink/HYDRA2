package edu.wisc.ssec.hydra;

import visad.AxisScale;
import visad.CellImpl;
import visad.ConstantMap;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.DataRenderer;
import visad.Display;
import visad.DisplayEvent;
import visad.DisplayListener;
import visad.FlatField;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.Gridded2DSet;
import visad.LocalDisplay;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.ScalarMap;
import visad.ScalarMapListener;
import visad.ScalarMapControlEvent;
import visad.ScalarMapEvent;
import visad.VisADException;
import visad.bom.RubberBandBoxRendererJ3D;
import visad.java3d.DefaultRendererJ3D;

import ucar.unidata.data.DirectDataChoice;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.XYDisplay;

import edu.wisc.ssec.adapter.GrabLineRendererJ3D;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import java.rmi.RemoteException;

import java.awt.Color;

import java.util.ArrayList;


public class DragLine extends CellImpl {
        private final String selectorId = "selector_"+Hydra.getUniqueID();
        private final String lineId = "line_"+Hydra.getUniqueID();

        private ConstantMap[] mappings = new ConstantMap[5];

        private DataReference line;

        private DataReference selector;

        private MultiSpectralDisplay multiSpectralDisplay;

        private RealType domainType;
        private RealType rangeType;

        private RealTupleType tupleType;

        private LocalDisplay display;

        private float[] YRANGE;

        private float lastSelectedValue;

        private DataRenderer lineRenderer;

        private GrabLineRendererJ3D grabRenderer;

        private PropertyChangeListener listener = null;
        
        private ArrayList<PropertyChangeListener> listeners = new ArrayList<PropertyChangeListener>();

        private boolean init = false;
        
        private ScalarMap xmap;
        
        private boolean noChange = false;
        

        public DragLine(LocalDisplay display, Gridded1DSet domain, RealType domainType, RealType rangeType,
            final ConstantMap[] color, float[] yrange, float XVALUE, final ScalarMap xmap) throws Exception 
        {
            if (color == null)
                throw new NullPointerException("must provide a non-null color");

            this.YRANGE = yrange;
            this.xmap = xmap;
            this.domainType = domainType;
            
            XVALUE = (float) 0.1*(domain.getHi()[0]);
            lastSelectedValue = XVALUE;
            

            for (int i = 0; i < color.length; i++) {
                mappings[i] = (ConstantMap)color[i].clone();
            }
            mappings[4] = new ConstantMap(-0.5, Display.YAxis);

            tupleType = new RealTupleType(RealType.XAxis, RealType.YAxis);

            selector = new DataReferenceImpl(selectorId);
            selector.setData(new Real(domainType, lastSelectedValue));

            line = new DataReferenceImpl(lineId);
            
            float xval = (float) (-2.5 + 0.1*5) ;
            
            
            line.setData(new Gridded2DSet(new RealTupleType(RealType.XAxis, RealType.YAxis),
                new float[][] {{xval, xval}, {-0.75f, 0.75f}}, 2));


            grabRenderer = new GrabLineRendererJ3D(domain);
            display.addReferences(grabRenderer, new DataReference[] { selector }, new ConstantMap[][] { mappings });
            
            lineRenderer = new DefaultRendererJ3D();
            display.addReferences(lineRenderer, line, cloneMappedColor(color));

            addReference(selector);
            
        }

        private static ConstantMap[] cloneMappedColor(final ConstantMap[] color) throws Exception {
            assert color != null && color.length >= 3 : color;
            return new ConstantMap[] { 
                (ConstantMap)color[0].clone(),
                (ConstantMap)color[1].clone(),
                (ConstantMap)color[2].clone(),
            };
        }

        public void annihilate() {
            try {
                display.removeReference(selector);
                display.removeReference(line);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        public void setVisible(boolean visible) {
            lineRenderer.toggle(visible);
            grabRenderer.toggle(visible);
        }

        /**
         * Handles drag and drop updates.
         */
        public void doAction() throws VisADException, RemoteException {
            if (!init) {
                init = true;
                return;
            }
            if (noChange) {
                noChange = false;
                return;
            }
            
            float val = getSelectedValue();
            if (val == lastSelectedValue) {
                return;
            }
            lastSelectedValue = val;
            
            val = (xmap.scaleValues(new float[] {lastSelectedValue}))[0];
            
            // move the line
            line.setData(new Gridded2DSet(tupleType,
                new float[][] { {val, val}, {-0.75f, 0.75f} }, 2));
            val = (val + 2.5f)/5f;
            
            for (int k=0; k<listeners.size(); k++) {
                listeners.get(k).propertyChange(new PropertyChangeEvent(this, "distmarker", null, new Float(val)));
            }
        }

        public float getSelectedValue() throws VisADException, RemoteException {
            float val = (float) ((Real)selector.getData()).getValue();
            if (Float.isNaN(val))
                val = lastSelectedValue;
            return val;
        }

        public void setSelectedValue(final float val) throws VisADException,
            RemoteException 
        {
            if (Float.isNaN(val)) return; 
            selector.setData(new Real(domainType, val));
        }
        
        public void updateSelector(double dist, Gridded1DSet dset) throws VisADException, RemoteException {
            grabRenderer.updateDomain(dset);
            
            noChange = true;
            selector.setData(new Real(domainType, dist));
        }

        public void addListener(PropertyChangeListener listener) {
           listeners.add(listener);
        }
        
        public void removeListener(PropertyChangeListener listener) {
           listeners.remove(listener);
        }
    }
