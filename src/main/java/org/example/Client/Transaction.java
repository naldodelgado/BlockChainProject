package org.example.Client;

public abstract class Transaction {

    public static Transaction fromGrpc(kademlia_public_ledger.kTransaction transaction) {
        if (transaction.hasBid()) {
            return Bid.fromGrpc(transaction.getBid());
        } else {
            return Auction.fromGrpc(transaction.getAuction());
        }
    }

    public static kademlia_public_ledger.kTransaction toGrpc(Transaction transaction) {
        if (transaction instanceof Bid) {
            return transaction.toGrpc();
        } else {
            return transaction.toGrpc();
        }
    }

    public abstract kademlia_public_ledger.kTransaction toGrpc();

    public abstract boolean verify();

    public abstract void store();

    public abstract byte[] hash();

}
