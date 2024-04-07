package org.example.Blockchain;

import org.example.Kamdelia.Kademlia;

import java.util.ArrayList;
import java.util.List;

public class BlockChain {

    private final List<Block> blocks; // limit 10 block at a time
    private final List<Transaction> transactions;
    private Miner miner;
    private final Kademlia kademlia;

    public BlockChain(Kademlia kademlia) {
        this.transactions = new ArrayList<Transaction>();
        this.blocks = new ArrayList<Block>();
        this.kademlia = kademlia;
    }

    // called by the network
    public boolean store(Block data) {
        if (!verify(data)) return false;

        //TODO: store block

        boolean b = miner.getBlock().getTransactions().stream().map(t -> data.getTransactions().contains(t)).reduce(false, (a1, a2) -> a1 || a2); // complexity n^2

        if (b){
            miner.stopMining();
            transactions.addAll(miner.getBlock().getTransactions());
        }

        for (Transaction t : data.getTransactions()) {
            transactions.remove(t);
        }

        //start mining if there are enough transactions
        if (transactions.size() >= 5) {
            miner = new Miner(new Block(0));
        }

        return true;
    }

    public boolean verify(Block block) {
        //are the transactions valid?

        //is the previous hash correct?

        //is the hash valid?

        return true;
    }

    public Block parseBlock(Byte[] data) {
        return null;
    }

    public Block getBlock() {
        return null;
    }

    public boolean addTransaction(Transaction transaction) {
        if (!verifyTransaction(transaction)) {
            return false;
        }

        transactions.add(transaction);

        return true;
    }

    public boolean verifyTransaction(Transaction transaction) {
        //is the signature valid?
        //is the sender's balance enough?
        //TODO: implement this method
        throw new UnsupportedOperationException("Not implemented yet");
    }

}
