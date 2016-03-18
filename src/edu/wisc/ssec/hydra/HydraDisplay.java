package edu.wisc.ssec.hydra;

import java.awt.event.WindowListener;
import java.awt.event.WindowEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseEvent;
import java.awt.Dimension;

import visad.MouseBehavior;


public abstract class HydraDisplay implements WindowListener, ComponentListener, MouseListener {

     boolean gotFirstResizeEvent = false;

     // TODO:  need a manager for this someday
     public static Dimension sharedWindowSize = new Dimension(500, 500);

     public void windowClosing(WindowEvent e) {
     }
     public void windowDeactivated(WindowEvent e) {
     }
     public void windowActivated(WindowEvent e) {
     }
     public void windowDeiconified(WindowEvent e) {
     }
     public void windowIconified(WindowEvent e) {
     }
     public void windowOpened(WindowEvent e) {
     }
     public void windowClosed(WindowEvent e) {
     }

     public void componentHidden(ComponentEvent e) {
     }

     public void componentMoved(ComponentEvent e) {
     }

     public void componentResized(ComponentEvent e) {
        if (!gotFirstResizeEvent) gotFirstResizeEvent = true;
     }

     public void componentShown(ComponentEvent e) {
     }

     public void mouseClicked(MouseEvent e) {
     }

     public void mouseEntered(MouseEvent e) {
     }

     public void mouseExited(MouseEvent e) {
     }

     public void mousePressed(MouseEvent e) {
     }

     public void mouseReleased(MouseEvent e) {
     }


     public static double[] getTranslation(MouseBehavior mouseBehav, double[] matrix) {
        double[] rot_a = new double[3];
        double[] scale_a = new double[3];
        double[] trans_a = new double[3];
        mouseBehav.instance_unmake_matrix(rot_a, scale_a, trans_a, matrix);
        return trans_a;
     }

}
