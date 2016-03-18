package edu.wisc.ssec.hydra;

import java.awt.Color;
import java.beans.PropertyChangeEvent;
import java.rmi.RemoteException;
import java.text.DecimalFormat;

import visad.CoordinateSystem;
import visad.Data;
import visad.Gridded2DSet;
import visad.DataReference;
import visad.DataReferenceImpl;
import visad.FlatField;
import visad.FunctionType;
import visad.MathType;
import visad.Real;
import visad.RealTuple;
import visad.RealTupleType;
import visad.RealType;
import visad.Text;
import visad.TextType;
import visad.Tuple;
import visad.TupleType;
import visad.VisADException;
import visad.georef.EarthLocationTuple;
import visad.georef.LatLonPoint;
import visad.georef.LatLonTuple;

import ucar.unidata.collab.Sharable;
import ucar.unidata.collab.SharableManager;
import ucar.unidata.data.grid.GridUtil;
import ucar.unidata.idv.control.GridDisplayControl;
import ucar.unidata.view.geoloc.NavigatedDisplay;
import ucar.visad.ShapeUtility;
import ucar.visad.display.DisplayMaster;
import ucar.visad.display.PointProbe;
import ucar.visad.display.SelectorDisplayable;
import ucar.visad.display.TextDisplayable;



/**
 * An abstract base class that manages a vertical probe
 * To create a probe call doMakeProbe
 * To be notified of changes override:
 * void probePositionChanged (double x, double y);
 *
 * @author IDV development team
 * @version $Revision: 1.10 $Date: 2011/03/24 16:06:32 $
 */
public class ReadoutProbe extends GridDisplayControl {

    /** profile sharing property */
    public static final String SHARE_PROFILE =
        "LineProbeControl.SHARE_PROFILE";

    /** the Point probe */
    protected PointProbe probe;

    /** the initial position */
    private RealTuple initPosition;

    /** The shape for the probe point */
    private String marker = "CROSS";

    /** The point size */
    private float pointSize = 1.0f;

    /** Keep around for the label macros */
    protected String positionText;

    private TupleType TUPTYPE = null;

    public  static int txtCnt = 0;

    private DataReference earthLocationRef = null;

    private Color currentColor = Color.MAGENTA;

    private RealTuple currentPosition = null;

    private float currentValue;

    private Tuple locationValue = null;

    private TextDisplayable valueDisplay = null;

    private TextType textType = null;

    private FlatField image = null;

    private RealTupleType earthTupleType = null;
 
    private boolean isLonLat = true;

    private DisplayMaster master;

    private DecimalFormat[] numFmt;

    private DecimalFormat latlonFmt;

    private float initTextSize = 1.75f;

    private boolean wasBig = false;

    private double baseScale = 1.0;

    private InfoLabel infoLabel;


    /**
     * Default Constructor.
     */
    public ReadoutProbe(FlatField grid2d, DisplayMaster master, double[][] dataRange)
           throws VisADException, RemoteException {
        this(grid2d, master, dataRange, true);
    }
    public ReadoutProbe(FlatField grid2d, DisplayMaster master, double[][] dataRange, boolean share) 
           throws VisADException, RemoteException {
        super();

        this.image = grid2d;
        this.master = master;

        earthTupleType = check2DEarthTuple(grid2d);
        if (earthTupleType != null) {
          isLonLat = earthTupleType.equals(RealTupleType.SpatialEarth2DTuple);
        }
        setAttributeFlags(FLAG_COLOR);
        initSharable();

        currentPosition = new RealTuple(RealTupleType.Generic2D);

        earthLocationRef = new DataReferenceImpl("positionRef_"+Hydra.getUniqueID());

        if (share) {
           setSharing(true);
        }

        /**
        master.getDisplay().addDisplayListener( new DisplayListener() {
            public void displayChanged(DisplayEvent de) {
              if ((de.getId() == DisplayEvent.MOUSE_RELEASED)) {
                try {
                  RealTuple position = getPosition();
                  double[] vals = position.getValues();
                  EarthLocationTuple elt =
                      (EarthLocationTuple)boxToEarth(new double[] { vals[0], vals[1], 1.0 });
                  doShare(SHARE_POSITION, elt.getLatLonPoint());
                } catch (Exception e) {
                    logException("doMoveProfile", e);
                }
              }
            }
        });
        */

        numFmt = getValueDisplayFormat(dataRange);
        latlonFmt = new DecimalFormat();
        latlonFmt.setMaximumFractionDigits(2);
        latlonFmt.setMinimumFractionDigits(2);
    }
    
