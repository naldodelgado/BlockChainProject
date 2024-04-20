package org.example.Blockchain;

import org.example.Kamdelia.Kademlia;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BlockChain {

    private final List<Block> blocks; // limit 10 block at a time
    private final List<Transaction> transactions;
    private Miner miner;
    private final Kademlia kademlia;
    private final int TRANSACTION_PER_BLOCK = 5;

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
            if (transactions.size() > TRANSACTION_PER_BLOCK){
                ArrayList<Transaction> t = new ArrayList<>(transactions.subList(0,TRANSACTION_PER_BLOCK));
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
            if (!blocks.isEmpty()) {
                if (Arrays.equals(data.getPreviousHash(), blocks.get(blocks.size() - 1).getHash())) {
                    blocks.add(data);
                } else if (Arrays.equals(data.getPreviousHash(), blocks.get(blocks.size() - 2).getHash())){
                    if (data.getTimestamp() > blocks.get(blocks.size() - 2).getTimestamp())
                        blocks.add(data);
                }

                Arrays.compare(data.getPreviousHash(), blocks.get(blocks.size() - 1).getHash());
            }

        }

        boolean b = miner.getBlock()
                .getBids()
                .stream()
                .map(t -> data.getBids().contains(t))
                .reduce(false, (a1, a2) -> a1 || a2); // complexity n^2

        if (b){
            miner.stopMining();
            synchronized (transactions){
                transactions.addAll(miner.getBlock().getBids());
            }
        }

        synchronized (transactions){
            transactions.removeAll(data.getBids());
        }
        
        //start mining if there are enough transactions
        synchronized (transactions){
            if (transactions.size() >= TRANSACTION_PER_BLOCK) {
                miner = new Miner(new Block(0),this);
            }
        }

        return true;
    }

    private boolean verify(Block block) {
        //are the transactions valid?
        for(Transaction t : block.getBids()) {
            if (!verifyTransaction(t)) {
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

    private boolean verifyTransaction(Transaction transaction) {
        //is the signature valid?
        //TODO: verify the signature
        throw new UnsupportedOperationException("Not implemented yet");
    }

    private static void storeBlock(Block block) {
        File file = new File("blockchain/" + hexString( block.getHash()) + ".block");

        // TODO: deal with hash collision

        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(block);
            objectOutputStream.close();
            fileOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static String hexString(byte[] byteArray)
    {
        String hex = "";

        // Iterating through each byte in the array
        for (byte i : byteArray) {
            hex += String.format("%02X", i);
        }

        return hex;
    }

}
