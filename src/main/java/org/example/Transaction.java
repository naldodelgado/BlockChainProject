package org.example;

import java.util.Date;

public class Transaction {
    private String transactionId;
    private String senderAddress;
    private String recipientAddress;
    private double amount;
    private long timestamp;
    private double transactionFee;
    private String signature;
    private String[] inputs;
    private String[] outputs;

    // Constructor
    public Transaction(String senderAddress, String recipientAddress, double amount) {
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
        this.amount = amount;
        this.timestamp = new Date().getTime();
        // Generate transactionId, signature, nonce, etc.
    }

    // Getters and setters

    // Additional methods for transaction processing, signature generation, etc.
}
