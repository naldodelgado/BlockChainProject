package org.example.Client;

import org.junit.Test;

public class TransactionTest {

    @Test
    public void TestToAuctionGrpc() {
        Auction a = new Auction(new byte[]{1, 2, 3}, 1, 1, 1, new byte[]{1, 2, 3}, new byte[]{1, 2, 3}, new byte[]{1, 2, 3});

        Auction b = (Auction) Transaction.fromGrpc(a.toGrpc());

        assert a.equals(b);
    }

    @Test
    public void TestToBidGrpc() {
        Bid a = new Bid(new byte[]{1, 2, 3}, new byte[]{1, 2, 3}, new byte[]{1, 2, 3}, 1, new byte[]{1, 2, 3});

        Bid b = (Bid) Transaction.fromGrpc(a.toGrpc());

        assert a.equals(b);
    }

}