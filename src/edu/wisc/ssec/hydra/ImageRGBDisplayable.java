package edu.wisc.ssec.hydra;

import ucar.visad.display.DisplayableData;
import ucar.visad.display.ScalarMapSet;

import ucar.unidata.data.grid.GridUtil;

import visad.*;

import visad.bom.ImageRendererJ3D;

import java.rmi.RemoteException;
import visad.georef.EarthLocationTuple;


public class ImageRGBDisplayable extends DisplayableData {


    /** color ScalarMaps */
    private volatile ScalarMap[] colorMaps = { null, null, null };

    /** color MathType */
    private volatile RealTupleType colorTupleType;

    /** color palette */
    private float[][] colorPalette;

    /** What do we map with */
    private DisplayRealType mapType = Display.RGB;

    /** flag for whether we use Alpha channel or not */
    private boolean doAlpha = false;

    private static int uniqueID = 0;
    
    public DataRenderer theRenderer;
    
    public EarthLocationTuple minLoc;
    
    public EarthLocationTuple maxLoc;
    
    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ImageRGBDisplayable(String name)
            throws VisADException, RemoteException {
        this(name, false);
    }


    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @param doAlpha           true to map to RGBA
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ImageRGBDisplayable(String name, boolean doAlpha)
            throws VisADException, RemoteException {
        this(name, BaseColorControl.initTableGreyWedge(new float[(doAlpha)
                ? 4
                : 3][255]), doAlpha);
    }

    /**
     * Constructs from a name for the Displayable and the type of the
     * RGB parameter.
     *
     * @param name              The name for the displayable.
     * @param colorPalette      The color palette
     * @param doAlpha           true to map to RGBA
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    public ImageRGBDisplayable(String name, float[][] colorPalette,
                               boolean doAlpha)
            throws VisADException, RemoteException {
        this(name, colorPalette, doAlpha, null);
    }


    /**
     * Constructs from another instance.  The following attributes are set from
     * the other instance: color palette, the color RealType.
     * @param that              The other instance.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected ImageRGBDisplayable(ImageRGBDisplayable that)
            throws VisADException, RemoteException {

        super(that);
        this.doAlpha   = that.doAlpha;
        colorTupleType = that.colorTupleType;
        colorPalette   = Set.copyFloats(that.colorPalette);
        if (colorTupleType != null) {
            setColorMaps();
        }
    }

    public ImageRGBDisplayable(String name, float[][] colorPalette, boolean doAlpha, FieldImpl field)
            throws VisADException, RemoteException {
        super(name);
        this.doAlpha = doAlpha;
        this.colorPalette = colorPalette;
        if (doAlpha) {
            mapType   = Display.RGBA;
            colorMaps = new ScalarMap[] { null, null, null, null };
        }

        addConstantMaps(new ConstantMap[] {
            new ConstantMap(GraphicsModeControl.SUM_COLOR_MODE,
                            Display.ColorMode),
            new ConstantMap(1.0, Display.MissingTransparent) });


        if (field != null) {
          TupleType     tt       = GridUtil.getParamType(field);
          RealTupleType ffldType = new RealTupleType(tt.getRealComponents());

          if ((getColorTupleType() == null)
                 || !ffldType.equals(getColorTupleType())) {
              setColorTupleType(ffldType);
          }
        }
    }
    
    /**
     * Set the data into the Displayable; set RGB Type
     *
     *
     * @param field an image or sequence of images
     * @exception VisADException  from construction of VisAd objects
     * @exception RemoteException from construction of VisAD objects
     */
    public void loadData(FieldImpl field)
            throws VisADException, RemoteException {
        setData(field);
    }


    /**
     * Get the RealTupleType of the RGB parameter.
     *  @return The RealTupleType of the RGB parameters.
     *         May be <code>null</code>.
     */
    public RealTupleType getColorTupleType() {
        return colorTupleType;
    }

    /**
     * Sets the RealTupleType of the RGB parameter.
     * @param realTupleType     The RealTupleType of the RGB parameters.  May
     *                          not be <code>null</code>.
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    protected void setColorTupleType(RealTupleType realTupleType)
            throws RemoteException, VisADException {

        if ( !realTupleType.equals(colorTupleType)) {
            RealTupleType oldValue = colorTupleType;
            colorTupleType = realTupleType;
            setColorMaps();
        }
    }


    /**
     * Returns the RealTupleType of the RGB parameter.
     * @return                  The RealTupleType of the color parameter.  May
     *                          be <code>null</code>.
     * @deprecated  use getColorTupleType()
     */
    public RealTupleType getRGBRealTupleType() {
        return colorTupleType;
    }


