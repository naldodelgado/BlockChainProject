package org.example.Blockchain;

import org.apache.commons.lang3.tuple.Pair;
import org.example.Client.Auction;
import org.example.Client.Bid;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AuctionManager {

    private final List<Pair<Auction, Bid>> activeAuctions = new ArrayList<>();

    private final ExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public AuctionManager() {

    }

    public void newAuction(Auction auction) {

    }

    public void newBid() {

    }

    public List<Pair<Auction, Bid>> getActiveAuctions() {
        return activeAuctions;
    }

}
