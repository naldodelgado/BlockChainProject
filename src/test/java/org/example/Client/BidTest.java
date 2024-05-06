package org.example.Client;

import org.example.Utils.KeysManager;
import org.junit.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BidTest {
    @Test
    public void testStore() {
        Bid bid = new Bid(new byte[]{1, 2, 76}, new byte[]{4, 5, 6}, new byte[]{7, 8, 9}, 10, new byte[]{11, 12, 13});
        System.out.println(KeysManager.hexString(bid.hash()));
        bid.store();
        Bid loadedBid = bid.load(KeysManager.hexString(bid.hash()));
        assertTrue(bid.equals(loadedBid));
    }
}