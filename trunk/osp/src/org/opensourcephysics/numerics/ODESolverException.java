package org.opensourcephysics.numerics;
/**
 * Signals that a numerical error occured within an ODE solver.
 *
 * @author W. Christian and S. Tuleja
 * @version 1.0
 */
public class ODESolverException extends RuntimeException {
   public ODESolverException(){
      super("ODE solver failed.");
   }

   public ODESolverException(String msg) {
     super(msg);
   }

}

