package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.NOAA_SNPP_DataSource;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
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

import ucar.unidata.data.DirectDataChoice;
import ucar.visad.display.DisplayableData;
import ucar.visad.display.XYDisplay;

import edu.wisc.ssec.adapter.GrabLineRendererJ3D;
import edu.wisc.ssec.adapter.MultiDimensionSubset;
import edu.wisc.ssec.adapter.MultiSpectralData;
import edu.wisc.ssec.adapter.MultiSpectralDataSource;
import visad.util.HersheyFont;

public class MultiSpectralDisplay implements DisplayListener {

    private static final String DISP_NAME = "Spectrum";
    private static int cnt = 1;

    private DirectDataChoice dataChoice;

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

    private MultiSpectralData data;

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

    private double aspect;
    private double scale;
    private double xmapScale;

    // From the incoming dataChoice, fixed for this instance
    private HashMap subset;
    
    public MultiChannelViewer processor = null;

    public MultiSpectralDisplay(final DirectDataChoice dataChoice) 
        throws VisADException, RemoteException 
    {
        this.dataChoice = dataChoice;
        init(Float.NaN, null, null);
    }

    public MultiSpectralDisplay(final DirectDataChoice dataChoice, float initWaveNumber) 
        throws VisADException, RemoteException
    {
        this.dataChoice = dataChoice;
        init(initWaveNumber, null, null);
    }

    public MultiSpectralDisplay(final DirectDataChoice dataChoice, float initWaveNumber, float[] xMapRange, float[] yMapRange)
        throws VisADException, RemoteException
    {
       this.dataChoice = dataChoice;
       init(initWaveNumber, xMapRange, yMapRange);
    }

