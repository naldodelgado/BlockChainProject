package org.example.Blockchain;

import kademlia_public_ledger.kTransaction;
import org.bouncycastle.crypto.digests.SHA1Digest;

import java.io.Serializable;
import java.util.Date;

public class Transaction implements Serializable {
    private final byte[] transactionId;
    private final byte[] senderAddress; // public key
    private final byte[] recipientAddress; // public key
    private final int amount;
    private final long timestamp;
    //private final byte[] signature;

    // Constructor
    public Transaction(byte[] senderAddress, byte[] recipientAddress, int amount) {
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
        byte[] data = concatenateByteArrays(senderAddress, recipientAddress);
        //TODO: Add timestamp to the data
        byte[] hash = new byte[digest.getDigestSize()];
        digest.update(data, 0, data.length);
        digest.doFinal(hash, 0);

        return hash;
    }

    private byte[] concatenateByteArrays(byte[] senderAddress, byte[] recipientAddress) {
        byte[] result = new byte[senderAddress.length + recipientAddress.length];
        System.arraycopy(senderAddress, 0, result, 0, senderAddress.length);
        System.arraycopy(recipientAddress, 0, result, senderAddress.length, recipientAddress.length);
        return result;
    }

    public static Transaction fromGrpc(kTransaction transaction) {
        byte[] senderAddress = transaction.getSender().toByteArray();// public key
        byte[] recipientAddress = transaction.getReceiver().toByteArray(); // public key
        int amount = transaction.getAmount();
        return new Transaction(senderAddress,recipientAddress,amount);
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public byte[] getSenderAddress() {
        return senderAddress;
    }

    public byte[] getRecipientAddress() {
        return recipientAddress;
    }

    public int getAmount() {
        return amount;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public kTransaction toGrpc() {
        return kTransaction.newBuilder()
                .setSender(com.google.protobuf.ByteString.copyFrom(senderAddress))
                .setReceiver(com.google.protobuf.ByteString.copyFrom(recipientAddress))
                .setAmount(amount)
                .build();
    }

    public byte[] encryptTransaction() {
        // Encrypt the transaction data
        return null;
    }

    public byte[] calculateHash() {
        SHA1Digest digest = new SHA1Digest();
        byte[] data = concatenateByteArrays(senderAddress, recipientAddress);
        //TODO: Add timestamp to the data
        byte[] hash = new byte[digest.getDigestSize()];
        digest.update(data, 0, data.length);
        digest.doFinal(hash, 0);
        return hash;
    }

    // Additional methods for transaction processing, signature generation, etc.
}
