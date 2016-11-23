package edu.wisc.ssec.hydra;

/**
 *
 * @author rink
 */
public class DatasetInfo {
   public String name;
   public float nadirResolution = Float.NaN;
   public DataSourceInfo datSrcInfo;

   public DatasetInfo(String name, DataSourceInfo datSrcInfo) {
      this.name = name;
      this.datSrcInfo = datSrcInfo;
   }
   
   public DatasetInfo(String name, float nadirResolution, DataSourceInfo datSrcInfo) {
      this.name = name;
      this.nadirResolution = nadirResolution;
      this.datSrcInfo = datSrcInfo;
   }
   
   public DatasetInfo(String name) {
      this.name = name;
   }
   
   public DatasetInfo clone() {
      return new DatasetInfo(this.name, this.nadirResolution, datSrcInfo.clone());
   }
   
}
