package edu.wisc.ssec.adapter;

import edu.wisc.ssec.hydra.data.DataSelection;
import edu.wisc.ssec.hydra.data.DataChoice;
import edu.wisc.ssec.hydra.data.DataGroup;
import edu.wisc.ssec.hydra.data.DataSource;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class HydraContext {

  private static HashMap<DataSource, HydraContext> dataSourceToContextMap = new HashMap<DataSource, HydraContext>(); 
  private static HashMap<DataChoice, HydraContext> dataChoiceToContextMap = new HashMap<DataChoice, HydraContext>(); 
  private static HashMap<DataGroup, HydraContext> dataCategoryToContextMap = new HashMap<DataGroup, HydraContext>(); 

  private static HashMap<DataSource, HashMap<DataGroup, HydraContext>> contextMap = new HashMap<DataSource, HashMap<DataGroup, HydraContext>>();

  private MultiDimensionSubset subset = null;
  private Object selectBox = null;

  DataSource dataSource = null;

  private static HydraContext lastManual = null;

  public static HydraContext getHydraContext(DataSource source, DataGroup dataCategory) {
    if (dataCategory == null) {
      return getHydraContext(source);
    }
    if (contextMap.containsKey(source)) {
      if ((contextMap.get(source)).containsKey(dataCategory)) {
        return contextMap.get(source).get(dataCategory);
      }
      else {
        HashMap catMap = contextMap.get(source);
        HydraContext hydraContext = new HydraContext();
        hydraContext.dataSource = source;
        catMap.put(dataCategory, hydraContext);
        return hydraContext;
      }
    }
    else {
      HydraContext hydraContext = new HydraContext();
      hydraContext.dataSource = source;
      HashMap catMap = new HashMap();
      catMap.put(dataCategory, hydraContext);
      contextMap.put(source, catMap);
      return hydraContext;
    }
  }

  public static HashMap<DataGroup, HydraContext> getHydraContexts(DataSource source) {
    return contextMap.get(source);
  }

  public static HydraContext getHydraContext(DataSource source) {
    if (dataSourceToContextMap.isEmpty()) {
      HydraContext hydraContext = new HydraContext();
      hydraContext.dataSource = source;
      dataSourceToContextMap.put(source, hydraContext);
      return hydraContext;
    }

    if (dataSourceToContextMap.containsKey(source)) {
      return dataSourceToContextMap.get(source);
    }
    else {
      HydraContext hydraContext = new HydraContext();
      hydraContext.dataSource = source;
      dataSourceToContextMap.put(source, hydraContext);
      return hydraContext;
    }
  }

  public static HydraContext getHydraContext(DataChoice choice) {
    if (dataChoiceToContextMap.isEmpty()) {
      HydraContext hydraContext = new HydraContext();
      dataChoiceToContextMap.put(choice, hydraContext);
      return hydraContext;
    }

    if (dataChoiceToContextMap.containsKey(choice)) {
      return dataChoiceToContextMap.get(choice);
    }
    else {
      HydraContext hydraContext = new HydraContext();
      dataChoiceToContextMap.put(choice, hydraContext);
      return hydraContext;
    }
  }

  public static HydraContext getHydraContext(DataGroup choice) {
    if (dataCategoryToContextMap.isEmpty()) {
      HydraContext hydraContext = new HydraContext();
      dataCategoryToContextMap.put(choice, hydraContext);
      return hydraContext;
    }

    if (dataCategoryToContextMap.containsKey(choice)) {
      return dataCategoryToContextMap.get(choice);
    }
    else {
      HydraContext hydraContext = new HydraContext();
      dataCategoryToContextMap.put(choice, hydraContext);
      return hydraContext;
    }
  }

  public HydraContext() {
  }


  public synchronized void setMultiDimensionSubset(MultiDimensionSubset subset) {
    this.subset = subset;
  }

  public void setSelectBox(Object box) {
    selectBox = box;
  }

  public Object getSelectBox() {
    return selectBox;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public static HydraContext getLastManual() {
    return lastManual;
  }

  public static void setLastManual(HydraContext context) {
    lastManual = context;
  }

  public synchronized MultiDimensionSubset getMultiDimensionSubset() {
    return subset;
  }


}
