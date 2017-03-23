package edu.wisc.ssec.hydra;

import edu.wisc.ssec.hydra.data.DataSource;
import javax.swing.*;
import java.util.*;


public abstract class SelectionAdapter implements Selection {

    private static ArrayList<Selection> listOfSelectors = new ArrayList<Selection>();

    protected ArrayList<SelectionListener> selectionListeners = new ArrayList<SelectionListener>();

    private DataSource dataSource;

    public static SelectionEvent lastSelectionEvent = null;

    public SelectionAdapter(DataSource dataSource) {
       this.dataSource = dataSource;
       for (int k=0; k<listOfSelectors.size(); k++) {
          Selection selection = listOfSelectors.get(k);
          ArrayList<SelectionListener> listeners = selection.getSelectionListeners();
          int numListeners = listeners.size();
          for (int t=0; t<numListeners; t++) {
              SelectionListener listener = listeners.get(t);
              addSelectionListener(listener);
          }
       }
       listOfSelectors.add(this);
    }

    public SelectionAdapter() {
       this(null);
    }

    public DataSource getDataSource() {
      return dataSource;
    }

    public String getSelectedName() {
      return null;
    }

    public JComponent getComponent() {
      return null;
    }

    public synchronized void remove() {
      listOfSelectors.remove(this);
    }

    public synchronized static void addSelectionListenerToAll(SelectionListener listener) {
      for (int k=0; k<listOfSelectors.size(); k++) {
        listOfSelectors.get(k).addSelectionListener(listener);
      }
    }

    public synchronized static void removeSelectionListenerFromAll(SelectionListener listener) {
      for (int k=0; k<listOfSelectors.size(); k++) {
        listOfSelectors.get(k).removeSelectionListener(listener);
      }
    }

    public synchronized void addSelectionListener(SelectionListener listener) {
      selectionListeners.add(listener);
    }

    public synchronized void removeSelectionListener(SelectionListener listener) {
      selectionListeners.remove(listener);
    }

    public synchronized ArrayList<SelectionListener> getSelectionListeners() {
       return selectionListeners;
    }

    public void fireSelectionEvent() {
      SelectionEvent e = new SelectionEvent(this, getDataSource(), getSelectedDataChoice(), getSelectedName());
      for (int k=0; k<selectionListeners.size(); k++) {
         selectionListeners.get(k).selectionPerformed(e);
      }
    }
}
