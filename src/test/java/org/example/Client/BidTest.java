package org.example.Client;

import org.junit.Test;

import static org.junit.jupiter.api.Assertions.*;

public class BidTest {
    @Test
    public void testStore() {
        Bid bid = new Bid(new byte[]{1, 2, 3}, new byte[]{4, 5, 6}, new byte[]{7, 8, 9}, 10, new byte[]{11, 12, 13});
        bid.store();
        Bid loadedBid = bid.load("Transactions/Bids/[1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13].bid");
        assertEquals(bid, loadedBid);
    }
}