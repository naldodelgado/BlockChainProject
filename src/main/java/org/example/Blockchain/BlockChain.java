package org.example.Blockchain;

import org.apache.commons.lang3.tuple.Pair;
import org.example.Blockchain.Kademlia.Kademlia;
import org.example.Client.Auction;
import org.example.Client.Bid;
import org.example.Client.Transaction;
import org.example.Utils.FileSystem;
import org.example.Utils.KeysManager;

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

    public boolean verify(Block block) {
        //are the transactions valid?
        for (Transaction t : block.getTransactions()) {
            if (!t.isValid()) {
                return false;
            }
        }

        synchronized (blocks){
            if (!blocks.isEmpty()) {
                for(int i = 1; i < 8; i++){ // checking if the current block is the successor of the last 8 blocks
                    if (Arrays.equals(block.getPreviousHash(), blocks.get(blocks.size() - i).getHash())){
                        if (block.getTimestamp() < blocks.get(blocks.size() - i).getTimestamp())
                            return false;
                        //is the merkle root correct?
                        if (!Arrays.equals(block.calculateMerkleRoot(), block.getMerkleRoot()))
                            return false;
                        //is the nonce valid?
                        if (block.isNonceValid())
                            return false;
                    } else {
                        // the previous hash does not match any of the last two blocks
                        return false;
                    }
                }
                //is the hash correct?
                return Arrays.equals(block.getHash(), block.calculateHash());
            }
        }

        return Arrays.equals(block.getHash(), block.calculateHash());
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

    public boolean addBlock(Block data) {
        if (!verify(data)) return false;

        synchronized (blocks) {
            if (!blocks.isEmpty()) {
                if (Arrays.equals(data.getPreviousHash(), blocks.get(blocks.size() - 1).getHash())) {
                    blocks.add(data);
                } else if (blocks.size() >= 2 && Arrays.equals(data.getPreviousHash(), blocks.get(blocks.size() - 2).getHash())) {
                    if (data.getTimestamp() > blocks.get(blocks.size() - 2).getTimestamp()) {
                        blocks.set(blocks.size() - 1, data);
                    }
                }
            }
        }

        boolean b = miner.getBlock()
                .getTransactions()
                .stream()
                .map(t -> data.getTransactions().contains(t))
                .reduce(false, (a1, a2) -> a1 || a2); // complexity n^2

        if (!b) {
            return true;
        }

        miner.stopMining();

        synchronized (transactions) {
            transactions.addAll(miner.getBlock().getTransactions());
        }

        synchronized (transactions) {
            transactions.removeAll(data.getTransactions());
        }

        startMining();

        return true;
    }

    public void uploadBlockchain(){
        try {
            Files.walk(Paths.get(FileSystem.blockchainPath))
                    .filter(Files::isRegularFile)
                    .forEach(file -> {
                        byte[] hash = KeysManager.getBytesFromHex(file.toString().split("\\.")[0]);
                        Optional<Block> block = Block.load(hash);
                        //TODO: use kademlia to send the block to the requester
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void downloadBlockChain() {
        // Request the blockchain from a neighbor
        List<Block> receivedBlockchain = kademlia.requestBlockchain();

        // Process each block in the received blockchain
        for (Block block : receivedBlockchain) {
            // Verify the block
            if (verify(block)) {
                // Add the block to the local blockchain
                addBlock(block);
            }
        }
    }


}
