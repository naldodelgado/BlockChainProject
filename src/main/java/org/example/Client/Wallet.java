package org.example.Client;

import org.apache.commons.lang3.tuple.Pair;
import org.example.Blockchain.BlockChain;
import org.example.CryptoUtils.KeysManager;

import java.security.KeyPair;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Wallet {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    // bids done by this wallet with the auction they belong to
    private final List<Pair<Bid, Auction>> bids = new ArrayList<>();
    // auctions started by this wallet with the bids they have received
    private final List<Pair<Auction, List<Bid>>> auctions = new ArrayList<>();
    private final BlockChain blockChain;

    public Wallet(PublicKey publicKey, PrivateKey privateKey, BlockChain blockChain) {
        this.blockChain = blockChain;
        this.privateKey=privateKey;
        this.publicKey=publicKey;
    }

    public Wallet(BlockChain blockChain) {
        this.blockChain = blockChain;
        KeyPair keyPair = KeysManager.generateKeys();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public void startAuction(byte[] idItem, int minAmount, int minIncrement, long timeout) {
        auctions.add(Pair.of(new Auction(idItem, minAmount, minIncrement, timeout, this), new ArrayList<>()));

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
}