    /**
     * Sets the set of ScalarMap-s of this instance.  The ScalarMap-s of
     * this instance will be added to the set before the SCALAR_MAP_SET
     * property is set.  This method fires a PropertyChangeEvent for
     * SCALAR_MAP_SET with <code>null</code> for the old value and the new
     * set of ScalarMap-s for the new Value.  Intermediate subclasses that
     * have their own ScalarMap-s should override this method and invoke
     * <code>super.setScalarMaps(ScalarMapSet)</code>.
     * @param maps              The set of ScalarMap-s to be added.
     * @throws BadMappingException      The RealType of the color parameter
     *                          has not been set or its ScalarMap is alread in
     *                          the set.
     */
    protected void setScalarMaps(ScalarMapSet maps)
            throws BadMappingException {

        if (colorMaps[0] == null) {
            throw new BadMappingException(getClass().getName()
                                          + ".setScalarMaps(ScalarMapSet): "
                                          + "Color not yet set");
        }

        for (int i = 0; i < colorMaps.length; i++) {
            maps.add(colorMaps[i]);
        }
        super.setScalarMapSet(maps);
    }

    /**
     * Set the alpha. Unused.
     *
     * @param alpha alpha
     *
     * @throws RemoteException On badness
     * @throws VisADException On badness
     */
    public void setAlpha(float alpha) throws RemoteException, VisADException {
        addConstantMaps(new ConstantMap[] {
            new ConstantMap(alpha, Display.Alpha) });
    }

    /**
     * creates the ScalarMaps for color  for this Displayable.
     *
     * @throws VisADException   VisAD failure.
     * @throws RemoteException  Java RMI failure.
     */
    private void setColorMaps() throws RemoteException, VisADException {

        ScalarMapSet set = new ScalarMapSet();
        for (int i = 0; i < colorMaps.length; i++) {
            colorMaps[i] =
                new ScalarMap((RealType) colorTupleType.getComponent(i),
                              mapType);
            /* TODO: maybe allow user to set range.  If so, just copy
               logic from RGBDisplayable */
            //-TDR colorMaps[i].setRange(0, 255);
            set.add(colorMaps[i]);
            final int colorMapIndex = i;
            colorMaps[i].addScalarMapListener(new ScalarMapListener() {
                public void controlChanged(ScalarMapControlEvent event)
                        throws RemoteException, VisADException {
                    int id = event.getId();
                    if ((id == event.CONTROL_ADDED)
                            || (id == event.CONTROL_REPLACED)) {
                        setColorsInControls(colorPalette, colorMapIndex);
                    }
                }

                public void mapChanged(ScalarMapEvent event)
                        throws RemoteException, VisADException { 
                }
            });
        }

        setScalarMapSet(set);
        setColorsInControls(colorPalette);
    }

    /**
     * Set the display.
     *
     * @param display  display to set this into
     *
     * @throws DisplayException Display type exception
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    public void setDisplay(LocalDisplay display)
            throws DisplayException, VisADException, RemoteException {
        super.setDisplay(display);
        setColorsInControls(colorPalette);
    }

    /**
     * This method sets the color palette
     * according to the color table in argument;
     * pair this method with setRange(lo,high) to get
     * a fixed association of color table and range of values.
     *
     * @param colorPalette     the color table or color-alpha table desired
     * @throws VisADException  if a core VisAD failure occurs.
     * @throws RemoteException if a Java RMI failure occurs.
     */
    public void setColorPalette(float[][] colorPalette)
            throws RemoteException, VisADException {

        setColorsInControls(colorPalette);
        this.colorPalette = colorPalette;
    }

    /**
     * Return the current color palette in this Displayable
     *
     * @return a color table float[3][len] or color-alpha table float[4][len]
     */
    public float[][] getColorPalette() {
        return colorPalette;
    }


    /**
     * Set colors for the controls of all color maps.
     *
     * @param colorPalette The 3xN color palette array
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setColorsInControls(float[][] colorPalette)
            throws RemoteException, VisADException {

        for (int i = 0; i < colorMaps.length; i++) {
            setColorsInControls(colorPalette, i);
        }
    }




    /**
     * Set colors for the control defined by the given colorMapIndex (0,1 or 2).
     *
     * @param colorPalette The 3xN color palette array
     * @param colorMapIndex Which of the color maps are we setting the color of.
     *
     * @throws RemoteException  Java RMI error
     * @throws VisADException   problem creating VisAD object
     */
    private void setColorsInControls(float[][] colorPalette,
                                     int colorMapIndex)
            throws RemoteException, VisADException {
        if (colorPalette == null) {
            return;
        }


        if (colorMaps[colorMapIndex] == null) {
            return;
        }

        BaseColorControl bcc =
            (BaseColorControl) colorMaps[colorMapIndex].getControl();

        if (bcc != null) {
            float[][] table =
                new float[colorMaps.length][colorPalette[0].length];
            table[colorMapIndex] = colorPalette[colorMapIndex];
            bcc.setTable(table);
        }
    }

    /**
     *
     * @return
     * @throws VisADException
     */
    @Override
    protected DataRenderer getDataRenderer() throws VisADException {
        
      ImageRendererJ3D myRenderer = new ImageRendererJ3D();
      theRenderer = myRenderer;
      return myRenderer;
    }
    
    /**
     *
     * @param yesno
     */
    @Override
    public void setVisible(boolean yesno) {
        if (theRenderer != null) {
            theRenderer.toggle(yesno);
        }
        else {
            try {
                super.setVisible(yesno);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    
    /**
     * Set whether this GridDisplayable should have the data colored
     * by another parameter.  This implementation is a no-op.
     *
     * @param yesno true if colored by another
     */
    public void setColoredByAnother(boolean yesno) {}

}