    DecimalFormat[] getValueDisplayFormat(double[][] dataRange) {
        DecimalFormat[] numFmt = new DecimalFormat[dataRange.length];
        
        //TODO: this is pretty kludgy but will do for now.
        for (int i=0; i<numFmt.length; i++) {
            double OofM = java.lang.Math.log10(Math.abs(dataRange[i][1]));
            if (OofM <= -2 || OofM >= 4) {
                numFmt[i] = new DecimalFormat("0.00E00");
            }
            else {
	        numFmt[i] = new DecimalFormat();
	        numFmt[i].setMaximumFractionDigits(2);
            }
        }
        
        return numFmt;
    }

    DataReference getEarthLocationRef() {
       return earthLocationRef;
    }

    public void updateData(FlatField image) {
       this.image = image;
       updateLocationValue();
    }
    
    public void updateData(FlatField image, EarthLocationTuple loc) throws Exception {
        this.image = image;
        setProbePosition(loc);
    }

    public void resize(double baseScale, double scale) throws VisADException, RemoteException {
      double baseSize = probe.getSelectorPoint().getScale();
      //probe.getSelectorPoint().setScale((float) ((baseScale/scale)*baseSize));
      probe.getSelectorPoint().setPointSize((float) ((baseScale/scale)*baseSize));
      valueDisplay.setTextSize((float) (initTextSize*(baseScale/scale)));
    }

    public void setTextSize(float textSize) throws VisADException, RemoteException {
      valueDisplay.setTextSize(textSize);
    }


