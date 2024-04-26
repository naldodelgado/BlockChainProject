package org.example.Blockchain;

import org.apache.commons.lang3.tuple.Pair;
import org.example.Client.Auction;
import org.example.Client.Bid;
import org.example.Client.Transaction;
import org.example.Kamdelia.Kademlia;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class BlockChain {
    private final List<Block> blocks; // limit 10 block at a time
    private final List<Transaction> transactions;
    private Miner miner;
    private final Kademlia kademlia;
    private List<Pair<Auction, Bid>> activeBids;

    public BlockChain(Kademlia kademlia) {
        kademlia.setBlockStorageFunction((t) -> store(Block.fromGrpc(t)));
        kademlia.setTransactionStorageFunction((t) -> addTransaction(Transaction.fromGrpc(t)));

        kademlia.setBlockStorageGetter((hash) -> {
            String filePath = "Blocks/" + hash + ".block";
            try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
                int nonce = Integer.parseInt(reader.readLine().trim());
                long timestamp = Long.parseLong(reader.readLine().trim());
                ArrayList<Transaction> transactions = new ArrayList<>();

                String line;

                while ((line = reader.readLine()) != null) {

                    String transactionFilePath = "Transactions/" + line.trim() + ".transaction";
                    BufferedReader transactionReader = new BufferedReader(new FileReader(transactionFilePath));

                    Transaction transaction = Transaction.fromStorage(line.trim());
                    transactions.add(transaction);
                    transactionReader.close(); // Close file reader to avoid resource leak
                }

                // Create and return a new Block object
                return Optional.of(new Block(nonce, timestamp, transactions));
            } catch (IOException e) {
                System.err.println("Error reading from file: " + e.getMessage());
                return Optional.empty();
            }
        });
        kademlia.setTransactionStorageGetter((t) -> Optional.empty());
        this.transactions = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.kademlia = kademlia;
    }

    public void propagateBlock(Block block) {
        synchronized (blocks){
            if (!Arrays.equals(block.getPreviousHash(), blocks.get(blocks.size() - 1).getHash())) {
                return;
            }
            kademlia.propagate(block); // this call is asynchronous

            if (blocks.size() > 10) {
                blocks.remove(0).store();
            }

            blocks.add(block);

            synchronized (transactions) {
                if (transactions.size() >= Block.TRANSACTION_PER_BLOCK) {
                    miner = new Miner(new Block(0), this);

                    miner.getBlock().getTransactions().addAll(transactions.subList(0, Block.TRANSACTION_PER_BLOCK));

                    transactions.subList(0, Block.TRANSACTION_PER_BLOCK).clear();

                    miner.run();
                }
            }

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
                block.setPreviousHash(transactions.getLast().hash());

                miner = new Miner(block, this);
            }
        }
    }

    // called by the network
    public boolean store(Block data) {
        if (!verify(data)) return false;

        synchronized (blocks){
            if (!blocks.isEmpty()) {
                if (Arrays.equals(data.getPreviousHash(), blocks.get(blocks.size() - 1).getHash())) {
                    blocks.add(data);
                } else if (Arrays.equals(data.getPreviousHash(), blocks.get(blocks.size() - 2).getHash())){
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

        synchronized (transactions){
            transactions.removeAll(data.getTransactions());
        }

        startMining();

        return true;
    }

    private boolean verify(Block block) {
        //are the transactions valid?
        for (Transaction t : block.getTransactions()) {
            if (t.verify()) {
                return false;
            }
        }

        synchronized (blocks){
            if (!blocks.isEmpty()) {

                //is the previous hash correct?
                if (Arrays.equals(block.getPreviousHash(), blocks.get(blocks.size() - 1).getHash())) {
                    if (block.getTimestamp() < blocks.get(blocks.size() - 1).getTimestamp())
                        return false;
                } else if (Arrays.equals(block.getPreviousHash(), blocks.get(blocks.size() - 2).getHash())){
                    // forking
                    if (block.getTimestamp() < blocks.get(blocks.size() - 2).getTimestamp())
                        return false;
                } else {
                    // the previous hash does not match any of the last two blocks
                    return false;
                }

                //is the merkle root correct?
                if (!Arrays.equals(block.calculateMerkleRoot(), block.getMerkleRoot()))
                    return false;

                //is the nonce valid?
                if (block.isNonceValid())
                    return false;

                //is the hash correct?
                return Arrays.equals(block.getHash(), block.calculateHash());
            }
        }

        return Arrays.equals(block.getHash(), block.calculateHash());
    }

    public boolean addTransaction(Transaction transaction) {
        if (!transaction.verify()) {
            return false;
        }
        synchronized (transactions){
            transactions.add(transaction);
        }
        return true;
    }


}
