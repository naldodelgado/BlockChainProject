package org.example.Blockchain;

import com.google.protobuf.ByteString;
import kademlia_public_ledger.kBlock;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.example.Client.Transaction;
import org.example.Utils.FileSystem;
import org.example.Utils.KeysManager;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class Block implements Serializable {
    private int numberOfOrder;
    private byte[] hash = new byte[32]; // Hash of this block - will only be defined after mining
    private byte[] previousHash = new byte[32]; // Hash of the previous block - will only be defined after mining
    public static final int TRANSACTION_PER_BLOCK = 8; // Maximum number of transactions in a block must be power of 2
    private final ArrayList<Transaction> transactions; // List of transactions in this block
    private final long timestamp; // Timestamp of when this block was created
    private int nonce; // Nonce used in mining - this can only be set once. IMMUTABLE
    private byte[] merkleRoot = new byte[32]; // Merkle root of the transactions in this block
    static int numZeros = 0; // Number of zeros required at the start of the hash


    // Constructor

    // constructor genesis block
    public Block (){
        this.transactions = new ArrayList<>();
        this.timestamp = Date.parse("2024-01-01 00:00:00");
        this.nonce = 0;
        this.numberOfOrder = 0;
        try {
            PrintWriter writer = new PrintWriter(FileSystem.UtilsPath);
            writer.print(this.numberOfOrder);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public Block(int nonce) {
        if(Paths.get(FileSystem.UtilsPath).toFile().exists()){
            try {
                Scanner scanner = new Scanner(new File(FileSystem.UtilsPath));
                this.numberOfOrder = scanner.nextInt() + 1;
                scanner.close();
                PrintWriter writer = new PrintWriter(FileSystem.UtilsPath);
                writer.print(this.numberOfOrder);
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        this.transactions = new ArrayList<>();
        this.nonce = nonce;
        this.timestamp = new Date().getTime();
    }

    public Block(int nonce, long timestamp) {
        if(Paths.get(FileSystem.UtilsPath).toFile().exists()){
            try {
                Scanner scanner = new Scanner(new File(FileSystem.UtilsPath));
                this.numberOfOrder = scanner.nextInt() + 1;
                scanner.close();
                PrintWriter writer = new PrintWriter(FileSystem.UtilsPath);
                writer.print(this.numberOfOrder);
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        this.transactions = new ArrayList<>();
        this.nonce = nonce;
        this.timestamp = timestamp;
    }

    public Block(int nonce, long timestamp, ArrayList<Transaction> transactions) {
        if(Paths.get(FileSystem.UtilsPath).toFile().exists()){
            try {
                Scanner scanner = new Scanner(new File(FileSystem.UtilsPath));
                this.numberOfOrder = scanner.nextInt() + 1;
                scanner.close();
                PrintWriter writer = new PrintWriter(FileSystem.UtilsPath);
                writer.print(this.numberOfOrder);
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        this.transactions = transactions;
        this.nonce = nonce;
        this.timestamp = timestamp;
        this.merkleRoot = calculateMerkleRoot();
    }

    public Block(int nonce, long timestamp, ArrayList<Transaction> transactions, byte[] previousHash) {
        if(Paths.get(FileSystem.UtilsPath).toFile().exists()){
            try {
                Scanner scanner = new Scanner(new File(FileSystem.UtilsPath));
                this.numberOfOrder = scanner.nextInt() + 1;
                scanner.close();
                PrintWriter writer = new PrintWriter(FileSystem.UtilsPath);
                writer.print(this.numberOfOrder);
                writer.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
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

        for (Transaction transaction : transactions) {
            // hash the transaction
            hashes.add(transaction.hash());
        }
        hashes.sort(Comparator.comparing(Arrays::toString));

        return calculateMerkleRoot(hashes);
    }

    private byte[] calculateMerkleRoot(List<byte[]> hashes) {
        if (hashes.size() == 1) {
            return hashes.get(0);
        }

        List<byte[]> newHashes = new ArrayList<>();

        for (int i = 0; i < hashes.size(); i += 2) {
            newHashes.add(calculateHash(hashes.get(i), hashes.get(i + 1)));
            //TODO: this is giving index out of bounds exception when using an odd number of transactions
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

    public boolean isValid() {
        if (numberOfOrder < 0)
            return false;

        if (previousHash == null)
            return false;

        if (transactions.size() != TRANSACTION_PER_BLOCK)
            return false;

        if (timestamp > 1704067200) //unix time for start of 2024
            return false;

        if (!isNonceValid())
            return false;

        if (merkleRoot == null || !Arrays.equals(merkleRoot, calculateMerkleRoot()))
            return false;

        return hash != null && Arrays.equals(hash, calculateHash());
    }

    public void store() {
        String filePath = FileSystem.blockchainPath + KeysManager.hexString(hash) + ".block";
        try {
            FileOutputStream fileOut = new FileOutputStream(filePath);
            ObjectOutputStream out = new ObjectOutputStream(fileOut);
            out.writeObject(this);
            out.close();
            fileOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static Optional<Block> load(byte[] key) {
        try {
            FileInputStream fileIn = new FileInputStream(KeysManager.hexString(key));
            ObjectInputStream in = new ObjectInputStream(fileIn);
            Block block = (Block) in.readObject();
            in.close();
            fileIn.close();
            if (block != null) return Optional.of(block);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Error reading to file: " + e.getMessage());
            e.printStackTrace();
        }
        return Optional.empty();
    }

    public int getNumberOfOrder() {
        return this.numberOfOrder;
    }
}
