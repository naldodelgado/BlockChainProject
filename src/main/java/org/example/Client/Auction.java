package org.example.Client;

import com.google.protobuf.ByteString;
import kademlia_public_ledger.kTransaction;
import org.example.Utils.FileSystem;
import org.example.Utils.KeysManager;

import java.io.*;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;
import java.util.logging.Logger;

public class Auction extends Transaction implements Serializable{

    private final byte[] ItemID;
    private final int minAmount;
    private final int minIncrement;
    private final long timeout;
    private  final byte[] sellerPublicKey;
    private final byte[] hash;
    private final byte[] signature;

    private static final Logger logger = Logger.getLogger(Auction.class.getName());

    public Auction(byte[] ItemID, int minAmount, int minIncrement, long timeout, byte[] sellerPublicKey, byte[] hash, byte[] signature) {
        this.ItemID = ItemID;
        this.minAmount = minAmount;
        this.minIncrement = minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey = sellerPublicKey;
        this.hash = hash;
        this.signature = signature;
    }

    public Auction(byte[] ItemID, int minAmount, int minIncrement, long timeout, PublicKey sellerPublicKey, byte[] hash, byte[] signature) {
        this.ItemID = ItemID;
        this.minAmount = minAmount;
        this.minIncrement = minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey = sellerPublicKey.getEncoded();
        this.hash = hash;
        this.signature = signature;
    }

    public Auction(byte[] ItemID, int minAmount, int minIncrement, long timeout, Wallet seller) {
        this.ItemID = ItemID;
        this.minAmount=minAmount;
        this.minIncrement=minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey= seller.getPublicKey().getEncoded();
        this.hash = hash();
        this.signature = KeysManager.sign(seller.getPrivateKey(), hash);
    }

    public static Auction fromGrpc(kademlia_public_ledger.Auction auction) {
        return new Auction(
                auction.getItemID().toByteArray(),
                auction.getStartBid(),
                auction.getMinBid(),
                auction.getTimeout(),
                auction.getKey().toByteArray(),
                auction.getHash().toByteArray(),
                auction.getSignature().toByteArray()
        );
    }

    @Override
    public kTransaction toGrpc(byte[] id) {
        return kTransaction.newBuilder()
                .setAuction(
                        kademlia_public_ledger.Auction.newBuilder()
                                .setItemID(ByteString.copyFrom(this.ItemID))
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
                                .setItemID(ByteString.copyFrom(this.ItemID))
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
    public boolean isValid() {
        if (this.hash == null || this.hash.length * 8 != 160)
            return false;

        if (this.ItemID == null || this.ItemID.length * 8 != 160)
            return false;

        if (this.sellerPublicKey == null || KeysManager.getPublicKeyFromBytes(this.sellerPublicKey).isEmpty())
            return false;

        if (this.signature == null)
            return false;

        if (this.minIncrement <= 0 || this.minAmount <= 0)
            return false;

        if (this.timeout < 1704067200) //unix time for start of 2024
            return false;

        return Arrays.equals(this.hash, hash())
                && KeysManager.verifySignature(this.signature, this.hash, this.sellerPublicKey);
    }

    @Override
    public byte[] hash() {
        return KeysManager.hash(new Object[]{this.ItemID, this.minAmount, this.minIncrement, this.timeout, this.sellerPublicKey});
    }

    @Override
    public String toString() {
        return "Auction{" +
                "itemID=" + KeysManager.hexString(ItemID) + "\n\t" +
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

    public static Optional<Transaction> load(byte[] id) {
        String fileName = FileSystem.auctionPath + KeysManager.hexString(id) + ".auction";

        try {
            FileInputStream fileIn = new FileInputStream(fileName);
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Auction auction = (Auction) in.readObject();
            in.close();
            fileIn.close();
            if (auction != null) return Optional.of(auction);
        } catch (IOException ignored) {
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return Optional.empty();
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

    public byte[] getItemID() {
        return ItemID;
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
            return Arrays.equals(this.ItemID, auction.ItemID)
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
