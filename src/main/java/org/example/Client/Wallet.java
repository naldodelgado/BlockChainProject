package org.example.Client;

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
    private final List<Transaction> transactions = new ArrayList<>();

    public Wallet(PublicKey publicKey, PrivateKey privateKey) {
        this.privateKey=privateKey;
        this.publicKey=publicKey;
    }

    public Wallet() {
        KeyPair keyPair = KeysManager.generateKeys();
        this.privateKey = keyPair.getPrivate();
        this.publicKey = keyPair.getPublic();
    }

    public void startAuction(byte[] idItem, int minAmount, int minIncrement, long timeout) {
        transactions.add(new Auction(idItem, minAmount, minIncrement, timeout, this));
    }

    public void bid(byte[] idItem, PublicKey sellerKey, int amount) {
        transactions.add(new Bid(idItem, publicKey, sellerKey, amount, privateKey));
    }

    public List<Transaction> getTransactions() {
        return transactions;
    }

    public List<Auction> getAuctions() {
        return transactions
                .stream()
                .filter(transaction -> transaction instanceof Auction)
                .map(transaction -> (Auction) transaction)
                .collect(Collectors.toList());
    }

    public List<Auction> getOnGoingAuctions() {
        return transactions
                .stream()
                .filter(transaction -> transaction instanceof Auction)
                .map(transaction -> (Auction) transaction)
                .filter(auction -> auction.getTimeout() > System.currentTimeMillis())
                .collect(Collectors.toList());
    }

    public List<Bid> getMyBids(Auction action) {
        return transactions
                .stream()
                .filter(transaction -> transaction instanceof Bid)
                .map(transaction -> (Bid) transaction)
                .filter(bid -> bid.getActionHash().equals(action.get_idItem()))
                .collect(Collectors.toList());
    }

    public List<Bid> getMyBids() {
        return transactions
                .stream()
                .filter(transaction -> transaction instanceof Bid)
                .map(transaction -> (Bid) transaction)
                .collect(Collectors.toList());
    }


    //Getters
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
