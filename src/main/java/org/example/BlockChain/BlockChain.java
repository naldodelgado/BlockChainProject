package org.example.BlockChain;

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
    public void store(Byte[] data) {
        Block a = parseBlock(data);

        if (!verify(a)) return;

        //TODO: store block

        boolean b = miner.getBlock().getTransactions().stream().map(t -> a.getTransactions().contains(t)).reduce(false, (a1, a2) -> a1 || a2); // complexity n^2

        if (b){
            miner.stopMining();
            transactions.addAll(miner.getBlock().getTransactions());
        }

        for (Transaction t : a.getTransactions()) {
            transactions.remove(t);
        }

        //start mining if there are enough transactions
        if (transactions.size() >= 5) {
            miner = new Miner(new Block(0));
        }

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

    public void addTransaction() {
        //add transaction to the transaction list

    }

    public void verifyTransaction(Transaction transaction) {
        //verify the transaction
    }

}
