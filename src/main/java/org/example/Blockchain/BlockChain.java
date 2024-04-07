package org.example.Blockchain;

import org.example.Kamdelia.Kademlia;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockChain {

    private final List<Block> blocks; // limit 10 block at a time
    private final List<Transaction> transactions;
    private Miner miner;
    private final Kademlia kademlia;

    public BlockChain(Kademlia kademlia) {
        this.transactions = new ArrayList<>();
        this.blocks = new ArrayList<>();
        this.kademlia = kademlia;
    }

    public void propagateBlock(Block block) {
        synchronized (blocks){
            if (!Arrays.equals(block.getPreviousHash(), blocks.get(blocks.size() - 1).getHash())) {
                return;
            }
            blocks.add(block);
        }

        synchronized (transactions){
            if (transactions.size() > 5){
                ArrayList<Transaction> t = new ArrayList<>(transactions.subList(0,5));
                transactions.removeAll(t);
                miner = new Miner(new Block(0, System.currentTimeMillis(), t),this);
            }
        }

        this.kademlia.propagate(block);
    }

    // called by the network
    public synchronized boolean store(Block data) {
        if (!verify(data)) return false;

        synchronized (blocks){
            blocks.add(data);
            //TODO: store block if the list is full
        }

        boolean b = miner.getBlock().getTransactions().stream().map(t -> data.getTransactions().contains(t)).reduce(false, (a1, a2) -> a1 || a2); // complexity n^2

        if (b){
            miner.stopMining();
            synchronized (transactions){
                transactions.addAll(miner.getBlock().getTransactions());
            }
        }

        synchronized (transactions){
            transactions.removeAll(data.getTransactions());
        }
        
        //start mining if there are enough transactions
        synchronized (transactions){
            if (transactions.size() >= 5) {
                miner = new Miner(new Block(0),this);
            }
        }

        return true;
    }

    private boolean verify(Block block) {
        //are the transactions valid?
        for(Transaction t : block.getTransactions()) {
            if (!verifyTransaction(t)) {
                return false;
            }
        }
        /* Does this block claim that he is the successor to the last block we have in memory?
        * If not, we can just discard it because we already have a successor*/
        synchronized (blocks){
            if (!blocks.isEmpty() && !Arrays.equals(blocks.get(blocks.size() - 1).getHash(), block.getPreviousHash())) {
                return false;
            }
        }

        /*TODO
        *  Verify extended forking : refer to: https://developer.bitcoin.org/devguide/block_chain.html#id1*/
        /* Check if the block is really the successor to the previous block by hashing it again and checking the result*/
        return Arrays.equals(block.getHash(), block.calculateHash());
    }

    public synchronized boolean addTransaction(Transaction transaction) {
        if (!verifyTransaction(transaction)) {
            return false;
        }
        synchronized (transactions){
            transactions.add(transaction);
        }
        return true;
    }

    public boolean verifyTransaction(Transaction transaction) {
        //is the signature valid?

        //is the sender's balance enough?
        //TODO: implement this method
        throw new UnsupportedOperationException("Not implemented yet");
    }

}