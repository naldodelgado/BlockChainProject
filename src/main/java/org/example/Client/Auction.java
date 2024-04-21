package org.example.Client;

import kademlia_public_ledger.kTransaction;
import org.example.CryptoUtils.KeysManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.logging.Logger;

public class Auction extends Transaction {

    private final byte[] idItem;
    private final int minAmount;
    private final int minIncrement;
    private final long timeout;
    private  final PublicKey sellerPublicKey;
    private final byte[] hash;
    private final byte[] signature;

    public Auction(byte[] idItem, int minAmount, int minIncrement, long timeout, PublicKey sellerPublicKey, byte[] hash, byte[] signature) {
        this.idItem = idItem;
        this.minAmount = minAmount;
        this.minIncrement = minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey = sellerPublicKey;
        this.hash = hash;
        this.signature = signature;
    }

    public Auction(byte[] idItem, int minAmount, int minIncrement, long timeout, Wallet seller) {
        this.idItem=idItem;
        this.minAmount=minAmount;
        this.minIncrement=minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey= seller.getPublicKey();
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
    public kTransaction toGrpc() {
        return kTransaction.newBuilder()
                .setAuction(
                        kademlia_public_ledger.Auction.newBuilder()
                                .setItemID(com.google.protobuf.ByteString.copyFrom(this.idItem))
                                .setStartBid(this.minAmount)
                                .setMinBid(this.minIncrement)
                                .setTimeout(this.timeout)
                                .setKey(com.google.protobuf.ByteString.copyFrom(this.sellerPublicKey.getEncoded()))
                                .setHash(com.google.protobuf.ByteString.copyFrom(this.hash))
                                .setSignature(com.google.protobuf.ByteString.copyFrom(this.signature))
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
    public void store() {
        Logger.getGlobal().info("Storing Auction");

        File file = new File("blockchain/auctions/" + KeysManager.hexString(this.hash) + ":" + KeysManager.hexString(this.sellerPublicKey.getEncoded()) + ".auction");

        try {

            //TODO: better way to store the auction
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
            fileOutputStream.close();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] hash() {
        return hash;
    }

    //Getters
    public PublicKey getSellerPublicKey() {
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
}
