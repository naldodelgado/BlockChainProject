package org.example;

import java.util.ArrayList;
import java.util.Date;

public class Block {
    private String hash; // Hash of this block
    private String previousHash; // Hash of the previous block
    private ArrayList<Transaction> transactions; // List of transactions in this block
    private long timestamp; // Timestamp of when this block was created
    private int nonce; // Nonce used in mining

    // Constructor
    public Block(String previousHash) {
        this.previousHash = previousHash;
        this.transactions = new ArrayList<>();
        this.timestamp = new Date().getTime();
        // Other initialization as needed
    }

    // Getters and setters
    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }

    public String getPreviousHash() {
        return previousHash;
    }

    public ArrayList<Transaction> getTransactions() {
        return transactions;
    }

    public void addTransaction(Transaction transaction) {
        this.transactions.add(transaction);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public int getNonce() {
        return nonce;
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }

    // Other methods as needed
}
