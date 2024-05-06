package org.example;

import org.example.Blockchain.BlockChain;
import org.example.Client.Wallet;
import org.example.Utils.FileSystem;
import org.example.Utils.KeysManager;
import org.example.Utils.LogFilter;
import org.example.poisson.PoissonProcess;

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
    static BlockChain blockChain = new BlockChain();
    static FileSystem fileSystem;

    public static void main(String[] args) {
        logger.setFilter(new LogFilter());
        fileSystem = new FileSystem();

        Wallet.setBlockchain(blockChain);

        for (int i = 0; i < 10; i++) {
            wallets.add(new Wallet());
        }

        int time = (int) (10 + Math.random() * 30);
        logger.info(String.format("scheduled auction in %d seconds", time));
        executor.schedule(Main::auctionStarter, time, TimeUnit.SECONDS);

    }

    public static void auctionStarter() {
        logger.info("generating auction");

        var wallet = wallets.get((int) (Math.random() * 10));

        blockChain.addTransaction(wallet.startAuction(
                KeysManager.hash(new Object[]{wallet, Math.random()}),
                100,
                10,
                System.currentTimeMillis() + 1_000_000
        ));

        long time = (long) (auctionTimer.timeForNextEvent() * 1000);
        logger.info(String.format("scheduled auction in %d seconds", time));
        executor.schedule(Main::auctionStarter, time, TimeUnit.SECONDS);
    }

}