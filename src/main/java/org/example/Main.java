package org.example;

import org.example.Blockchain.Block;
import org.example.Blockchain.BlockChain;
import org.example.Client.Wallet;
import org.example.CryptoUtils.KeysManager;
import org.example.Kamdelia.Kademlia;
import org.example.poisson.PoissonProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Main {

    static List<Wallet> wallets = new ArrayList<>(10);
    static PoissonProcess auctionTimer = new PoissonProcess(4, new Random((int) (Math.random() * 1000)));
    static PoissonProcess bidder = new PoissonProcess(16, new Random((int) (Math.random() * 1000)));
    static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    static Kademlia kademlia;

    public static void main(String[] args) {
        try {
            kademlia = new Kademlia(5000);

            BlockChain blockChain = new BlockChain(kademlia);

            // Can only be called after the blockChain is initialized
            kademlia.start();

            Scanner scanner = new Scanner(System.in);

            for (int i = 0; i < 10; i++) {
                wallets.add(new Wallet(blockChain));
            }

            executor.schedule(Main::actionStarter, (long) (auctionTimer.timeForNextEvent() * 1000), TimeUnit.SECONDS);
//            executor.schedule(Main::bidder, (long) bidder.timeForNextEvent() * 1000, TimeUnit.SECONDS);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void actionStarter() {
        wallets.get((int) (Math.random() * 10))
                .startAuction(
                        KeysManager.hash(new Object[]{wallets, Math.random()}),
                        100,
                        10,
                        System.currentTimeMillis() + 1_000_000
                );

        kademlia.propagate(new Block(0));

        long time = (long) (auctionTimer.timeForNextEvent() * 1000);

        executor.schedule(Main::actionStarter, time, TimeUnit.SECONDS);

        System.out.println("schedule for auction in " + time + " seconds");
    }

//    public static void bidder(){
//        wallets.get((int) Math.random())
//                .bid();
//    }
}