package org.example.poisson;

public class SampleValues {

  private final String id;
  private int count;
  private double sum, sumSq, min, max;
  
  SampleValues(String statsId) {
    id = statsId;
    count = 0;
    sum = 0;
    sumSq = 0;
    min = Double.MAX_VALUE;
    max = Double.MIN_VALUE;

  }
  
  public void add(double v) {
    count++;
    sum += v;
    sumSq += (v * v);
    min = Math.min(v, min);
    max = Math.max(v, max);
  }
  
  public String id() {
    return id;
  }
  
  public double min() { 
    return min; 
  }
  
  public double max() { 
    return max; 
  }
  
  public int count() { 
    return count; 
  }
  
  public double mean() { 
    return sum / count; 
  }
  
  public double stddev() { 
    return Math.sqrt(variance());
  }
   
  public double variance() { 
    double u = mean();
    return u * u + (sumSq - 2 * u * sum)/count;
  }
  
  @Override
  public String toString() {
    return String.format("%s|count=%d|avg=%f|variance=%f|min=%f|max=%f",
                         id(), count(), mean(), variance(), min(), max());
  }
  
  public void mergeWith(SampleValues other) {
    count += other.count;
    sum += other.sum;
    sumSq += other.sumSq;
    min = Math.min(min, other.min);
    max = Math.max(max, other.max);
  }
  
}
