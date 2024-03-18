package org.example.poisson;

import java.util.Random;

/**
 * Poisson process random number generation.
 * 
 * @author Eduardo R. B. Marques
 *
 */
public final class PoissonProcess {

  /**
   * Rate parameter.
   */
  private final double lambda;
  
  /**
   * Random number generator to use.
   */
  private final Random rng;
  
  /**
   * Constructor.
   * @param lambda Rate parameter.
   * @param rng Base random number generator to use.
   */
  public PoissonProcess(double lambda, Random rng) {
    if (lambda <= 0d) {
      throw new IllegalArgumentException("Supplied rate parameter is not positive: " + lambda);
    }
    if (rng == null) {
      throw new IllegalArgumentException("Null RNG argument");
    }
    this.lambda = lambda;
    this.rng = rng;
  }
  
  /**
   * Get rate parameter.
   * @return The RNG in use.
   */
  public double getLambda() {
    return lambda;
  }
  
  /**
   * Get random number generator.
   * @return The RNG in use.
   */
  public Random getRNG() {
    return rng;
  }
  
  /**
   * Get time for next event. 
   * 
   * @return A random inter-arrival time.
   */
  public double timeForNextEvent() {
    // The sequence of inter-arrival times are independent and have an exponential distribution with mean 1/lambda.
    // To generate it we use the recipe in https://en.wikipedia.org/wiki/Exponential_distribution#Generating_exponential_variates
    return - Math.log(1.0 - rng.nextDouble()) / lambda;
  }
  
  /**
   * Get number of events in an unit of time.
   * The call is shorthand for <code>events(1.0)</code>.
   * @return Number of events.
   */
  public int events() {
    return events(1d);
  }
  
  /**
   * Get number of occurrences in time t (assumed to be relative to the unit time).
   * @param time Length of time interval.
   */
  public int events(double time) {
    // The algorithm based on inverse transform sampling is used -- see:
    // https://en.wikipedia.org/wiki/Poisson_distribution#Generating_Poisson-distributed_random_variables
    int n = 0;
    double p = Math.exp(-lambda * time);
    double s = p;
    double u = rng.nextDouble();
    while (u > s) {
      n = n + 1;
      p = p * lambda / n;
      s = s + p;
    }    
    return n;
  }
  
}
