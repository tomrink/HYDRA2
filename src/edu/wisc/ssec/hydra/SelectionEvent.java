package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataSource;
import edu.wisc.ssec.hydra.data.DataChoice;

public class SelectionEvent { //should extend EventObject?
  DataSource dataSource;
  DataChoice choice;
  String name;
  Selection selection;

  Compute compute;
  boolean fromCompute = false;

  public SelectionEvent(Selection selection, DataSource dataSource, DataChoice choice) {
     this(selection, dataSource, choice, null);
  }

  public SelectionEvent(Selection selection, DataSource dataSource, DataChoice choice, String name) {
    this.dataSource = dataSource;
    this.choice = choice;
    this.name = name;
    this.selection = selection;
  }

  public SelectionEvent(Selection selection, Compute compute, String name) {
    this.selection = selection;
    this.compute = compute;
    this.name = name;
    this.fromCompute = true;
  }

  public DataSource getDataSource() {
    return dataSource;
  }

  public DataChoice getDataChoice() {
    return choice;
  }

  public String getName() {
    return name;
  }

  public Selection getSelection() {
    return selection;
  }
}
