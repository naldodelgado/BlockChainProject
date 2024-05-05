package org.example.Client;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Optional;

public abstract class Transaction {

    public static Transaction fromGrpc(kademlia_public_ledger.kTransaction transaction) {
        if (transaction.hasBid()) {
            return Bid.fromGrpc(transaction.getBid());
        } else {
            return Auction.fromGrpc(transaction.getAuction());
        }
    }

    public static Optional<Transaction> fromStorage(String id) {
        return Optional.empty();
    }

    private void storeTransactionOnDisk(){
        switch (this.getClass().getSimpleName()){
            case "Bid":
                String filePath = "Transactions/Bids/" + Arrays.toString(this.hash()) + ".bid";
                try (PrintWriter writer = new PrintWriter(new FileWriter(filePath))) {
                    writer.println("Bid");
                    writer.println(Arrays.toString(this.hash()));
                    writer.println(((Bid)this).getAmount());
                    //TODO: write bid details
                } catch (IOException e) {
                    System.err.println("Error writing to file: " + e.getMessage());
                }
                break;
            case "Auction":
                String filePath2 = "Transactions/Auctions/" + Arrays.toString(this.hash()) + ".auction";
                this.store();
                try (PrintWriter writer = new PrintWriter(new FileWriter(filePath2))) {

                    //TODO: write auction details
                } catch (IOException e) {
                    System.err.println("Error writing to file: " + e.getMessage());
                }
                break;
        }
    }

    public abstract kademlia_public_ledger.kTransaction toGrpc(byte[] senderID);

    public abstract kademlia_public_ledger.kTransaction toGrpc();

    public abstract boolean verify();

    public abstract void store();

    public abstract Transaction load(String filePath);

    public abstract byte[] hash();

    @Override
    public abstract String toString();

}