    public FlatField getImageData() {
        try {
           // check if subset has changed in the dataChoice
              MultiDimensionSubset select = null;
              Hashtable table = dataChoice.getProperties();
              Enumeration keys = table.keys();
              while (keys.hasMoreElements()) {
                Object key = keys.nextElement();
                if (key instanceof MultiDimensionSubset) {
                  select = (MultiDimensionSubset) table.get(key);
                }
              }
              HashMap subset = select.getSubset();
              image = data.getImage(waveNumber, subset);
              image = processRange(image, waveNumber);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return image;
    }

    public FlatField getImageDataFrom(final float channel) {
        FlatField imageData = null;
        try {
           // Use the initial subset from the init() method
           imageData = data.getImage(channel, subset);
           imageData = processRange(imageData, channel);
        } catch (Exception e) {
           e.printStackTrace();
        }
        return imageData;
    }
    
    private FlatField processRange(FlatField fltFld, float value) {
        if (processor == null) {
            return fltFld;
        }
        else {
            return processor.processRange(fltFld, value);
        }
    }

    public LocalDisplay getDisplay() {
        return display;
    }
 
    public void setActive() throws VisADException, RemoteException {
       master.setDisplayActive();
    }

    public void setInactive() throws VisADException, RemoteException {
       master.setDisplayInactive();
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

    public MultiSpectralData getMultiSpectralData() {
        return data;
    }

    public Gridded1DSet getDomainSet() {
        return domainSet;
    }

    public float getXmapScale() {
        double[] so = new double[2];
        double[] dum1 = new double[2];
        double[] dum2 = new double[2];
        xmap.getScale(so, dum1, dum2);
        return (float) so[0];
    }

    public float getYmapScale() {
        double[] so = new double[2];
        double[] dum1 = new double[2];
        double[] dum2 = new double[2];
        ymap.getScale(so, dum1, dum2);
        return (float) so[0];
    }

    public float[] getXmapRange() {
       double[] rng = xmap.getRange();
       return new float[] {(float)rng[0],(float)rng[1]};
    }

    public float[] getYmapRange() {
       double[] rng = ymap.getRange();
       return new float[] {(float)rng[0], (float)rng[1]};
    }


    private void init(float initWaveNumber, float[] xMapRange, float[] yMapRange) throws VisADException, RemoteException {
    	
        Object source = dataChoice.getDataSource();

            MultiDimensionSubset select = null;
            Hashtable table = dataChoice.getProperties();
            Enumeration keys = table.keys();
            while (keys.hasMoreElements()) {
              Object key = keys.nextElement();
              if (key instanceof MultiDimensionSubset) {
                select = (MultiDimensionSubset) table.get(key);
              }
            }
            subset = select.getSubset();


        if (source instanceof MultiSpectralDataSource) {
        	data = ((MultiSpectralDataSource) source).getMultiSpectralData(dataChoice);
        }
        
        if (source instanceof NOAA_SNPP_DataSource) {
                data = ((NOAA_SNPP_DataSource) source).getMultiSpectralData(dataChoice);
        }

        if (Float.isNaN(initWaveNumber)) {
           waveNumber = data.init_wavenumber;
        }
        else {
           waveNumber = initWaveNumber;
        }

        try {
            spectrum = data.getSpectrum(new int[] { 1, 1 });
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
        xmap = new ScalarMap(domainType, Display.XAxis);
        ymap = new ScalarMap(rangeType, Display.YAxis);

        if (xMapRange == null || yMapRange == null) {
           xmap.setRange(initialRangeX[0], initialRangeX[1]);
           ymap.setRange(initialRangeY[0], initialRangeY[1]);
        }
        else {
           xmap.setRange(xMapRange[0], xMapRange[1]);
           ymap.setRange(yMapRange[0], yMapRange[1]);
        }

        double[] so = new double[2];
        double[] dum1 = new double[2];
        double[] dum2 = new double[2];
        xmap.getScale(so, dum1, dum2);

        display = master.getDisplay();
        display.addMap(xmap);
        display.addMap(ymap);
        display.addDisplayListener(this);
        
        AxisScale xAxis = xmap.getAxisScale();
        AxisScale yAxis = ymap.getAxisScale();
        //HersheyFont font = new HersheyFont("timesrb");
        //xAxis.setFont(font);
        //yAxis.setFont(font);
        //xAxis.setLabelSize(3*xAxis.getLabelSize());
        //yAxis.setLabelSize(3*yAxis.getLabelSize());
        xAxis.setLabelSize(34);
        yAxis.setLabelSize(34);
        xAxis.setSnapToBox(true);
        yAxis.setSnapToBox(true);

        setDisplayMasterAttributes(master);

        new RubberBandBox(this, xmap, ymap);

        spectrumRef = new DataReferenceImpl("spectrumRef_"+Hydra.getUniqueID());
        addRef(spectrumRef, Color.WHITE);

        if (data.hasBandNames()) {
            bandSelectComboBox = new JComboBox(data.getBandNames().toArray());
            bandSelectComboBox.setSelectedItem(data.init_bandName);
            bandSelectComboBox.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    String bandName = (String)bandSelectComboBox.getSelectedItem();
                    if (bandName == null)
                        return;

                    HashMap<String, Float> bandMap = data.getBandNameMap();
                    if (bandMap == null)
                        return;

                    if (!bandMap.containsKey(bandName))
                        return;

                    setWaveNumber(bandMap.get(bandName));
                }
            });
        }
    }

    public JComboBox getBandSelectComboBox() {
      return bandSelectComboBox;
    }

