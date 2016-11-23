package edu.wisc.ssec.adapter;

import visad.FlatField;
import visad.SampledSet;
import visad.RealTuple;
import visad.RealTupleType;
import visad.VisADException;
import visad.FunctionType;
import visad.Gridded1DSet;
import visad.QuickSort;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.ArrayList;
import visad.CoordinateSystem;
import visad.Linear2DSet;

public class MultiSpectralAggr extends MultiSpectralData {

  Gridded1DSet aggrDomain = null;

  MultiSpectralData[] adapters = null;

  int[] sort_indexes = null;

  float[] aggrValues = null;

  float[] aggrSamples = null;

  int numAdapters;

  int numBands;

  int[] offset;

  public MultiSpectralAggr(MultiSpectralData[] adapters)
         throws Exception {
    this(adapters, null);
  }

  public MultiSpectralAggr(MultiSpectralData[] adapters, String name)
         throws Exception {
    super(adapters[0].swathAdapter, null);
    this.adapters = adapters;
    paramName = adapters[0].getParameter();
    sensorName = adapters[0].getSensorName();
    if (name != null) {
      this.name = name;
    }

    numAdapters = adapters.length;
    int[] numBandsAdapter = new int[numAdapters];
    offset = new int[numAdapters];
    SampledSet[] spectrumDomains = new SampledSet[numAdapters];

    if (adapters[0].spectrumAdapter.hasBandNames()) {
      hasBandNames = true;
      bandNameList = new ArrayList<String>();
      bandNameMap = new HashMap<String, Float>();
      for (int k=0; k<numAdapters; k++) {
        bandNameList.addAll(adapters[k].spectrumAdapter.getBandNames());
        bandNameMap.putAll(adapters[k].spectrumAdapter.getBandNameMap());
      }
    }

    numBands = 0;
    for (int k=0; k<numAdapters; k++) {
      SampledSet set = adapters[k].spectrumAdapter.getDomainSet();
      spectrumDomains[k] = set;
      numBandsAdapter[k] = set.getLength();
      offset[k] = numBands;
      numBands += numBandsAdapter[k];
    }
   
    aggrSamples = new float[numBands];
    aggrValues  = new float[numBands];

    for (int k=0; k<numAdapters; k++) {
      float[][] samples = spectrumDomains[k].getSamples(false);
      System.arraycopy(samples[0], 0, aggrSamples, offset[k], samples[0].length);
    }

    sort_indexes = QuickSort.sort(aggrSamples);
    SpectrumAdapter specAdapt = adapters[0].spectrumAdapter;
    aggrDomain = new Gridded1DSet(specAdapt.getDomainSet().getType(), 
                        new float[][] {aggrSamples}, aggrSamples.length); 
  }

  public FlatField getSpectrum(int[] coords) throws Exception {
    FlatField spectrum = null;
    for (int k=0; k<numAdapters; k++) {
      spectrum = adapters[k].getSpectrum(coords);
      if (spectrum == null) {
        return null;
      }
      float[][] values = spectrum.getFloats(false);
      System.arraycopy(values[0], 0, aggrValues, offset[k], values[0].length);
    }

    float[] sortVals = new float[numBands];
    for (int t=0; t<numBands; t++) {
      sortVals[t] = aggrValues[sort_indexes[t]];
    }

    spectrum = new FlatField((FunctionType)spectrum.getType(), aggrDomain);
    spectrum.setSamples(new float[][] {sortVals});

    return spectrum;
  }

  public FlatField getSpectrum(RealTuple location) throws Exception {
    FlatField spectrum = null;
    for (int k=0; k<numAdapters; k++) {
      spectrum = adapters[k].getSpectrum(location);
      if (spectrum == null) {
        return null;
      }
      float[][] values = spectrum.getFloats(false);
      System.arraycopy(values[0], 0, aggrValues, offset[k], values[0].length);
    }

    float[] sortVals = new float[numBands];
    for (int t=0; t<numBands; t++) {
      sortVals[t] = aggrValues[sort_indexes[t]];
    }
    
    spectrum = new FlatField((FunctionType)spectrum.getType(), aggrDomain);
    spectrum.setSamples(new float[][] {sortVals});

    return spectrum;
  }

  public FlatField getImage(HashMap subset) throws Exception {
    int channelIndex = (int) ((double[])subset.get(SpectrumAdapter.channelIndex_name))[0];
    return getImage(channelIndex, subset);
  }

  public FlatField getImage(float channel, HashMap subset) throws Exception {
    int channelIndex = aggrDomain.valueToIndex(new float[][] {{channel}})[0];
    return getImage(channelIndex, subset);
  }
  
  public FlatField getImage(int channelIndex, HashMap subset) throws Exception {
    int idx = sort_indexes[channelIndex];     
    int swathAdapterIndex = numAdapters-1;
    for (int k=0; k<numAdapters-1;k++) {
      if (idx >= offset[k] && idx < offset[k+1]) swathAdapterIndex = k;
    }
    
    float channel = aggrSamples[channelIndex];
    FlatField image = adapters[swathAdapterIndex].getImage(channel, subset);
    Linear2DSet domSet = (Linear2DSet) image.getDomainSet();
    cs = ((RealTupleType) ((FunctionType)image.getType()).getDomain()).getCoordinateSystem();
    
    float[] reflCorr = adapters[swathAdapterIndex].getReflectanceCorr(domSet);
    for (int k=0; k<numAdapters;k++) {
      adapters[k].setCoordinateSystem(cs);
      if (k != swathAdapterIndex) {
         adapters[k].setReflectanceCorr(domSet, reflCorr);
      }
    }
    
    return image;     
  }

  public int getChannelIndexFromWavenumber(float channel) throws VisADException, RemoteException {
    int idx = (aggrDomain.valueToIndex(new float[][] {{channel}}))[0];
    return idx;
  }

  public float getWavenumberFromChannelIndex(int index) throws Exception {
    return (aggrDomain.indexToValue(new int[] {index}))[0][0];
  }
  
  public void setCoordinateSystem(CoordinateSystem cs) {
     this.cs = cs;
     for (int k=0; k<numAdapters; k++) {
        adapters[k].setCoordinateSystem(cs);
     }
  }
  
  public void setSwathDomainSet(Linear2DSet dset) {
     for (int k=0; k<numAdapters; k++) {
        adapters[k].setSwathDomainSet(dset);
     }     
  }
  
  public int getNumChannels() {
     return numBands;
  }
  
  public Gridded1DSet getSpectralDomain() {
     return aggrDomain;
  }
  
  public boolean hasBandName(String name) {
     for (int k=0; k<numAdapters; k++) {
        if (adapters[k].hasBandName(name)) {
           return true;
        }
     }
     return false;
  }

  public HashMap getDefaultSubset() {
    HashMap subset = adapters[0].getDefaultSubset();
    double chanIdx = 0;
    try {
      chanIdx = getChannelIndexFromWavenumber(init_wavenumber);
    }
    catch (Exception e) {
      System.out.println("couldn't get chanIdx, using zero");
    }
    subset.put(SpectrumAdapter.channelIndex_name, new double[] {chanIdx, chanIdx, 1});
    return subset;
  }

}
