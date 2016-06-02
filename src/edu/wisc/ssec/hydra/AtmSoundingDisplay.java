package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.AtmSoundingDataSource;
import java.awt.Color;
import java.awt.Component;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JComboBox;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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
import visad.VisADException;
import visad.bom.RubberBandBoxRendererJ3D;
import visad.java3d.DefaultRendererJ3D;

import edu.wisc.ssec.hydra.data.DataChoice;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.XYDisplay;

import edu.wisc.ssec.adapter.GrabLineRendererJ3D;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.SwathSoundingData;

public class AtmSoundingDisplay implements DisplayListener {

    private static final String DISP_NAME = "Retrieval";
    private static int cnt = 1;

    private DataChoice dataChoice;

    private float[] initialRangeX;
    private float[] initialRangeY = { 180f, 320f };

    private RealType domainType;
    private RealType rangeType;
    private RealType uniqueRangeType;

    private ScalarMap xmap;
    private ScalarMap ymap;

    private LocalDisplay display;

    private FlatField image;

    private FlatField spectrum = null;
    
    private DataReference spectrumRef;

    private boolean imageExpired = true;

    private SwathSoundingData data;

    private float waveNumber;

    private List<DataReference> displayedThings = new ArrayList<DataReference>();
    private HashMap<String, DataReference> idToRef = new HashMap<String, DataReference>();
    private HashMap<DataReference, ConstantMap[]> colorMaps = 
        new HashMap<DataReference, ConstantMap[]>();

    private DisplayableData imageDisplay = null;

    private XYDisplay master;

    private Gridded1DSet domainSet;

    private JComboBox bandSelectComboBox = null;

    private PropertyChangeListener listener = null;

    // From the incoming dataChoice, fixed for this instance
    private HashMap subset;


    public AtmSoundingDisplay(final DataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        this.dataChoice = dataChoice;
        init();
    }

