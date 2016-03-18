package edu.wisc.ssec.hydra;

  public  class LeafInfo {
       public String name;
       public String desc;
       public int index;
       public Object source;

       public LeafInfo(Object source, String name, String desc, int index) {
          this.name = name;
          this.desc = desc;
          if (desc == null) {
             this.desc = name;
          }
          else {
             this.desc = name+" "+desc;
          }
          this.index = index;
          this.source = source;
       }

       public LeafInfo(Object source, String name, int index) {
          this(source, name, null, index);
       }

       public String toString() {
          return desc;
       }
  }

