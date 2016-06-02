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
public class DataGroup {
   
   String name;
   
   public DataGroup(String name) {
      this.name = name;
   }
   
   public boolean equals(Object obj) {
      if (obj instanceof DataGroup) {
         if ( ((DataGroup)obj).equals(this.name)) {
            return true;
         }
      }
      return false;
   }
   
}
