package org.example.Blockchain;

import org.example.Client.Auction;
import org.example.Client.Bid;
import org.example.Client.Transaction;
import org.example.Utils.KeysManager;
import org.junit.Test;

import java.security.PublicKey;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BlockChainTest {
    @Test
    public void testVerify(){
        BlockChain blockChain = new BlockChain();
        ArrayList<Transaction> t = new ArrayList<>();
        PublicKey publicKey = KeysManager.generateKeys().getPublic();
        Auction auction = new Auction(new byte[]{1, 2, 3}, 4, 5, 6, publicKey.getEncoded(), new byte[]{7, 8, 9}, new byte[]{10, 11, 12});
        t.add(auction);
        Bid bid = new Bid(new byte[]{1, 2, 4}, new byte[]{4, 5, 7}, new byte[]{7, 0, 9}, 11, new byte[]{101, 12, 13});
        t.add(bid);
        Block block = new Block(0, 0, t);
        Bid bid2 = new Bid(new byte[]{1, 2, 4}, new byte[]{4, 5, 7}, new byte[]{7, 0, 9}, 11, new byte[]{101, 12, 13});
        Bid bid3 = new Bid(new byte[]{1, 3, 4}, new byte[]{4, 5, 7}, new byte[]{7, 0, 9}, 11, new byte[]{101, 12, 13});
        t.add(bid2);
        t.add(bid3);
        Block block2 = new Block(1, 1, t);
        blockChain.addBlock(block);
        blockChain.addBlock(block2);
        assertTrue(blockChain.verify(block2));
        //TODO: verify is not working as expected
    }
}