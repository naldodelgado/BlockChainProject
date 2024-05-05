package org.example.Client;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AuctionTest {
    @Test
    public void testStore() {
        Auction auction = new Auction(new byte[]{1, 2, 3}, 4, 5, 6, new Wallet());
        auction.store();
        Auction loadedAuction = auction.load("Transactions/Auctions/[1, 2, 3, 4, 5, 6].auction");
        assertEquals(auction, loadedAuction);
    }
}