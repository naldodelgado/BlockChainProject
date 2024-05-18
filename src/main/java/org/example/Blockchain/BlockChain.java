package org.example.Blockchain;

import org.apache.commons.lang3.tuple.Pair;
import org.example.Blockchain.Kademlia.Kademlia;
import org.example.Client.Auction;
import org.example.Client.Bid;
import org.example.Client.Transaction;
import org.example.Utils.FileSystem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class BlockChain {
    private final List<Block> blocks; // limit 10 block at a time
    private final List<Transaction> transactions;
    private Miner miner;
    private final Kademlia kademlia;
    private final Executor threads = Executors.newScheduledThreadPool(1);
    private List<Pair<Auction, Bid>> activeBids;
    private final Block genesisBlock;

    public BlockChain() {
        try {
            Files.createDirectories(Paths.get(FileSystem.blockchainPath));
            Files.createDirectories(Paths.get(FileSystem.auctionPath));
            Files.createDirectories(Paths.get(FileSystem.bidPath));
            Files.createDirectories(Paths.get(FileSystem.UtilsPath));
        } catch (IOException e) {
            System.err.println("Error creating directories: " + e.getMessage());
        }
        kademlia = new Kademlia(5000, this);
        this.transactions = new ArrayList<>();
        this.blocks = new ArrayList<>();
        genesisBlock = new Block();
        this.blocks.add(genesisBlock);
        kademlia.start();
    }

    //called by the miner once it mines a block
    void propagateBlock(Block block) {
        synchronized (blocks){
            if (!Arrays.equals(block.getPreviousHash(), blocks.get(blocks.size() - 1).getHash())) {
                return;
            }
            kademlia.propagate(block); // this call is asynchronous

            if (blocks.size() > 10) {
                blocks.get(0).store();
                blocks.remove(0);
            }

            blocks.add(block);

            startMining();
        }

        startMining();

        this.kademlia.propagate(block);
    }

    private void startMining() {
        synchronized (transactions) {
            if (transactions.size() > Block.TRANSACTION_PER_BLOCK) {
                ArrayList<Transaction> t = new ArrayList<>(transactions.subList(0, Block.TRANSACTION_PER_BLOCK));
                transactions.removeAll(t);

                Block block = new Block(0, System.currentTimeMillis(), t);
                if (!blocks.isEmpty()) {
                    transactions.get(transactions.size() - 1).hash();
                }

                miner = new Miner(block, this);
                new Thread(miner).start();
            } else {
                miner = null;
            }
        }
    }

    public boolean addBlock(Block block) {

        // combined functions (addBlock + verify)
        for (Transaction t : block.getTransactions()) {
            if (!t.isValid()) {
                return false;
            }
        }
        block.isValid();

        if (block.getNumberOfOrder() == 1 && Arrays.equals(block.getPreviousHash(), genesisBlock.getHash())) {
            blocks.add(block);
        }


        synchronized (blocks) {
            int index = block.getNumberOfOrder()-1 - blocks.get(0).getNumberOfOrder(); // the first one in the list

            if(index < 0) return false;
            if(index > blocks.size()){
                Optional<Block> loadedb = Block.load(block.getPreviousHash());
                if(loadedb.isEmpty() || Arrays.equals(block.getHash(), block.calculateHash()))
                    return false;

                Optional<Block> b = kademlia.getBlock(block.getPreviousHash());
                if (b.isEmpty() || block.getNumberOfOrder() - b.get().getNumberOfOrder() != 1) return false;
                if (addBlock(b.get())) return false;

                index = block.getNumberOfOrder()-1 - blocks.get(0).getNumberOfOrder();
            }

            if (Arrays.equals(block.getPreviousHash(), blocks.get(index).getHash())) {
                if (index < blocks.size() - 1) {
                    blocks.set(index + 1, block);
                } else {
                    blocks.add(block);
                    blocks.remove(0);
                }
            }
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

    public void addTransaction(Transaction transaction) {
        if (!transaction.isValid()) {
            return;
        }

        synchronized (transactions){
            transactions.add(transaction);
        }

        threads.execute(() -> kademlia.propagate(transaction));

        if (miner == null) startMining();

    }

    public boolean addPropagatedTransaction(Transaction transaction) {
        if (!transaction.isValid()) {
            return false;
        }

        synchronized (transactions) {
            transactions.add(transaction);
        }

        if (miner == null) startMining();

        return true;
    }


}
