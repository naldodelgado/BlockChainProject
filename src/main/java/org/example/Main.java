package org.example;

import org.example.Blockchain.Block;
import org.example.Blockchain.BlockChain;
import org.example.Kamdelia.Kademlia;
import org.example.poisson.PoissonProcess;
import org.example.Blockchain.Transaction;

import java.io.IOException;
import java.util.Random;

public class Main {
    public static void main(String[] args) {
        try {
            Kademlia kademlia = new Kademlia(5000);

            BlockChain blockChain = new BlockChain(kademlia);

            kademlia.setBlockStorageFunction((t) -> blockChain.store(Block.fromGrpc(t)));
            kademlia.setTransactionStorageFunction((t) -> blockChain.addTransaction(Transaction.fromGrpc(t)));

            kademlia.start();

            while (true) {
                PoissonProcess pp = new PoissonProcess(4, new Random((int) (Math.random() * 1000)));
                double t = pp.timeForNextEvent() * 60.0 * 1000.0;

                Thread.sleep((int) t);

                System.out.println("Poisson Process Event");

                kademlia.propagate(new Block(0));

            }

        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}