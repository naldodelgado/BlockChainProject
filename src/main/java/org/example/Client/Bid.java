package org.example.Client;

import com.google.protobuf.ByteString;
import kademlia_public_ledger.kTransaction;
import org.example.Utils.FileSystem;
import org.example.Utils.KeysManager;

import java.io.*;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Date;

public class Bid extends Transaction implements Serializable {
    private final byte[] transactionId;
    private final byte[] auctionHash; // actionID
    private final byte[] senderAddress; // public key
    private final byte[] recipientAddress; // public key
    private final byte[] signature;
    private final int amount;
    private final long timestamp;

    public Bid(byte[] auctionHash, byte[] senderAddress, byte[] recipientAddress, int amount, byte[] signature, long timestamp) {
        this.auctionHash = auctionHash;
        this.senderAddress = senderAddress;
        this.recipientAddress = recipientAddress;
        this.amount = amount;
        this.timestamp = timestamp;
        this.transactionId = KeysManager.hash(new Object[]{auctionHash, senderAddress, recipientAddress, amount, timestamp});
        assert transactionId.length == 32;
        this.signature = signature;
    }

    public Bid(byte[] auctionHash, PublicKey senderAddress, PublicKey recipientAddress, int amount, PrivateKey privateKey) {
        this.auctionHash = auctionHash;
        this.senderAddress = senderAddress.getEncoded();
        this.recipientAddress = recipientAddress.getEncoded();
        this.amount = amount;
        this.timestamp = new Date().getTime();
        this.transactionId = KeysManager.hash(new Object[]{auctionHash, senderAddress.getEncoded(), recipientAddress.getEncoded(), amount, timestamp});
        this.signature = KeysManager.sign(privateKey, new Object[]{transactionId});
    }

    public static Bid fromGrpc(kademlia_public_ledger.Bid bid) {
        byte[] senderAddress = bid.getSender().toByteArray();// public key
        byte[] recipientAddress = bid.getReceiver().toByteArray(); // public key
        byte[] signature = bid.getSignature().toByteArray();
        byte[] actionID = bid.getItemID().toByteArray();
        int amount = bid.getAmount();
        long time = bid.getTimestamp();

        return new Bid(actionID, senderAddress, recipientAddress, amount, signature, time);
    }

    public kademlia_public_ledger.kTransaction toGrpc(byte[] senderID) {
        return kademlia_public_ledger.kTransaction.newBuilder()
                .setBid(
                        kademlia_public_ledger.Bid.newBuilder()
                                .setSender(ByteString.copyFrom(senderAddress))
                                .setReceiver(ByteString.copyFrom(recipientAddress))
                                .setAmount(amount)
                                .setTimestamp(timestamp)
                                .setSignature(ByteString.copyFrom(this.signature))
                                .setItemID(ByteString.copyFrom(auctionHash))
                                .build()
                ).setSender(ByteString.copyFrom(senderID))
                .build();
    }

    @Override
    public kTransaction toGrpc() {
        return kademlia_public_ledger.kTransaction.newBuilder()
                .setBid(
                        kademlia_public_ledger.Bid.newBuilder()
                                .setSender(ByteString.copyFrom(senderAddress))
                                .setReceiver(ByteString.copyFrom(recipientAddress))
                                .setAmount(amount)
                                .setTimestamp(timestamp)
                                .setSignature(ByteString.copyFrom(this.signature))
                                .setItemID(ByteString.copyFrom(auctionHash))
                                .build()
                ).build();
    }

    @Override
    public boolean verify() {
        byte[] data = KeysManager.hash(new Object[]{auctionHash, recipientAddress, amount, timestamp});
        //TODO: verify that the timestamp is bigger than the timestamp of its auction
        // verify that its auction exists
        // verify that the amount is bigger that the last bid plus the min bid amount
        return KeysManager.verifySignature(signature, data, auctionHash);
    }

    @Override
    public void store() {
    String filePath = FileSystem.bidPath + KeysManager.hexString(this.hash()) + ".bid";
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    @Override
    public Bid load(String filePath) {
        Bid bid = null;
        try {
            String file = "blockchain/transactions/bids/" + filePath + ".bid";
            FileInputStream fileIn = new FileInputStream(file);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            bid = (Bid) in.readObject();
            in.close();
            fileIn.close();
        } catch (IOException e) {
            System.err.println("Error reading to file: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return bid;
    }

    @Override
    public byte[] hash() {
        return KeysManager.hash(new Object[]{auctionHash, recipientAddress, amount, timestamp});
    }

    @Override
    public String toString() {
        return "Bid{" +
                "transactionId=" + KeysManager.hexString(transactionId) + "\n\t" +
                "actionHash=" + KeysManager.hexString(auctionHash) + "\n\t" +
                "senderAddress=" + KeysManager.hexString(senderAddress) + "\n\t" +
                "recipientAddress=" + KeysManager.hexString(recipientAddress) + "\n\t" +
                "signature=" + KeysManager.hexString(signature) + "\n\t" +
                "amount=" + amount + "\n\t" +
                "timestamp=" + timestamp + "\n" +
                '}';
    }



    public byte[] getTransactionId() {
        return transactionId;
    }

    public byte[] getAuctionHash() {
        return auctionHash;
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

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Bid) {
            Bid bid = (Bid) obj;
            return Arrays.equals(this.transactionId, bid.transactionId) &&
                    Arrays.equals(this.auctionHash, bid.auctionHash) &&
                    Arrays.equals(this.senderAddress, bid.senderAddress) &&
                    Arrays.equals(this.recipientAddress, bid.recipientAddress) &&
                    Arrays.equals(this.signature, bid.signature) &&
                    this.amount == bid.amount &&
                    this.timestamp == bid.timestamp;
        }
        return false;
    }
}
