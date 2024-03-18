package org.example;

import org.example.Kamdelia.Kademlia;
import org.example.poisson.PoissonProcess;

import java.io.IOException;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        try {
            Kademlia kademlia = new Kademlia(5000);
            kademlia.start();

            while (true) {
                PoissonProcess pp = new PoissonProcess(4, new Random((int) (Math.random() * 1000)));
                double t = pp.timeForNextEvent() * 60.0 * 1000.0;

                Thread.sleep((int) t);

                System.out.println("Poisson Process Event");
                kademlia.store(("Hello World" + System.currentTimeMillis()).getBytes());
            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}