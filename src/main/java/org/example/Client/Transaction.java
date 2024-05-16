package org.example.Client;

import kademlia_public_ledger.TransactionKey;
import kademlia_public_ledger.Type;
import kademlia_public_ledger.kTransaction;

import java.util.Optional;

public abstract class Transaction {

    public static Transaction fromGrpc(kTransaction transaction) {
        if (transaction.hasBid()) {
            return Bid.fromGrpc(transaction.getBid());
        } else {
            return Auction.fromGrpc(transaction.getAuction());
        }
    }

    public Type getType() {
        if (this instanceof Auction)
            return Type.auction;
        return Type.bid;
    }

    public abstract void store();

    public static Optional<Transaction> load(TransactionKey key) {
        switch (key.getType()) {
            case bid:
                return Bid.load(key.getKey().toByteArray());
            case auction:
                return Auction.load(key.getKey().toByteArray());
            default:
                return Optional.empty();
        }
    }

    public abstract kTransaction toGrpc(byte[] senderID);

    public abstract kTransaction toGrpc();

    public abstract boolean isValid();

    public abstract byte[] hash();

    @Override
    public abstract String toString();

}
