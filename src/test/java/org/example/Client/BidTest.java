package org.example.Client;

import org.example.Utils.KeysManager;
import org.junit.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class BidTest {
    @Test
    public void testStore() {
        // generate the folder blockchain\transactions\bids\ in the project directory
        Path path = Paths.get("blockchain", "transactions", "bids");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        Bid bid = new Bid(new byte[]{1, 2, 76}, new byte[]{4, 5, 6}, new byte[]{7, 8, 9}, 10, new byte[]{11, 12, 13});
        System.out.println(KeysManager.hexString(bid.hash()));
        bid.store();
        Bid loadedBid = bid.load(KeysManager.hexString(bid.hash()));
        assertTrue(bid.equals(loadedBid));
        try {
            // delete the file
            Files.deleteIfExists(Paths.get("blockchain", "transactions", "bids", KeysManager.hexString(bid.hash()) + ".bid"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testEquals(){
        Bid bid = new Bid(new byte[]{1, 2, 76}, new byte[]{4, 5, 6}, new byte[]{7, 8, 9}, 10, new byte[]{11, 12, 13});
        assertTrue(bid.equals(bid));
    }
}