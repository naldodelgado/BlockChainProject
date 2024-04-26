package org.example.Client;

import org.example.CryptoUtils.KeysManager;

import java.io.File;
import java.io.Serializable;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;

public class Bid extends Transaction implements Serializable {
    private final byte[] transactionId;
    private final byte[] actionHash; // actionID
    private final byte[] senderAddress; // public key
    private final byte[] recipientAddress; // public key
    private final byte[] signature;
    private final int amount;
    private final long timestamp;

    public Bid(byte[] actionHash, byte[] senderAddress, byte[] recipientAddress, int amount, byte[] signature) {
        this.actionHash = actionHash;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
        this.amount = amount;
        this.timestamp = new Date().getTime();
        this.transactionId = KeysManager.hash(new Object[]{actionHash, senderAddress, recipientAddress, amount, timestamp});
        assert transactionId.length == 20;
        this.signature = signature;
    }

    public Bid(byte[] actionHash, PublicKey senderAddress, PublicKey recipientAddress, int amount, PrivateKey privateKey) {
        this.actionHash = actionHash;
        this.senderAddress = senderAddress.getEncoded();
        this.recipientAddress = recipientAddress.getEncoded();
        this.amount = amount;
        this.timestamp = new Date().getTime();
        this.transactionId = KeysManager.hash(new Object[]{actionHash, recipientAddress, amount, timestamp});
        this.signature = KeysManager.sign(privateKey, new Object[]{actionHash, recipientAddress, amount, timestamp});
    }

    public static Bid fromGrpc(kademlia_public_ledger.Bid bid) {
        byte[] senderAddress = bid.toByteArray();// public key
        byte[] recipientAddress = bid.getReceiver().toByteArray(); // public key
        byte[] signature = bid.getSignature().toByteArray();
        byte[] actionID = bid.getItemID().toByteArray();
        int amount = bid.getAmount();
        return new Bid(actionID, senderAddress, recipientAddress, amount, signature);
    }

    public byte[] getTransactionId() {
        return transactionId;
    }

    public byte[] getActionHash() {
        return actionHash;
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

    public kademlia_public_ledger.kTransaction toGrpc() {
        return kademlia_public_ledger.kTransaction.newBuilder()
                .setBid(
                        kademlia_public_ledger.Bid.newBuilder()
                                .setSender(com.google.protobuf.ByteString.copyFrom(actionHash))
                                .setReceiver(com.google.protobuf.ByteString.copyFrom(recipientAddress))
                                .setAmount(amount)
                                .build()
                ).build();
    }

    @Override
    public boolean verify() {
        byte[] data = KeysManager.hash(new Object[]{actionHash, recipientAddress, amount, timestamp});
        return KeysManager.verifySignature(signature, data, KeysManager.getPublicKeyFromBytes(actionHash));
    }

    @Override
    public void store() {
        File file = new File("blockchain/actions/" + KeysManager.hexString(actionHash) + ":" + KeysManager.hexString(senderAddress) + ".auction");

        //TODO: implement a way to append the bid to the auction file

    }

    @Override
    public byte[] hash() {
        return KeysManager.hash(new Object[]{actionHash, recipientAddress, amount, timestamp});
    }

    @Override
    public String toString() {
        return "Bid{" +
                "transactionId=" + KeysManager.hexString(transactionId) + "\n\t" +
                "actionHash=" + KeysManager.hexString(actionHash) + "\n\t" +
                "senderAddress=" + KeysManager.hexString(senderAddress) + "\n\t" +
                "recipientAddress=" + KeysManager.hexString(recipientAddress) + "\n\t" +
                "signature=" + KeysManager.hexString(signature) + "\n\t" +
                "amount=" + amount + "\n\t" +
                "timestamp=" + timestamp + "\n" +
                '}';
    }

}
