package org.example.Client;

public abstract class Transaction {

    public static Transaction fromGrpc(kademlia_public_ledger.kTransaction transaction) {
        if (transaction.hasBid()) {
            return Bid.fromGrpc(transaction.getBid());
        } else {
            return Auction.fromGrpc(transaction.getAuction());
        }
    }

    public static Transaction fromStorage(String id) {
        return null;
    }

    public abstract kademlia_public_ledger.kTransaction toGrpc();

    public abstract boolean verify();

    public abstract void store();

    public abstract byte[] hash();

    @Override
    public abstract String toString();

}
