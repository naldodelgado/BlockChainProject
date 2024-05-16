package org.example.Blockchain;

import org.example.Client.Auction;
import org.example.Client.Bid;
import org.example.Client.Transaction;
import org.example.Client.Wallet;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockChainTest {
    @Test
    public void testVerify(){
        BlockChain blockChain = new BlockChain();
        ArrayList<Transaction> t = new ArrayList<>();

        Wallet.setBlockchain(blockChain);
        Wallet wallet = new Wallet();

        Auction auction = new Auction(new byte[]{1, 2, 3}, 4, 5, 6, wallet);
        Bid bid = new Bid(new byte[]{1, 3, 4}, wallet.getPublicKey(), wallet.getPublicKey(), 11, wallet.getPrivateKey());

        assertTrue(bid.isValid());
        assertTrue(auction.isValid());

        t.add(auction);
        t.add(bid);

        Block block = new Block(0, 0, t);
        
        assertTrue(blockChain.verify(block, 1));
        //TODO: verify is not working as expected
    }
}