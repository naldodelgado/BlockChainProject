package org.example.Blockchain;

import kademlia_public_ledger.Type;
import org.apache.commons.lang3.tuple.Pair;
import org.example.Blockchain.Kademlia.Kademlia;
import org.example.Client.Auction;
import org.example.Client.Bid;
import org.example.Client.Transaction;
import org.example.Client.Wallet;
import org.example.Utils.FileSystem;
import org.example.Utils.KeysManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class BlockChain {
    private final List<Block> blocks; // limit 10 block at a time
    private final List<Transaction> transactions;
    private Miner miner;
    private final Kademlia kademlia;
    private final Executor threads = Executors.newScheduledThreadPool(1);
    private List<Pair<Auction, Bid>> activeBids;
    private final Block genesisBlock;
    private final Logger logger = Logger.getLogger(BlockChain.class.getName());
    public static Map<byte[], List<Wallet>> mapPkTransaction = new HashMap<>();
    static int numZeros = 0; // Number of zeros required at the start of the hash

    // List of timestamps of the last N blocks
    private static final LinkedList<Long> lastBlockTimestamps = new LinkedList<>();
    // Number of last blocks to consider when adjusting difficulty
    private static final int N = 10;
    // Target time to mine a block in milliseconds
    private static final long TARGET_TIME = 60000; // 1 minute

    public BlockChain() {
        try {
            Files.createDirectories(Paths.get(FileSystem.blockchainPath));
            Files.createDirectories(Paths.get(FileSystem.auctionPath));
            Files.createDirectories(Paths.get(FileSystem.bidPath));
            Files.createDirectories(Paths.get(FileSystem.UtilsPath));
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
            throw new RuntimeException();
        }
        kademlia = new Kademlia(5000, this);
        this.transactions = new ArrayList<>();
        this.blocks = new ArrayList<>();
        genesisBlock = new Block();
        this.blocks.add(genesisBlock);
        genesisBlock.store();
        kademlia.start();
    }

    //called by the miner once it mines a block
    void propagateBlock(Block block) {

        synchronized (blocks){
            if (!Arrays.equals(block.getPreviousHash(), blocks.get(blocks.size() - 1).getHash())) {
                startMining();
                return;
            }
            kademlia.propagate(block); // this call is asynchronous

            if (blocks.size() > 10) blocks.remove(0);

            blocks.add(block);
        }

        block.store();

        for (var a : block.getTransactions()) a.store();

        startMining();

        this.kademlia.propagate(block);
    }

    public static void adjustDifficulty(long timestamp) {
        // Add timestamp of this block to the list
        lastBlockTimestamps.addLast(timestamp);
        // If list size exceeds N, remove the oldest timestamp
        if (lastBlockTimestamps.size() > N) {
            lastBlockTimestamps.removeFirst();
        }

        // If we have mined N blocks, adjust difficulty
        if (lastBlockTimestamps.size() == N) {
            // Calculate average time to mine a block
            long totalDuration = 0;
            for (int i = 1; i < lastBlockTimestamps.size(); i++) {
                long duration = lastBlockTimestamps.get(i) - lastBlockTimestamps.get(i - 1);
                totalDuration += duration;
            }
            long averageTime = totalDuration / (N - 1);

            // Adjust difficulty based on average time
            if (averageTime < TARGET_TIME) {
                increaseDifficulty();
            } else {
                decreaseDifficulty();
            }
        }
    }

    public static void increaseDifficulty() {
        numZeros++;
    }

    public static void decreaseDifficulty() {
        if(numZeros > 0)
            numZeros--;
    }

    private void startMining() {
        ArrayList<Transaction> t;
        synchronized (transactions) {
            if (transactions.size() >= Block.TRANSACTION_PER_BLOCK) {
                t = new ArrayList<>(transactions.subList(0, Block.TRANSACTION_PER_BLOCK));
                transactions.removeAll(t);
            } else return;
        }

        Block block;
        synchronized (blocks) {
            block = new Block(0, System.currentTimeMillis(), t, blocks.get(blocks.size() - 1).getHash(), blocks.get(blocks.size() - 1).getNumberOfOrder() + 1);
        }
        logger.info("Generated block:\n" + block);
        miner = new Miner(block, this);
        new Thread(miner).start();
    }

    public boolean addBlock(Block block) {
        // combined functions (addBlock + verify)
        for (Transaction t : block.getTransactions()) {
            if (!t.isValid()) {
                logger.info(String.format("Block with hash %s has invalid transactions", KeysManager.hexString(block.getHash())));
                return false;
            }
        }

        if (!block.isValid()) {
            logger.info(String.format("Block with hash %s is invalid", KeysManager.hexString(block.getHash())));
            return false;
        }

        if (block.getNumberOfOrder() == 1 && Arrays.equals(block.getPreviousHash(), genesisBlock.getHash())) {
            blocks.add(block);
        }

        synchronized (blocks) {
            int index = block.getNumberOfOrder() - 1 - blocks.get(0).getNumberOfOrder(); // the first one in the list

            if(index < 0) return false;
            if(index > blocks.size()){
                Optional<Block> loadedb = Block.load(block.getPreviousHash());
                if (loadedb.isEmpty() || block.getNumberOfOrder() - loadedb.get().getNumberOfOrder() != 1)
                    return false;

                Optional<Block> b = kademlia.getBlock(block.getPreviousHash());
                if (b.isEmpty() || block.getNumberOfOrder() - b.get().getNumberOfOrder() != 1) return false;
                if (!addBlock(b.get())) return false;

                index = block.getNumberOfOrder() - 1 - blocks.get(0).getNumberOfOrder();
            }

            if (!Arrays.equals(block.getPreviousHash(), blocks.get(index).getHash())) {
                return false;
            }

            if (index + 1 < blocks.size() - 1) {
                blocks.set(index + 1, block);
                removeFromIndex(blocks, index + 2);
            } else {
                blocks.add(block);
                blocks.remove(0);
            }
        }

        block.store();

        for (var a : block.getTransactions()) {
            a.store();
        }

        boolean b = miner.getBlock()
                .getTransactions()
                .stream()
                .map(t -> block.getTransactions().contains(t))
                .reduce(false, (a1, a2) -> a1 || a2); // complexity n^2

        if (!b) {
            return true;
        }

        miner.stopMining();

        synchronized (transactions) {
            transactions.addAll(miner.getBlock().getTransactions());
        }

        synchronized (transactions) {
            transactions.removeAll(block.getTransactions());
        }

        startMining();

        return true;
    }

    public static void removeFromIndex(List<?> list, int index) {
        if (list == null || index < 0 || index >= list.size()) {
            throw new IndexOutOfBoundsException("Index is out of range");
        }

        list.subList(index, list.size()).clear();
    }

    public void addTransaction(Transaction transaction) {
        logger.info(String.format("received transaction %s", transaction));
        if (!transaction.isValid()) {
            return;
        }

        synchronized (transactions){
            transactions.add(transaction);
        }

        new Thread(() -> kademlia.propagate(transaction)).start();

        if (miner == null) startMining();

    }

    public boolean addPropagatedTransaction(Transaction transaction) {
        logger.info(String.format("received transaction %s", transaction));

        if (!transaction.isValid()) return false;

        synchronized (transactions) {
            transactions.add(transaction);
        }

        //check if the current transaction is present on mapPkTransaction and if present I want to alert the wallet that a new auction has been made there
        if (mapPkTransaction.containsKey(transaction.getAuctionHash()) && transaction.getType() == Type.bid) {
            for (Wallet w : mapPkTransaction.get(transaction.getAuctionHash())) {
                w.alert(transaction);
            }
        }

        if (miner == null) startMining();

        return true;
    }

    public static void subscribe(Wallet wallet, Auction auction) {
        if (mapPkTransaction.get(auction.getHash()) == null) {
            List<Wallet> w = new ArrayList<>();
            w.add(wallet);
            mapPkTransaction.put(auction.getHash(), w);
        }
        mapPkTransaction.get(auction.getHash()).add(wallet);
    }
}
