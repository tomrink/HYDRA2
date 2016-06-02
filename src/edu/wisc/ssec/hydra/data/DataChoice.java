/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.wisc.ssec.hydra.data;


/**
 *
 * @author rink
 */
public class DataChoice {
   
   private DataSource dataSource;
   private String name;
   private DataGroup group;
   private DataSelection dataSelection;
   
   public DataChoice(DataSource dataSource, String name, DataGroup group) {
      this.dataSource = dataSource;
      this.name = name;
      this.group = group;
   }
   
   public DataGroup getGroup() {
      return group;
   }
   
   public DataSource getDataSource() {
      return dataSource;
   }
   
   public String getName() {
      return name;
   }
   
   public DataSelection getDataSelection() {
      return dataSelection;
   }
   
   public void setDataSelection(DataSelection dataSel) {
      this.dataSelection = dataSel;
   }
   
}
