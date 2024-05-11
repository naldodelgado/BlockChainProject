package org.example.Blockchain;

import org.example.Client.Auction;
import org.example.Client.Bid;
import org.example.Client.Transaction;
import org.example.Utils.KeysManager;
import org.junit.Assert;
import org.junit.Test;

import java.security.PublicKey;
import java.util.ArrayList;

public class BlockTest {

    @Test
    public void testMerkleRoot() {
        PublicKey publicKey = KeysManager.generateKeys().getPublic();
        Auction auction = new Auction(new byte[]{1, 2, 3}, 4, 5, 6, publicKey.getEncoded(), new byte[]{7, 8, 9}, new byte[]{10, 11, 12});
        Auction auction2 = new Auction(new byte[]{1, 2, 5}, 3, 2, 1, publicKey.getEncoded(), new byte[]{7, 0, 9}, new byte[]{0, 11, 12});
        Bid bid = new Bid(new byte[]{1, 2, 4}, new byte[]{4, 5, 7}, new byte[]{7, 0, 9}, 11, new byte[]{101, 12, 13}, 1);
        Bid bid2 = new Bid(new byte[]{1, 2, 76}, new byte[]{4, 5, 6}, new byte[]{7, 8, 9}, 10, new byte[]{11, 12, 13}, 1);
        ArrayList<Transaction> t = new ArrayList<>();
        ArrayList<Transaction> t2 = new ArrayList<>();
        // Add transactions to the block 1 in some order
        t.add(auction);
        t.add(auction2);
        t.add(bid);
        t.add(bid2);
        // Add transactions to the block 2 in another order
        t2.add(auction);
        t2.add(auction2);
        t2.add(bid2);
        t2.add(bid);

        Block block = new Block(0, 0, t);
        Block block2 = new Block(0, 0, t2);

        byte[] merkleRoot = block.getMerkleRoot();
        System.out.println();
        byte[] merkleRoot2 = block2.getMerkleRoot();
        Assert.assertArrayEquals(merkleRoot, merkleRoot2);
    }
}