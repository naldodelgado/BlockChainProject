package org.example.Client;

public abstract class Transaction {

    public static Transaction fromGrpc(kademlia_public_ledger.kTransaction transaction) {
        if (transaction.hasBid()) {
            return Bid.fromGrpc(transaction.getBid());
        } else {
            return Auction.fromGrpc(transaction.getAuction());
        }
    }

    public abstract kademlia_public_ledger.kTransaction toGrpc(byte[] senderID);

    public abstract kademlia_public_ledger.kTransaction toGrpc();

    public abstract boolean isValid();

    public abstract void store();

    public abstract Transaction load(String filePath);

    public abstract byte[] hash();

    @Override
    public abstract String toString();

}
