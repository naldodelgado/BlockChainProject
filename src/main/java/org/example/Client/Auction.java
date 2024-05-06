package org.example.Client;

import com.google.protobuf.ByteString;
import kademlia_public_ledger.kTransaction;
import org.example.Utils.FileSystem;
import org.example.Utils.KeysManager;

import java.io.*;
import java.security.PublicKey;
import java.util.Arrays;

public class Auction extends Transaction implements Serializable{

    private final byte[] idItem;
    private final int minAmount;
    private final int minIncrement;
    private final long timeout;
    private  final byte[] sellerPublicKey;
    private final byte[] hash;
    private final byte[] signature;

    public Auction(byte[] idItem, int minAmount, int minIncrement, long timeout, byte[] sellerPublicKey, byte[] hash, byte[] signature) {
        this.idItem = idItem;
        this.minAmount = minAmount;
        this.minIncrement = minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey = sellerPublicKey;
        this.hash = hash;
        this.signature = signature;
    }

    public Auction(byte[] idItem, int minAmount, int minIncrement, long timeout, PublicKey sellerPublicKey, byte[] hash, byte[] signature) {
        this.idItem = idItem;
        this.minAmount = minAmount;
        this.minIncrement = minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey = sellerPublicKey.getEncoded();
        this.hash = hash;
        this.signature = signature;
    }

    public Auction(byte[] idItem, int minAmount, int minIncrement, long timeout, Wallet seller) {
        this.idItem=idItem;
        this.minAmount=minAmount;
        this.minIncrement=minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey= seller.getPublicKey().getEncoded();
        this.hash = KeysManager.hash(new Object[]{this.idItem, this.minAmount, this.minIncrement, this.timeout, this.sellerPublicKey.hashCode()});

        Object[] objects = {this.idItem, this.minAmount, this.minIncrement, this.timeout, this.sellerPublicKey.hashCode()};

        this.signature = KeysManager.sign(seller.getPrivateKey(), objects);
    }

    public static Auction fromGrpc(kademlia_public_ledger.Auction auction) {
        return new Auction(
                auction.getItemID().toByteArray(),
                auction.getStartBid(),
                auction.getMinBid(),
                auction.getTimeout(),
                KeysManager.getPublicKeyFromBytes(auction.getKey().toByteArray()),
                auction.getHash().toByteArray(),
                auction.getSignature().toByteArray()
        );
    }

    @Override
    public kTransaction toGrpc(byte[] id) {
        return kTransaction.newBuilder()
                .setAuction(
                        kademlia_public_ledger.Auction.newBuilder()
                                .setItemID(ByteString.copyFrom(this.idItem))
                                .setStartBid(this.minAmount)
                                .setMinBid(this.minIncrement)
                                .setTimeout(this.timeout)
                                .setKey(ByteString.copyFrom(this.sellerPublicKey))
                                .setHash(ByteString.copyFrom(this.hash))
                                .setSignature(ByteString.copyFrom(this.signature))
                                .build()
                ).setSender(ByteString.copyFrom(id))
                .build();
    }

    @Override
    public kTransaction toGrpc() {
        return kTransaction.newBuilder()
                .setAuction(
                        kademlia_public_ledger.Auction.newBuilder()
                                .setItemID(com.google.protobuf.ByteString.copyFrom(this.idItem))
                                .setStartBid(this.minAmount)
                                .setMinBid(this.minIncrement)
                                .setTimeout(this.timeout)
                                .setKey(ByteString.copyFrom(this.sellerPublicKey))
                                .setHash(ByteString.copyFrom(this.hash))
                                .setSignature(ByteString.copyFrom(this.signature))
                                .build()
                ).build();
    }

    @Override
    public boolean verify() {
        Object[] objects = {this.idItem, this.minAmount, this.minIncrement, this.timeout, this.sellerPublicKey.hashCode()};
        return Arrays.equals(this.hash, KeysManager.hash(objects))
                || !KeysManager.verifySignature(this.signature, this.hash, this.sellerPublicKey);
    }

    @Override
    public byte[] hash() {
        return KeysManager.hash(new Object[]{this.idItem, this.minAmount, this.minIncrement, this.timeout, this.sellerPublicKey.hashCode()});
    }

    @Override
    public String toString() {
        return "Auction{" +
                "idItem=" + KeysManager.hexString(idItem) + "\n\t" +
                "minAmount=" + minAmount + "\n\t" +
                "minIncrement=" + minIncrement + "\n\t" +
                "timeout=" + timeout + "\n\t" +
                "sellerPublicKey=" + KeysManager.hexString(sellerPublicKey) + "\n\t" +
                "hash=" + KeysManager.hexString(hash) + "\n\t" +
                "signature=" + KeysManager.hexString(signature) + "\n" +
                '}';
    }

    @Override
    public void store() {
        String filePath = FileSystem.auctionPath + KeysManager.hexString(this.hash()) + ".auction";
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(filePath))) {
            out.writeObject(this);
        } catch (IOException e) {
            System.err.println("Error writing to file: " + e.getMessage());
        }
    }

    @Override
    public Auction load(String filePath){
            Auction auction = null;
            try {
                String file = "blockchain/transactions/auctions/" + filePath + ".auction";
                FileInputStream fileIn = new FileInputStream(file);
                ObjectInputStream in = new ObjectInputStream(fileIn);
                auction = (Auction) in.readObject();
                in.close();
                fileIn.close();
            } catch (IOException e) {
                System.err.println("Error reading to file: " + e.getMessage());
            } catch (ClassNotFoundException e) {
                throw new RuntimeException(e);
            }
            return auction;
}



    //Getters
    public byte[] getSellerPublicKey() {
        return sellerPublicKey;
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getSignature() {
        return signature;
    }

    public byte[] get_idItem() {
        return idItem;
    }

    public long getMinAmount() {
        return minAmount;
    }

    public float getMinIncrement() {
        return minIncrement;
    }

    public long getTimeout() {
        return timeout;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Auction) {
            Auction auction = (Auction) obj;
            return Arrays.equals(this.idItem, auction.idItem)
                    && this.minAmount == auction.minAmount
                    && this.minIncrement == auction.minIncrement
                    && this.timeout == auction.timeout
                    && Arrays.equals(this.sellerPublicKey, auction.sellerPublicKey)
                    && Arrays.equals(this.hash, auction.hash)
                    && Arrays.equals(this.signature, auction.signature);
        }
        return false;
    }
}
