package org.example.Client;

import org.example.Utils.KeysManager;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuctionTest {
    @Test
    public void testStore() {
        Path path = Paths.get("blockchain", "transactions", "auctions");
        try {
            if (!Files.exists(path)) {
                Files.createDirectories(path);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        PublicKey publicKey = KeysManager.generateKeys().getPublic();
        Auction auction = new Auction(new byte[]{1, 2, 3}, 4, 5, 6, publicKey.getEncoded(), new byte[]{7, 8, 9}, new byte[]{10, 11, 12});
        auction.store();
        Auction loadedAuction = auction.load(KeysManager.hexString(auction.hash()));
        assertTrue(auction.equals(loadedAuction));
        try {
            // delete the file
            Files.deleteIfExists(Paths.get("blockchain", "transactions", "auctions", KeysManager.hexString(auction.hash()) + ".auction"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
