package org.example.Client;

import org.example.Utils.KeysManager;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class BidTest {
    @Test
    public void testStore() throws IOException {
        // generate the folder blockchain\transactions\bids\ in the project directory
        Path path = Paths.get("blockchain", "transactions", "bids");
        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        Bid bid = new Bid(new byte[]{1, 2, 76}, new byte[]{4, 5, 6}, new byte[]{7, 8, 9}, 10, new byte[]{11, 12, 13}, 1);
        bid.store();
        Optional<Bid> loadedBid = Bid.load(bid.hash());
        assertTrue(loadedBid.isPresent());
        assertEquals(bid, loadedBid.get());

        // delete the file
        Files.deleteIfExists(Paths.get("blockchain", "transactions", "bids", KeysManager.hexString(bid.hash()) + ".bid"));

    }

    @Test
    public void testEquals(){
        Bid bid = new Bid(new byte[]{1, 2, 76}, new byte[]{4, 5, 6}, new byte[]{7, 8, 9}, 10, new byte[]{11, 12, 13}, 1);
        Assert.assertEquals(bid, bid);
    }
}