    // TODO: generalize this so that you can grab the image data for any
    // channel
    public FlatField getImageData() {
        try {
           //  check if subset has changed in the DataChoice
              MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
              HashMap subset = select.getSubset();
              image = data.getImage(waveNumber, subset);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return image;
    }

    public FlatField getImageDataFrom(final float channel) {
        FlatField imageData = null;
        try {
           // use initial subset from the init method
           imageData = data.getImage(channel, subset);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return imageData;
    }

    public LocalDisplay getDisplay() {
        return display;
    }

    public Component getDisplayComponent() {
      return master.getDisplayComponent();
    }

    public RealType getDomainType() {
        return domainType;
    }

    public RealType getRangeType() {
        return rangeType;
    }

    public SwathSoundingData getSwathSoundingData() {
        return data;
    }

    public Gridded1DSet getDomainSet() {
        return domainSet;
    }

    private void init() throws VisADException, RemoteException {

        MultiDimensionSubset select = (MultiDimensionSubset) dataChoice.getDataSelection();
        subset = select.getSubset();
    	
        /* get SwathSoundingData from ctr?
        */
        data = ((AtmSoundingDataSource)dataChoice.getDataSource()).getSwathSoundingData(dataChoice);

        waveNumber = data.init_level;

        try {
            spectrum = data.getSounding(new int[] { 1, 1 });
        } catch (Exception e) {
            e.printStackTrace();
        }

        domainSet = (Gridded1DSet)spectrum.getDomainSet();
        initialRangeX = getXRange(domainSet);
        initialRangeY = data.getDataRange();

        domainType = getDomainType(spectrum);
        rangeType = getRangeType(spectrum);

        master = new XYDisplay(DISP_NAME, domainType, rangeType);

        // set up the x- and y-axis
        xmap = new ScalarMap(rangeType, Display.XAxis);
        ymap = new ScalarMap(domainType, Display.YAxis);

        xmap.setRange(initialRangeY[0], initialRangeY[1]);
        ymap.setRange(initialRangeX[1], initialRangeX[0]);

        display = master.getDisplay();
        display.addMap(xmap);
        display.addMap(ymap);
        display.addDisplayListener(this);

        AxisScale xAxis = xmap.getAxisScale();
        AxisScale yAxis = ymap.getAxisScale();
        xAxis.setLabelSize(30);
        yAxis.setLabelSize(30);
        xAxis.setSnapToBox(true);
        yAxis.setSnapToBox(true);

        setDisplayMasterAttributes(master);

        new RubberBandBox(this, xmap, ymap);

        spectrumRef = new DataReferenceImpl("spectrumRef_"+Hydra.getUniqueID());
        addRef(spectrumRef, Color.WHITE);
    }

    public JComboBox getBandSelectComboBox() {
      return bandSelectComboBox;
    }

    public void displayChanged(final DisplayEvent e) throws VisADException, RemoteException {
        // TODO: write a method like isChannelUpdate(EVENT_ID)? or maybe just 
        // deal with a super long if-statement and put an "OR MOUSE_RELEASED" 
        // up here?
        if (e.getId() == DisplayEvent.MOUSE_RELEASED_CENTER) {
        }
        else if (e.getId() == DisplayEvent.MOUSE_PRESSED_LEFT) {
            if (e.getInputEvent().isControlDown()) {
                xmap.setRange(initialRangeX[0], initialRangeX[1]);
                ymap.setRange(initialRangeY[0], initialRangeY[1]);
            }
        }
        else if (e.getId() == DisplayEvent.MOUSE_RELEASED) {
            float val = getSelectorValue(channelSelector);
            if (val != waveNumber) {
              waveNumber = val;
              notifyListener(val);
            }
        }
    }

    private void notifyListener(float val) {
      if (listener != null) {
         listener.propertyChange(new PropertyChangeEvent(this, "wavenumber", null, new Float(val)));
      }
    }

    public void setListener(PropertyChangeListener listener) {
       this.listener = listener;
    }

    public DataReference getSpectrumRef() {
       return displayedThings.get(0);
    }

    public float getWaveNumber() {
        return waveNumber;
    }

    public int getChannelIndex() throws Exception {
      return data.getLevelIndexFromLevel(waveNumber);
    }

    public void refreshDisplay() throws VisADException, RemoteException {
        if (display == null)
            return;

        synchronized (displayedThings) {
            for (DataReference ref : displayedThings) {
                display.removeReference(ref);
                display.addReference(ref, colorMaps.get(ref));
            }
        }
    }
    
    public void updateRef(final DataReference thing, final Color color)
        throws VisADException, RemoteException 
    {
        ConstantMap[] colorMap = makeColorMap(color);
        ConstantMap[] constMaps = colorMap;
        ConstantMap[] newConstMaps = new ConstantMap[constMaps.length+1];
        System.arraycopy(constMaps, 0, newConstMaps, 0, constMaps.length);
        newConstMaps[newConstMaps.length-1] = new ConstantMap(2f, Display.LineWidth);
        constMaps = newConstMaps;
        
        colorMaps.put(thing, constMaps);
        idToRef.put(thing.getName(), thing);
        refreshDisplay();
    }

    public boolean hasNullData() {
        try {
            synchronized (displayedThings) {
                for (DataReference ref : displayedThings) {
                    if (ref.getData() == null)
                        return true;
                }
            }
        } catch (Exception e) { }
        return false;
    }

    /** ID of the selector that controls the displayed channel. */
    private final String channelSelector = "chanSelect_"+Hydra.getUniqueID();

    /** The map of selector IDs to selectors. */
    private final Map<String, DragLine> selectors = 
        new HashMap<String, DragLine>();

    public void showChannelSelector() {
        try {
            createSelector(channelSelector, Color.GREEN);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void hideChannelSelector() {
        try {
            DragLine selector = removeSelector(channelSelector);
            selector = null;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public DragLine createSelector(final String id, final Color color) throws Exception {
        if (id == null)
            throw new NullPointerException("selector id cannot be null");
        if (color == null)
            throw new NullPointerException("selector color cannot be null");
        return createSelector(id, makeColorMap(color));
    }

    public DragLine createSelector(final String id, final Color color, float xval) throws Exception {
        if (id == null)
            throw new NullPointerException("selector id cannot be null");
        if (color == null)
            throw new NullPointerException("selector color cannot be null");
        return createSelector(id, makeColorMap(color), xval);
    }


    public DragLine createSelector(final String id, final ConstantMap[] color) throws Exception {
        if (id == null)
            throw new NullPointerException("selector id cannot be null");
        if (color == null)
            throw new NullPointerException("selector color cannot be null");

        if (selectors.containsKey(id))
            return selectors.get(id);

        DragLine selector = new DragLine(this, id, color, initialRangeY, Float.NaN);
        //selector.setSelectedValue(waveNumber);
        selectors.put(id, selector);
        return selector;
    }

    public DragLine createSelector(final String id, final ConstantMap[] color, float xval) throws Exception {
        if (id == null)
            throw new NullPointerException("selector id cannot be null");
        if (color == null)
            throw new NullPointerException("selector color cannot be null");

        if (selectors.containsKey(id))
            return selectors.get(id);

        DragLine selector = new DragLine(this, id, color, initialRangeY, xval);
        selectors.put(id, selector);
        return selector;
    }


    public DragLine getSelector(final String id) {
        return selectors.get(id);
    }

    public float getSelectorValue(final String id) 
        throws VisADException, RemoteException {
        DragLine selector = selectors.get(id);
        if (selector == null)
            return Float.NaN;
        return selector.getSelectedValue();
    }

    public void setSelectorValue(final String id, final float value) 
        throws VisADException, RemoteException 
    {
        DragLine selector = selectors.get(id);
        if (selector != null)
            selector.setSelectedValue(value);
    }

    public void setSelectorValue(final float value)
        throws VisADException, RemoteException
    {
        DragLine selector = selectors.get(channelSelector);
        if (selector != null) {
            selector.setSelectedValue(value);
        }
    }

    public DragLine removeSelector(final String id) {
        DragLine selector = selectors.remove(id);
        if (selector == null)
            return null;
        selector.annihilate();
        return selector;
    }

    public void setSelectorVisible(final String id, final boolean visible) {
        DragLine selector = selectors.get(id);
        if (selector != null)
            selector.setVisible(visible);
    }

    public List<DragLine> getSelectors() {
        return new ArrayList<DragLine>(selectors.values());
    }

    public void addSelectorListener(final String id, PropertyChangeListener listener) {
       selectors.get(id).setListener(listener);
    }

    public void addRef(final DataReference thing, final Color color) 
        throws VisADException, RemoteException 
    {
        if (display == null)
            return;

        synchronized (displayedThings) {
            ConstantMap[] colorMap = makeColorMap(color);

            displayedThings.add(thing);
            idToRef.put(thing.getName(), thing);
            ConstantMap[] constMaps;
            constMaps = colorMap;
            colorMaps.put(thing, constMaps);
            
            ConstantMap[] newConstMaps = new ConstantMap[constMaps.length+1];
            System.arraycopy(constMaps, 0, newConstMaps, 0, constMaps.length);
            newConstMaps[newConstMaps.length-1] = new ConstantMap(2f, Display.LineWidth);
            constMaps = newConstMaps;
            
            colorMaps.put(thing, constMaps);

            display.addReference(thing, newConstMaps);
        }
    }

    public boolean setWaveNumber(float val) {
       if (waveNumber == val) {
          return true;
       }
       
       try {
            int[] idx = domainSet.valueToIndex(new float[][] {{val}});
            if (idx[0] >= 0) {
               float[][] tmp = domainSet.indexToValue(idx);
               waveNumber = tmp[0][0];
               setSelectorValue(waveNumber);
            }
            else {
               return false;
            }
       } catch (Exception e) {
            e.printStackTrace();
            return false;
       }

       return true;
    }
    
    public void setBackground(Color color) throws VisADException, RemoteException {
       master.setBackground(color);
       if (color.equals(Color.white)) {
          master.setBackground(new Color(236, 236, 236));
          master.setForeground(Color.black);
          updateRef(spectrumRef, Color.black);
       }
       else if (color.equals(Color.black)) {
          master.setForeground(Color.white);
          updateRef(spectrumRef, Color.white);
       }
    }

    /**
     * @return The ConstantMap representation of <code>color</code>.
     */
    public static ConstantMap[] makeColorMap(final Color color)
        throws VisADException, RemoteException 
    {
        float r = color.getRed() / 255f;
        float g = color.getGreen() / 255f;
        float b = color.getBlue() / 255f;
        float a = color.getAlpha() / 255f;
        return new ConstantMap[] { new ConstantMap(r, Display.Red),
                                   new ConstantMap(g, Display.Green),
                                   new ConstantMap(b, Display.Blue),
                                   new ConstantMap(a, Display.Alpha) };
    }

    /**
     * Provides <code>master</code> some sensible default attributes.
     */
    private static void setDisplayMasterAttributes(final XYDisplay master) 
        throws VisADException, RemoteException 
    {
        master.showAxisScales(true);
        master.setAspect(1.4, 2.0);
        
        double[] proj = master.getProjectionMatrix();
        proj[0] = 0.46;
        proj[5] = 0.46;
        proj[10] = 0.46;
        proj[3] = 0.12;
        proj[7] = 0.04;        

        master.setProjectionMatrix(proj);
    }

    /**
     * @return The minimum and maximum values found on the x-axis.
     */
    private static float[] getXRange(final Gridded1DSet domain) {
        return new float[] { domain.getLow()[0], domain.getHi()[0] };
    }

    public static RealType getRangeType(final FlatField spectrum) {
        return (((FunctionType)spectrum.getType()).getFlatRange().getRealComponents())[0];
    }

    private static RealType getDomainType(final FlatField spectrum) {
        return (((FunctionType)spectrum.getType()).getDomain().getRealComponents())[0];
    }

    private static class RubberBandBox extends CellImpl {

        private static final String RBB = "rubberband_";
        
        private DataReference rubberBand;

        private boolean init = false;

        private ScalarMap xmap;

        private ScalarMap ymap;

        public RubberBandBox(final AtmSoundingDisplay msd,
            final ScalarMap x, final ScalarMap y) throws VisADException,
            RemoteException 
        {
            RealType domainType = msd.getDomainType();
            RealType rangeType = msd.getRangeType();

            LocalDisplay display = msd.getDisplay();

            rubberBand = new DataReferenceImpl(RBB+Hydra.getUniqueID());
            rubberBand.setData(new RealTuple(new RealTupleType(domainType,
                rangeType), new double[] { Double.NaN, Double.NaN }));

            display.addReferences(new RubberBandBoxRendererJ3D(domainType,
                rangeType, 1, 1), new DataReference[] { rubberBand }, null);

            xmap = x;
            ymap = y;

            this.addReference(rubberBand);
        }

        public void doAction() throws VisADException, RemoteException {
            if (!init) {
                init = true;
                return;
            }

            Gridded2DSet set = (Gridded2DSet)rubberBand.getData();

            float[] low = set.getLow();
            float[] high = set.getHi();

            xmap.setRange(low[0], high[0]);
            ymap.setRange(low[1], high[1]);
        }
    }

    public static class DragLine extends CellImpl {
        private final String selectorId = "selector_"+Hydra.getUniqueID();
        private final String lineId = "line_"+Hydra.getUniqueID();
        private final String controlId;

        private ConstantMap[] mappings = new ConstantMap[5];

        private DataReference line;

        private DataReference selector;

        private AtmSoundingDisplay multiSpectralDisplay;

        private RealType domainType;
        private RealType rangeType;

        private RealTupleType tupleType;

        private LocalDisplay display;

        private float[] YRANGE;

        private float lastSelectedValue;

        private DataRenderer lineRenderer;

        private DataRenderer grabRenderer;

        private PropertyChangeListener listener = null;

        private boolean init = false;

        public DragLine(final AtmSoundingDisplay msd, final String controlId, final Color color) throws Exception {
            this(msd, controlId, makeColorMap(color));
        }

        public DragLine(final AtmSoundingDisplay msd, final String controlId, final Color color, float[] YRANGE) throws Exception {
            this(msd, controlId, makeColorMap(color), YRANGE, Float.NaN);
        }

        public DragLine(final AtmSoundingDisplay msd, final String controlId,
            final ConstantMap[] color) throws Exception
        {
            this(msd, controlId, color, new float[] {180f, 320f}, Float.NaN);
        }

        public DragLine(final AtmSoundingDisplay msd, final String controlId, 
            final ConstantMap[] color, float[] YRANGE, float XVALUE) throws Exception 
        {
            if (msd == null)
                throw new NullPointerException("must provide a non-null MultiSpectralDisplay");
            if (controlId == null)
                throw new NullPointerException("must provide a non-null control ID");
            if (color == null)
                throw new NullPointerException("must provide a non-null color");

            this.controlId = controlId;
            this.multiSpectralDisplay = msd;
            this.YRANGE = YRANGE;
            
            if (Float.isNaN(XVALUE)) {
              lastSelectedValue = multiSpectralDisplay.getWaveNumber();
            } else {
              lastSelectedValue = XVALUE;
            }
            

            for (int i = 0; i < color.length; i++) {
                mappings[i] = (ConstantMap)color[i].clone();
            }
            mappings[4] = new ConstantMap(-0.5, Display.YAxis);

            Gridded1DSet domain = multiSpectralDisplay.getDomainSet();

            domainType = multiSpectralDisplay.getDomainType();
            rangeType = multiSpectralDisplay.getRangeType();
            tupleType = new RealTupleType(domainType, rangeType);

            selector = new DataReferenceImpl(selectorId);
            selector.setData(new Real(domainType, lastSelectedValue));

            line = new DataReferenceImpl(lineId);
            line.setData(new Gridded2DSet(tupleType,
                new float[][] { { lastSelectedValue, lastSelectedValue }, { YRANGE[0], YRANGE[1] } }, 2));


            display = multiSpectralDisplay.getDisplay();
            
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

        public String getControlId() {
            return controlId;
        }

        /**
         * Handles drag and drop updates.
         */
        public void doAction() throws VisADException, RemoteException {
            if (!init) {
                init = true;
                return;
            }
            float val = getSelectedValue();

            // move the line
            line.setData(new Gridded2DSet(tupleType,
                new float[][] { { val, val }, { YRANGE[0], YRANGE[1] } }, 2));
            lastSelectedValue = val;

            if (listener != null) { // notify THE listener
                listener.propertyChange(new PropertyChangeEvent(this, "wavenumber", null, new Float(lastSelectedValue)));
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
            // let doAction move the line as if an event from dragging the selector
            selector.setData(new Real(domainType, val));
        }

        public void setListener(PropertyChangeListener listener) {
           this.listener = listener;
        }
    }
}
