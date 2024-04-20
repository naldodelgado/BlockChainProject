package org.example.Blockchain;

import kademlia_public_ledger.Bid;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.example.CryptoUtils.KeysManager;

import java.io.Serializable;
import java.security.PrivateKey;
import java.util.Date;


public class Transaction implements Serializable {
    private final byte[] transactionId;
    private final byte[] actionID; // public key
    private final byte[] recipientAddress; // public key
    private final byte[] signature;
    private final int amount;
    private final long timestamp;

    // Constructor
    public Transaction(byte[] senderAddress, byte[] recipientAddress, int amount, byte[] signature) {
        this.actionID = senderAddress;
        this.recipientAddress = recipientAddress;
        this.amount = amount;
        this.timestamp = new Date().getTime();
        this.transactionId = KeysManager.hash(new Object[]{actionID, recipientAddress, amount, timestamp});
        assert transactionId.length == 20;
        this.signature = signature;
    }

    public Transaction(byte[] senderAddress, byte[] recipientAddress, int amount, PrivateKey privateKey) {
        this.actionID = senderAddress;
        this.recipientAddress = recipientAddress;
        this.amount = amount;
        this.timestamp = new Date().getTime();
        this.transactionId = KeysManager.hash(new Object[]{actionID, recipientAddress, amount, timestamp});
        this.signature = KeysManager.sign(privateKey, new Object[]{actionID, recipientAddress, amount, timestamp});
    }

    private byte[] concatenateByteArrays(byte[] senderAddress, byte[] recipientAddress) {
        byte[] result = new byte[senderAddress.length + recipientAddress.length];
        System.arraycopy(senderAddress, 0, result, 0, senderAddress.length);
        System.arraycopy(recipientAddress, 0, result, senderAddress.length, recipientAddress.length);
        return result;
    }

    public static Transaction fromGrpc(Bid transaction) {
        byte[] senderAddress = transaction.getSender().toByteArray();// public key
        byte[] recipientAddress = transaction.getReceiver().toByteArray(); // public key
        byte[] signature = transaction.getSignature().toByteArray();
        int amount = transaction.getAmount();
        return new Transaction(senderAddress,recipientAddress,amount,signature);
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public byte[] getActionID() {
        return actionID;
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

    public Bid toGrpc() {
        return Bid.newBuilder()
                .setSender(com.google.protobuf.ByteString.copyFrom(actionID))
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
        byte[] data = concatenateByteArrays(actionID, recipientAddress);
        //TODO: Add timestamp to the data
        byte[] hash = new byte[digest.getDigestSize()];
        digest.update(data, 0, data.length);
        digest.doFinal(hash, 0);
        return hash;
    }

    // Additional methods for transaction processing, signature generation, etc.
}
