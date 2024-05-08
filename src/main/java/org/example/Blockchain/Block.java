package org.example.Blockchain;

import com.google.protobuf.ByteString;
import kademlia_public_ledger.kBlock;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.example.Client.Transaction;
import org.example.Utils.KeysManager;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class Block implements Serializable {
    private byte[] hash = new byte[32]; // Hash of this block - will only be defined after mining
    private byte[] previousHash = new byte[32]; // Hash of the previous block - will only be defined after mining
    public static final int TRANSACTION_PER_BLOCK = 8; // Maximum number of transactions in a block must be power of 2
    private final ArrayList<Transaction> transactions; // List of transactions in this block
    private final long timestamp; // Timestamp of when this block was created
    private int nonce; // Nonce used in mining - this can only be set once. IMMUTABLE
    private byte[] merkleRoot = new byte[32]; // Merkle root of the transactions in this block
    static int numZeros = 0; // Number of zeros required at the start of the hash

    // Constructor
    public Block(int nonce) {
        this.transactions = new ArrayList<>();
        this.nonce = nonce;
        this.timestamp = new Date().getTime();
    }

    public Block(int nonce, long timestamp) {
        this.transactions = new ArrayList<>();
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public Block(int nonce, long timestamp, ArrayList<Transaction> transactions) {
        this.transactions = transactions;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.merkleRoot = calculateMerkleRoot();
    }

    public Block(int nonce, long timestamp, ArrayList<Transaction> transactions, byte[] previousHash) {
        this.transactions = transactions;
        this.previousHash = previousHash;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.merkleRoot = calculateMerkleRoot();
    }

    public byte[] getMerkleRoot() {
        return merkleRoot;
    }

    public boolean isNonceValid() {
        int i;
        for (i = 0; i < numZeros; i++) {
            if (hash[i] != 0) {
                break;
            }
        }

        return i >= numZeros;
    }

    // Getters and setters
    public byte[] getHash() {
        return hash;
    }

    public static Block fromGrpc(kBlock block) {
        Block newBlock = new Block(block.getNonce(), block.getTimestamp());

        for (kademlia_public_ledger.kTransaction transaction : block.getTransactionsList()) {
            newBlock.transactions.add(Transaction.fromGrpc(transaction));
        }

        return newBlock;
    }

    public kBlock toGrpc(byte[] sender) {
        kBlock.Builder builder = kBlock.newBuilder()
                .setNonce(nonce)
                .setSender(ByteString.copyFrom(sender))
                .setTimestamp(timestamp)
                .addAllTransactions(transactions.stream().map(Transaction::toGrpc).collect(Collectors.toList()));

        if (previousHash != null) {
            builder.setPrevHash(ByteString.copyFrom(previousHash));
        }

        return builder.build();
    }

    public void setNonce(int nonce) {
        this.nonce = nonce;
        this.hash = KeysManager.hash(new Object[]{nonce, previousHash, merkleRoot, timestamp});
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

    public long getTimestamp() {
        return timestamp;
    }

    public int getNonce() {
        return nonce;
    }

    public byte[] calculateMerkleRoot() {
        List<byte[]> hashes = new ArrayList<>();

        for (int i = 0; i < transactions.size(); i += 1) {
            // hash the transaction
            hashes.add(transactions.get(i).hash());
        }
        hashes.sort(Comparator.comparing(t -> Arrays.toString(t)));

        return calculateMerkleRoot(hashes);
    }

    private byte[] calculateMerkleRoot(List<byte[]> hashes) {
        if (hashes.size() == 1) {
            return hashes.get(0);
        }

        List<byte[]> newHashes = new ArrayList<>();

        for (int i = 0; i < hashes.size(); i += 2) {
            newHashes.add(calculateHash(hashes.get(i), hashes.get(i + 1)));
        }

        return calculateMerkleRoot(newHashes);
    }

    private byte[] calculateHash(byte[] hash1, byte[] hash2) {
        byte[] data = new byte[hash1.length + hash2.length];
        System.arraycopy(hash1, 0, data, 0, hash1.length);
        System.arraycopy(hash2, 0, data, hash1.length, hash2.length);

        // Using SHA-256 as an example hash function
        Digest digest = new SHA256Digest();
        byte[] hash = new byte[digest.getDigestSize()];

        digest.update(data, 0, data.length);
        digest.doFinal(hash, 0);

        return hash;
    }

    public byte[] calculateHash() {
        return KeysManager.hash(new Object[]{nonce, previousHash, merkleRoot, timestamp});
    }

    public void store() {
        File file = new File("blockchain/blocks" + KeysManager.hexString(hash) + ".block");
        
        try {
            FileOutputStream fileOutputStream = new FileOutputStream(file);
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(fileOutputStream);
            objectOutputStream.writeObject(this);
            objectOutputStream.close();
            fileOutputStream.close();

            //store the transactions
            for (Transaction t : transactions) {
                t.store();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
