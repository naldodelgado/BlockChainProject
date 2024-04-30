package org.example;

import org.example.Blockchain.BlockChain;
import org.example.Client.Auction;
import org.example.Client.Wallet;
import org.example.Kamdelia.Kademlia;
import org.example.Utils.KeysManager;
import org.example.Utils.LogFilter;
import org.example.poisson.PoissonProcess;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class Main {

    static List<Wallet> wallets = new ArrayList<>(10);
    static PoissonProcess auctionTimer = new PoissonProcess(16, new Random((int) (Math.random() * 1000)));
    static PoissonProcess bidder = new PoissonProcess(16, new Random((int) (Math.random() * 1000)));
    static ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);
    static Logger logger = Logger.getLogger(Main.class.getName());
    static Kademlia kademlia;

    public static void main(String[] args) {
        logger.setFilter(new LogFilter());
        try {
            kademlia = new Kademlia(5000);

            BlockChain blockChain = new BlockChain(kademlia);

            // Can only be called after the blockChain is initialized
            kademlia.start();

            for (int i = 0; i < 10; i++) {
                wallets.add(new Wallet(blockChain));
            }

            int time = (int) (auctionTimer.timeForNextEvent() * 1000);
            logger.info(String.format("scheduled auction in %d seconds", time));
            executor.schedule(Main::actionStarter, time, TimeUnit.SECONDS);
            //executor.schedule(Main::bidder, (long) bidder.timeForNextEvent() * 1000, TimeUnit.SECONDS);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void actionStarter() {
        logger.info("generating auction");
        Auction auction = wallets.get((int) (Math.random() * 10))
                .startAuction(
                        KeysManager.hash(new Object[]{wallets, Math.random()}),
                        100,
                        10,
                        System.currentTimeMillis() + 1_000_000
                );

        logger.info("propagating the auction");

        kademlia.propagate(auction);

        long time = (long) (auctionTimer.timeForNextEvent() * 1000);
        logger.info(String.format("scheduled auction in %d seconds", time));
        executor.schedule(Main::actionStarter, time, TimeUnit.SECONDS);
    }

//    public static void bidder(){
//        wallets.get((int) Math.random())
//                .bid();
//    }
}