package org.example;

import java.io.Serializable;
import java.util.Date;
import org.bouncycastle.crypto.digests.SHA1Digest;

public class Transaction implements Serializable {
    private byte[] transactionId;
    private String senderAddress;
    private String recipientAddress;
    private int amount;
    private long timestamp;


    // Constructor
    public Transaction(String senderAddress, String recipientAddress, int amount) {
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
        this.amount = amount;
        this.timestamp = new Date().getTime();
        this.transactionId = generateTransactionId();
        assert transactionId.length == 20;
        // Generate transactionId, signature, nonce, etc.
    }

    private byte[] generateTransactionId() {
        // Use SHA-1 as an example hash function
        SHA1Digest digest = new SHA1Digest();
        byte[] data = (senderAddress + recipientAddress + amount + timestamp).getBytes();
        byte[] hash = new byte[digest.getDigestSize()];

        digest.update(data, 0, data.length);
        digest.doFinal(hash, 0);

        return hash;
    }

    // Getters and setters

    // Additional methods for transaction processing, signature generation, etc.
}
