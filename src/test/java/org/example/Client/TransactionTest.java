package org.example.Client;

import org.example.Utils.KeysManager;
import org.junit.Test;

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

        Bid a = new Bid(new byte[]{1, 2, 3}, key.getPublic(), key.getPublic(), 1, key.getPrivate());

        Bid b = (Bid) Transaction.fromGrpc(a.toGrpc());

        assert a.equals(b);
    }

}