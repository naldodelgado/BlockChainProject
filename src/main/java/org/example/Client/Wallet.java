package org.example.Client;

import org.apache.commons.lang3.tuple.Pair;
import org.example.Blockchain.BlockChain;
import org.example.Utils.KeysManager;
import org.example.Utils.LogFilter;

import java.io.Serializable;
import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Wallet implements Serializable {
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    // bids done by this wallet with the auction they belong to
    private final List<Pair<Bid, Auction>> bids = new ArrayList<>();
    // auctions started by this wallet with the bids they have received
    private final List<Pair<Auction, List<Bid>>> auctions = new ArrayList<>();
    private static BlockChain blockChain;
    private static final Logger logger = Logger.getLogger(Wallet.class.getName());

    public Wallet(PublicKey publicKey, PrivateKey privateKey) {
        if (blockChain == null)
            throw new RuntimeException("blockchain not initialized");
        this.privateKey=privateKey;
        this.publicKey=publicKey;
        logger.setFilter(new LogFilter());
    }

    public Wallet() {
        if (blockChain == null)
            throw new RuntimeException("blockchain not initialized");
        KeyPair keyPair = KeysManager.generateKeys();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
        logger.setFilter(new LogFilter());
    }

    public static void setBlockchain(BlockChain blockChain) {
        Wallet.blockChain = blockChain;
    }

    public void bid(Auction auction, PublicKey sellerKey, int amount) {
        bids.add(Pair.of(new Bid(auction.getHash(), publicKey, sellerKey, amount, privateKey), auction));
        blockChain.addTransaction(bids.get(bids.size() - 1).getLeft());
    }

    public List<Bid> getBids() {
        return bids
                .stream()
                .map(Pair::getLeft)
                .collect(Collectors.toList());
    }

    public List<Pair<Auction, List<Bid>>> getAuctions() {
        return auctions;
    }

    public List<Pair<Auction, List<Bid>>> getOnGoingAuctions() {
        return auctions
                .stream()
                .filter(auction -> auction.getLeft().getTimeout() > System.currentTimeMillis())
                .collect(Collectors.toList());
    }

    public List<Bid> getMyBids(Auction action) {
        return auctions
                .stream()
                .filter(auction -> auction.getLeft().equals(action))
                .findFirst()
                .map(Pair::getRight)
                .orElse(new ArrayList<>());
    }

    public Auction startAuction(byte[] idItem, int minAmount, int minIncrement, long timeout) {
        Auction temp = new Auction(idItem, minAmount, minIncrement, timeout, this);
        blockChain.addTransaction(temp);
        auctions.add(Pair.of(temp, new ArrayList<>()));
        BlockChain.subscribe(this, temp);
        return temp;
    }

    public List<Pair<Bid, Auction>> getMyBids() {
        return bids;
    }

    //Getters
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public void alert(Transaction transaction) {
        if (transaction instanceof Bid) {
            Bid bid = (Bid) transaction;
            for (Pair<Auction, List<Bid>> auction : auctions) {
                if (Arrays.equals(auction.getLeft().getAuctionHash(), bid.getAuctionHash())) {
                    auction.getRight().add(bid);
                    break;
                }
            }
        }
    }
}
