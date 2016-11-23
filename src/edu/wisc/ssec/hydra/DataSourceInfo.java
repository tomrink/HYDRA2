package edu.wisc.ssec.hydra;

/**
 *
 * @author rink
 */

 
public class DataSourceInfo {
   public String description;
   public String dateTimeStamp;
   public int dataSourceId = -1;
   
   public DataSourceInfo() {
   }
   
   public DataSourceInfo(String description, String dateTimeStamp, int dataSourceId) {
      this.description = description;
      this.dateTimeStamp = dateTimeStamp;
      this.dataSourceId = dataSourceId;
   }
   
   public DataSourceInfo(String description, String dateTimeStamp) {
      this.description = description;
      this.dateTimeStamp = dateTimeStamp;
   }
   
   public DataSourceInfo(String dateTimeStamp) {
      this.dateTimeStamp = dateTimeStamp;
   }
   
   public DataSourceInfo clone() {
      return new DataSourceInfo(this.description, this.dateTimeStamp, this.dataSourceId);
   }
}
