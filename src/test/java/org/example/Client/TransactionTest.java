package org.example.Client;

import org.example.Utils.KeysManager;
import org.junit.Test;

import java.util.Arrays;

public class TransactionTest {

    @Test
    public void TestToAuctionGrpc() {
        var key = KeysManager.generateKeys();

        Auction a = new Auction(new byte[]{1, 2, 3}, 1, 1, 1, key.getPublic(), new byte[]{1, 2, 3}, new byte[]{1, 2, 3});

        Auction b = (Auction) Transaction.fromGrpc(a.toGrpc());

        assert a.equals(b);
    }

    @Test
    public void TestToBidGrpc() {
        var key = KeysManager.generateKeys();

        var key2 = KeysManager.generateKeys();

        assert !Arrays.equals(key2.getPublic().getEncoded(), key.getPublic().getEncoded());

        Bid a = new Bid(new byte[]{1, 2, 3}, key.getPublic(), key2.getPublic(), 1, key.getPrivate());

        Bid b = (Bid) Transaction.fromGrpc(a.toGrpc());

        System.out.println(a);

        System.out.println(b);

        assert a.equals(b);
    }

}