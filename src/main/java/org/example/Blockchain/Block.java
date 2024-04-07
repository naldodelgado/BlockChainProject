package org.example.Blockchain;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Date;

import kademlia_public_ledger.kBlock;
import kademlia_public_ledger.kTransaction;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;

public class Block {
    private byte[] hash = new byte[32]; // Hash of this block - will only be defined after mining
    private byte[] previousHash = new byte[32]; // Hash of the previous block - will only be defined after mining
    private ArrayList<Transaction> transactions; // List of transactions in this block
    private static final int MAX_TRANSACTIONS = 5; // Maximum number of transactions in a block
    private long timestamp; // Timestamp of when this block was created
    private int nonce; // Nonce used in mining - this can only be set once. IMMUTABLE
     static int numZeros = 0; // Number of zeros required at the start of the hash

    // Constructor
    public Block(int nonce) {
        this.transactions = new ArrayList<>();
        this.nonce = nonce;
        this.timestamp = new Date().getTime();
    }

    // Getters and setters
    public byte[] getHash() {
        return hash;
    }

    public void calculateHash() {
        Object[] objects = {previousHash, timestamp, nonce, transactions}; // Serialize these objects to calculate the hash
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);

            // Serialize each object and write it to the stream so than the digest function is able to use multiple objects as input
            for (Object obj : objects) {
                objectOutputStream.writeObject(obj);
            }
            objectOutputStream.close();

            byte[] data = byteArrayOutputStream.toByteArray();

            // Using SHA-256 as an example hash function
            Digest digest = new SHA256Digest();
            byte[] hash = new byte[digest.getDigestSize()];

            digest.update(data, 0, data.length);
            digest.doFinal(hash, 0);

            this.hash = hash;
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Block fromGrpc(kBlock block) {
        // TODO: Implement this method
        Block b =  new Block(0);
        for (kTransaction t: block.getTransactionsList()){

        }
        throw new UnsupportedOperationException("Not implemented yet");

    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
    }



    public byte[] getPreviousHash() {
        return previousHash;
    }

    public void setPreviousHash(byte[] previousHash) {
        this.previousHash = previousHash;
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

    // Other methods as needed
}
