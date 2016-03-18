package edu.wisc.ssec.hydra;

  public  class NodeInfo {
       public String name;
       public String desc;
       public Object source;

       public NodeInfo(Object source, String name, String desc) {
          this.name = name;
          this.desc = desc;
          if (desc == null) {
             this.desc = name;
          }
          else {
             this.desc = name+" "+desc;
          }
          this.source = source;
       }

       public NodeInfo(Object source, String name) {
          this(source, name, null);
       }

       public String toString() {
          return desc;
       }
  }