    /**
     * Make the probe with the specific <code>Color</code> and
     * <code>ViewDescriptor</code>.
     *
     * @param probeColor    color for the probe
     * @param view  view descriptor
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public void doMakeProbe(Color probeColor, DisplayMaster master, double baseScale, double scale) 
            throws VisADException, RemoteException {
       doMakeProbe(probeColor, master, baseScale, scale, true, "CROSS", 1.2f, 1.75f, null);
    }

    public void doMakeProbe(Color probeColor, DisplayMaster master, double baseScale, double scale, boolean visible, String marker, float pointSize, float textSize, Color textColor)
            throws VisADException, RemoteException {

        initTextSize = textSize;

        if (initPosition != null) {
            probe = new PointProbe(initPosition);
        } else {
            probe = new PointProbe(getInitialLinePosition());
        }
        initPosition = probe.getPosition();
        updatePosition(initPosition);

        probe.setColor(probeColor);
        probe.setVisible(visible);
        probe.setPointSize(pointSize);
        if (marker != null) {
            probe.setMarker(ShapeUtility.setSize(ShapeUtility.makeShape(marker), 0.04f));
        }
        probe.setAutoSize(true);

        if (textColor != null) currentColor = textColor;
        valueDisplay = createValueDisplayer(currentColor, initTextSize);
        valueDisplay.setVisible(visible);
        updateLocationValue();

        this.baseScale = baseScale;
        this.marker = marker;

        if ( (baseScale != scale) && !Double.isNaN(baseScale) && !Double.isNaN(scale) ) {
           resize(baseScale, scale);
        }

        master.addDisplayable(probe);
        master.addDisplayable(valueDisplay);

        probe.addPropertyChangeListener(this);
    }

    public void destroy() {
       image = null;
       SharableManager.removeSharable(this);
       try {
          probe.removePropertyChangeListener(this);
       }
       catch (Exception e) {
          e.printStackTrace();
       }
    }

    /**
     * Handle changes
     *
     * @param evt The event
     */
    public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getPropertyName().equals(
                SelectorDisplayable.PROPERTY_POSITION)) {
            doMoveProbe();
        } else {
            super.propertyChange(evt);
        }
    }

    /**
     * Reset the position of the probe to the center.
     */
    public void resetProbePosition() {
        try {
            setProbePosition(0.0, 0.0);
        } catch (Exception exc) {
            logException("Resetting probe position", exc);
        }
    }

    /**
     * Set the probe position.  Probes are set in XY space.
     *
     * @param xy  X and Y position of the probe.
     *
     * @throws VisADException  problem setting probe position
     * @throws RemoteException  problem setting probe position on remote display
     */
    public void setProbePosition(RealTuple xy)
            throws VisADException, RemoteException {
        probe.setPosition(xy);
    }

    public void setProbePosition(EarthLocationTuple lla)
            throws VisADException, RemoteException {
       double[] xyz = earthToBox(lla);
       if (Double.isNaN(xyz[0]) || Double.isNaN(xyz[1])) { // safeguard: problems if NaN passed through
           return;
       }
       setProbePosition(xyz[0], xyz[1]);
    }

    /**
     * Set the probe position from display x and y positions.
     *
     * @param x    X position of the probe.
     * @param y    Y position of the probe.
     *
     * @throws VisADException  problem setting probe position
     * @throws RemoteException  problem setting probe position on remote display
     */
    public void setProbePosition(double x, double y)
            throws VisADException, RemoteException {
        setProbePosition(new RealTuple(new Real[] {
            new Real(RealType.XAxis, x),
            new Real(RealType.YAxis, y) }));
    }

    /**
     * Set the initial position of the probe.  This is used by the
     * XML persistense.
     *
     * @param p  position
     */
    public void setPosition(RealTuple p) {
        initPosition = p;
    }

    public void setPosition(EarthLocationTuple lla)
            throws VisADException, RemoteException {
        double[] xyz = earthToBox(lla);
        double x = xyz[0];
        double y = xyz[1];
        initPosition = new RealTuple(new Real[] {
            new Real(RealType.XAxis, x),
            new Real(RealType.YAxis, y) });
    }

    public void setFixed() {
        probe.setFixed(true, true, true);
    }


    /**
     * Get the position of the probe.  This is used by the
     * XML persistense.
     *
     * @return current probe position or null if probe has not been created.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   VisAD Error
     */
    public RealTuple getPosition() throws VisADException, RemoteException {
        return ((probe != null)
                ? probe.getPosition()
                : null);
    }

    /**
     * Get the initial position of the probe set during unpersistence.
     *
     * @return  initial position or <code>null</code> if not set during
     *          initialization.
     */
    public RealTuple getInitialPosition() {
        return initPosition;
    }

    /**
     * Method called when sharing is enabled.
     *
     * @param from  Sharable that send the data.
     * @param dataId  identifier for data to be shared
     * @param data   data to be shared.
     */
    public void receiveShareData(Sharable from, Object dataId,
                                 Object[] data) {
        if (dataId.equals(SHARE_POSITION)) {
            if (probe == null) {
                return;
            }
            try {
                RealTuple tup = (RealTuple) data[0];
                if (tup instanceof LatLonPoint) {
                   RealTuple xyz = earthToBoxTuple(new EarthLocationTuple((LatLonPoint) tup,
                                new Real(RealType.Altitude, 0)));
                   if (xyz != null) {
                      RealTuple pos = new RealTuple(new Real[] { (Real) xyz.getComponent(0),
                          (Real) xyz.getComponent(1) });
                      probe.setPosition(pos);
                   }
                }
                else {
                  probe.setPosition((RealTuple) data[0]);
                }
            } catch (Exception e) {
                logException("receiveShareData:" + dataId, e);
            }
            return;
        }
    }


    /**
     * Method called when probe is moved.
     */
    protected void doMoveProbe() {
        try {
            RealTuple position = getPosition();
            probePositionChanged(position);

            double[] vals = position.getValues();
            EarthLocationTuple elt =
                  (EarthLocationTuple)boxToEarth(new double[] { vals[0], vals[1], 1.0 });

            // send position while dragging
            doShare(SHARE_POSITION, elt.getLatLonPoint());
            //test doShare(SHARE_POSITION, getNearestImagePixel(image, elt.getLatLonPoint()));
        } catch (Exception e) {
            logException("doMoveProfile", e);
        }
    }

    /**
     * This gets called when either the user moves the probe point or
     * when we get a sharable event to move the probe point. Subclasses
     * need to implement this.
     *
     * @param position  new position for the probe.
     */
    protected void probePositionChanged(final RealTuple newPos) {
        if (!currentPosition.equals(newPos)) {
            updatePosition(newPos);
            updateLocationValue();
            currentPosition = newPos;
        }
    }

    protected void updatePosition(final RealTuple position) {
        double[] vals = position.getValues();
        try {
            EarthLocationTuple elt = (EarthLocationTuple)boxToEarth(
                new double[] { vals[0], vals[1], 1.0 });

            earthLocationRef.setData(elt.getLatLonPoint());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void setInfoLabel(InfoLabel infoLabel) {
        this.infoLabel = infoLabel;
    }

    private void updateInfoLabel(float lon, float lat, float val) {
        if (infoLabel != null) {
           infoLabel.setLatLonAndValue(lat, lon, val);
        }
    }
    
    private void updateInfoLabel(float lon, float lat, double[] vals) {
        if (infoLabel != null) {
           infoLabel.setLatLonAndValues(lat, lon, vals);
        }
    }

    private void updateLocationValue() {
        Tuple tup = null;
        RealTuple earthTuple;

        try {
            RealTuple location = (RealTuple)earthLocationRef.getData();
          
            if (location == null)
                return;

            if (image == null)
                return;

            double[] vals = location.getValues();
            if (vals[1] < -180)
                vals[1] += 360f;

            if (vals[1] > 180)
                vals[1] -= 360f;

            
            if (earthTupleType != null) {
              RealTuple lonLat =
                 new RealTuple(RealTupleType.SpatialEarth2DTuple,
                     new double[] { vals[1], vals[0] });
              RealTuple latLon = new RealTuple(RealTupleType.LatitudeLongitudeTuple,
                     new double[] { vals[0], vals[1] });
              RealTuple rtup = lonLat;
              if (!(isLonLat)) {
                rtup = latLon;
              }
           
              Real val = null;
              Data dat = image.evaluate(rtup, Data.NEAREST_NEIGHBOR, Data.NO_ERRORS);

              if ( ((FunctionType)image.getType()).getRange() instanceof RealTupleType ) { 
                 RealTuple tmp = (RealTuple)dat;
                 Real[] reals = tmp.getRealComponents();
                 double[] dvals = new double[reals.length];
                 for (int k=0; k<dvals.length; k++){
                    dvals[k] = reals[k].getValue();
                 }
                 updateInfoLabel((float)vals[1], (float)vals[0], dvals);
                 tup = new Tuple(TUPTYPE,
                         new Data[] { lonLat, new Text(textType, numFmt[0].format(dvals[0])) });
                 currentValue = (float) dvals[0];
              }
              else {
                 val = (Real)dat;
                 float fval = (float)val.getValue();
                 updateInfoLabel((float)vals[1], (float)vals[0], fval);
                 tup = new Tuple(TUPTYPE,
                         new Data[] { lonLat, new Text(textType, numFmt[0].format(fval)) });
                 currentValue = fval;
              }
              
            }
            
            if (valueDisplay != null) {
              valueDisplay.setData(tup);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (tup != null)
            locationValue = tup;
    }

    public float getFloatValue() {
       return currentValue;
    }

    public NavigatedDisplay  getNavigatedDisplay() {
      return (NavigatedDisplay) master;
    }

    public static RealTupleType check2DEarthTuple(FlatField field) {
      CoordinateSystem cs;
      FunctionType ftype = (FunctionType) field.getType();
      RealTupleType domain = ftype.getDomain();
      if ( (domain.equals(RealTupleType.SpatialEarth2DTuple)) ||
           (domain.equals(RealTupleType.LatitudeLongitudeTuple)) ) {
        return domain;
      } 
      else if ((cs = domain.getCoordinateSystem()) != null) {
        RealTupleType ref = cs.getReference();
        if ( (ref.equals(RealTupleType.SpatialEarth2DTuple)) ||
             (ref.equals(RealTupleType.LatitudeLongitudeTuple)) ) {
           return ref;
        }
      }
      return null;
    }

    public static LatLonPoint getNearestImagePixel(FlatField image, LatLonPoint llPt) throws VisADException, RemoteException {
      CoordinateSystem cs;
      FunctionType ftype = (FunctionType) image.getType();
      Gridded2DSet domainSet = (Gridded2DSet) image.getDomainSet();
      RealTupleType domain = ftype.getDomain();
      int latIdx = 0;
      int lonIdx = 1;

      double[] dblVal = ((RealTuple)llPt).getValues();
      float[][] latLon = new float[][] {{(float) dblVal[0]}, {(float)dblVal[1]}};
      float[][] lonLat = new float[][] {{latLon[1][0]}, {latLon[0][0]}};

      float[][] earthPt = null;

      if ((cs = domain.getCoordinateSystem()) != null) {
        RealTupleType ref = cs.getReference();
        if (ref.equals(RealTupleType.SpatialEarth2DTuple)) {
          earthPt = lonLat;
          lonIdx = 0;
          latIdx = 1;
        }
        else if (ref.equals(RealTupleType.LatitudeLongitudeTuple)) {
          earthPt = latLon;
        }

        float[][] val = cs.fromReference(earthPt);
        int[] idx = domainSet.valueToIndex(val);
        val = domainSet.indexToValue(idx);
        val = cs.toReference(val);
        return new LatLonTuple(val[latIdx][0], val[lonIdx][0]);
      }
      else {
        if (domain.equals(RealTupleType.SpatialEarth2DTuple)) {
          earthPt = lonLat;
          lonIdx = 0;
          latIdx = 1;
        }
        else if (domain.equals(RealTupleType.LatitudeLongitudeTuple)) {
          earthPt = latLon;
        }
        int[] idx = domainSet.valueToIndex(latLon);
        float[][] val = domainSet.indexToValue(idx);
        return new LatLonTuple(val[latIdx][0], val[lonIdx][0]);
      }
    }

    private TextDisplayable createValueDisplayer(final Color color, float initTextSize)
        throws VisADException, RemoteException
    {
        DecimalFormat fmt = new DecimalFormat();
        fmt.setMaximumIntegerDigits(3);
        fmt.setMaximumFractionDigits(1);

        textType = new TextType("GENERIC_TEXT"+txtCnt++);
        TUPTYPE = makeTupleType();
        TextDisplayable td = new TextDisplayable(textType);
        td.setLineWidth(2f);
        td.setColor(color);
        td.setTextSize(initTextSize);

        return td;
    }

    private TupleType makeTupleType() {
        TupleType t = null;
        try {
            t = new TupleType(new MathType[] {RealTupleType.SpatialEarth2DTuple,
                                              textType});
        } catch (Exception e) {
            e.printStackTrace();
        }
        return t;
    }

    /**
     * Respond to a change in the display's projection.  In this case
     * we fire the probePositionChanged() method with the probe's
     * position.
     */
    public void projectionChanged() {
        try {
            updatePosition(getPosition());
            probePositionChanged(getPosition());
        } catch (Exception exc) {
            logException("projectionChanged", exc);
        }
    }

    /**
     * Increase the probe size
     */
    public void increaseProbeSize() {
        if (probe == null) {
            return;
        }
        pointSize = probe.getPointScale();
        setPointSize(pointSize + pointSize * 0.5f);
    }


    /**
     * Decrease the probe size
     */
    public void decreaseProbeSize() {
        if (probe == null) {
            return;
        }
        pointSize = probe.getPointScale();
        pointSize = pointSize - pointSize * 0.5f;
        if (pointSize < 0.1f) {
            pointSize = 0.1f;
        }
        setPointSize(pointSize);
    }


    /**
     *  Set the PointSize property.
     *
     *  @param value The new value for PointSize
     */
    public void setPointSize(float value) {
        pointSize = value;
        if (probe != null) {
            try {
                probe.setAutoSize(false);
                probe.setPointSize(pointSize);
                probe.setAutoSize(true);
            } catch (Exception exc) {
                logException("Increasing probe size", exc);
            }
        }
    }

    /**
     *  Get the PointSize property.
     *
     *  @return The PointSize
     */
    public float getPointSize() {
        return pointSize;
    }


    /**
     * Get initial XY position from grid data.
     *
     * @return initial XY position of grid center point in VisAD space
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public RealTuple getGridCenterPosition()
            throws VisADException, RemoteException {
        RealTuple pos = new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                                      new double[] { 0,
                0 });
        if (getGridDataInstance() != null) {
            LatLonPoint rt = GridUtil.getCenterLatLonPoint(
                                 getGridDataInstance().getGrid());
            RealTuple xyz = earthToBoxTuple(new EarthLocationTuple(rt,
                                new Real(RealType.Altitude, 0)));
            if (xyz != null) {
                pos = new RealTuple(new Real[] { (Real) xyz.getComponent(0),
                        (Real) xyz.getComponent(1) });
            }
        }
        return pos;
    }


    /**
     * Get initial XY position from the screen
     *
     * @return initial XY position  in VisAD space
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public RealTuple getInitialLinePosition()
            throws VisADException, RemoteException {
        //-double[] center = getScreenCenter();
        double[] center = new double[] {0,0};
        return new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                             new double[] { center[0],
                                            center[1] });
    }

    /**
     * Set initial XY position from the screen
     *
     * @return initial XY position  in VisAD space
     *
     * @throws RemoteException Java RMI problem
     * @throws VisADException VisAD problem
     */
    public RealTuple setInitialPosition(double x, double y)
            throws VisADException, RemoteException {
        double[] pos = new double[] {x,y};
        initPosition = new RealTuple(RealTupleType.SpatialCartesian2DTuple,
                             new double[] { pos[0], pos[1] });
        return initPosition;
    }


    /**
     * Set the Marker property.
     *
     * @param value The new value for Marker
     */
    public void setMarker(String value) {
        marker = value;
        if ((probe != null) && (marker != null)) {
            try {
                //probe.setAutoSize(false);
		/*
                probe.setMarker(
                    SelectorPoint.reduce(ShapeUtility.makeShape(marker))); */
                //probe.setAutoSize(true);
            } catch (Exception exc) {
                logException("Setting marker", exc);
            }
        }
    }

    /**
     * Get the Marker property.
     *
     * @return The Marker
     */
    public String getMarker() {
        return marker;
    }

    public boolean getVisible() {
       return probe.getVisible();
    }

    public boolean getReadoutVisible() {
       return valueDisplay.getVisible();
    }

    public void setReadoutVisible(boolean yesno) {
       try {
          valueDisplay.setVisible(yesno);
       }
       catch (Exception e) {
          e.printStackTrace();
          System.out.println("could not change probe visibility");
       }
    }

    public void setVisible(boolean yesno) {
       try {
          probe.setVisible(yesno);
          valueDisplay.setVisible(yesno);
       }
       catch (Exception e) {
          e.printStackTrace();
          System.out.println("could not change probe visibility");
       }
    }
}
