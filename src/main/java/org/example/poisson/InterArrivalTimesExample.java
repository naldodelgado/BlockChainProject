package org.example.poisson;

import java.util.Random;
import java.util.Scanner;

public class InterArrivalTimesExample {

  public static void main(String[] args) {

    try (Scanner in = new Scanner(System.in)) {
      System.out.print("lambda ? ");
      double lambda = in.nextDouble();
      System.out.print("samples ? ");
      
      int N = in.nextInt();
      PoissonProcess pp = new PoissonProcess(lambda, new Random());
      SampleValues sv = new SampleValues("example");
      
      for (int i = 1; i <= N; i++) {
        double t = pp.timeForNextEvent();
        sv.add(t);
        System.out.printf("%6d: %9.5f%n", i, t);
      }
      
      System.out.printf("sample mean: %9.5f -- dist. mean: %9.5f%n", sv.mean(), 1d / lambda);
      System.out.printf("sample var:  %9.5f -- dist var. : %9.5f%n", sv.variance(), 1d / (lambda*lambda));
    }
  }

}