    public void displayChanged(final DisplayEvent e) throws VisADException, RemoteException {
        // TODO: write a method like isChannelUpdate(EVENT_ID)? or maybe just 
        // deal with a super long if-statement and put an "OR MOUSE_RELEASED" 
        // up here?
        if (e.getId() == DisplayEvent.MOUSE_RELEASED_CENTER) {
           /*
            //float val = (float)display.getDisplayRenderer().getDirectAxisValue(domainType);
            //setWaveNumber(val);
            float val = getSelectorValue(channelSelector);
            waveNumber = val;
            notifyListener(val);
            */
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
      return data.getChannelIndexFromWavenumber(waveNumber);
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

    /**
     * @return Whether or not the channel selector is being displayed.
     */
    public boolean displayingChannel() {
        return (getSelector(channelSelector) != null);
    }

    public void removeRef(final DataReference thing) throws VisADException, 
        RemoteException 
    {
        if (display == null)
            return;

        synchronized (displayedThings) {
            displayedThings.remove(thing);
            colorMaps.remove(thing);
            idToRef.remove(thing.getName());
            display.removeReference(thing);
        }
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
            if (data.hasBandNames()) {
                constMaps = new ConstantMap[colorMap.length+2];
                System.arraycopy(colorMap, 0, constMaps, 0, colorMap.length);
                constMaps[colorMap.length] = new ConstantMap(1f, Display.PointMode);
                constMaps[colorMap.length+1] = new ConstantMap(5f, Display.PointSize);
            } else {
                constMaps = colorMap;
            }
            colorMaps.put(thing, constMaps);

            display.addReference(thing, constMaps);
        }
    }

    public void updateRef(final DataReference thing, final Color color)
        throws VisADException, RemoteException 
    {
        ConstantMap[] colorMap = makeColorMap(color);
        ConstantMap[] constMaps;
        if (data.hasBandNames()) {
            constMaps = new ConstantMap[colorMap.length+2];
            System.arraycopy(colorMap, 0, constMaps, 0, colorMap.length);
            constMaps[colorMap.length] = new ConstantMap(1f, Display.PointMode);
            constMaps[colorMap.length+1] = new ConstantMap(5f, Display.PointSize);
        } else {
            constMaps = colorMap;
        }
        colorMaps.put(thing, constMaps);
        idToRef.put(thing.getName(), thing);
        refreshDisplay();
    }

    public void reorderDataRefsById(final List<String> dataRefIds) {
        if (dataRefIds == null)
            throw new NullPointerException("");

        synchronized (displayedThings) {
            try {
                displayedThings.clear();
                for (String refId : dataRefIds) {
                    DataReference ref = idToRef.get(refId);
                    ConstantMap[] color = colorMaps.get(ref);
                    display.removeReference(ref);
                    display.addReference(ref, color);
                }
            } catch (Exception e) { }
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
    private void setDisplayMasterAttributes(final XYDisplay master) 
        throws VisADException, RemoteException 
    {
        master.showAxisScales(true);
        aspect = 3.0;
        ((visad.DisplayImpl)master.getDisplay()).getProjectionControl().setAspectCartesian(new double[] {aspect, 1.0, 1.0});
        master.zoom(0.38);
        scale = master.getScale();
        
        double[] proj = master.getProjectionMatrix();
        // translate a little
        proj[3] = 0.06;
        proj[7] = 0.04;
        master.setProjectionMatrix(proj);
    }
    
    public void setBackground(Color color) throws VisADException, RemoteException {
       master.setBackground(color);
       if (color.equals(Color.white)) {
          master.setForeground(Color.black);
          updateRef(spectrumRef, Color.black);
       }
       else if (color.equals(Color.black)) {
          master.setForeground(Color.white);
          updateRef(spectrumRef, Color.white);
       }
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

        public RubberBandBox(final MultiSpectralDisplay msd,
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

        private MultiSpectralDisplay multiSpectralDisplay;

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

        public DragLine(final MultiSpectralDisplay msd, final String controlId, final Color color) throws Exception {
            this(msd, controlId, makeColorMap(color));
        }

        public DragLine(final MultiSpectralDisplay msd, final String controlId, final Color color, float[] YRANGE) throws Exception {
            this(msd, controlId, makeColorMap(color), YRANGE, Float.NaN);
        }

        public DragLine(final MultiSpectralDisplay msd, final String controlId,
            final ConstantMap[] color) throws Exception
        {
            this(msd, controlId, color, new float[] {180f, 320f}, Float.NaN);
        }

        public DragLine(final MultiSpectralDisplay msd, final String controlId, 
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
