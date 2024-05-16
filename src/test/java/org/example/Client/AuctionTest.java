package org.example.Client;

import org.example.Utils.KeysManager;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PublicKey;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AuctionTest {
    @Test
    public void testStore() throws IOException {
        Path path = Paths.get("blockchain", "transactions", "auctions");

        if (!Files.exists(path)) {
            Files.createDirectories(path);
        }

        PublicKey publicKey = KeysManager.generateKeys().getPublic();
        Auction auction = new Auction(new byte[]{1, 2, 3}, 4, 5, 6, publicKey.getEncoded(), new byte[]{7, 8, 9}, new byte[]{10, 11, 12});
        auction.store();
        Optional<Transaction> loadedAuction = Auction.load(auction.hash());
        assertTrue(loadedAuction.isPresent());
        assertEquals(auction, loadedAuction.get());

        // delete the file
        Files.deleteIfExists(Paths.get("blockchain", "transactions", "auctions", KeysManager.hexString(auction.hash()) + ".auction"));
    }
}
