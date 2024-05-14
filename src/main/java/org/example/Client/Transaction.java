package org.example.Client;

import kademlia_public_ledger.TransactionKey;
import kademlia_public_ledger.kTransaction;

import java.util.Optional;

public abstract class Transaction {

    public static Transaction fromGrpc(kademlia_public_ledger.kTransaction transaction) {
        if (transaction.hasBid()) {
            return Bid.fromGrpc(transaction.getBid());
        } else {
            return Auction.fromGrpc(transaction.getAuction());
        }
    }

    public abstract void store();

    public static kTransaction load(TransactionKey key) {
        switch (key.getType()) {
            case bid:
                Optional<Bid> bid = Bid.load(key.getKey().toByteArray());
                if (bid.isPresent()) {
                    return bid.get().toGrpc();
                }
            case auction:
                Optional<Auction> auction = Auction.load(key.getKey().toByteArray());
                if (auction.isPresent()) {
                    return auction.get().toGrpc();
                }
            default:
                return kTransaction.newBuilder().build();
        }
    }

    public abstract kademlia_public_ledger.kTransaction toGrpc(byte[] senderID);

    public abstract kademlia_public_ledger.kTransaction toGrpc();

    public abstract boolean isValid();

    public abstract byte[] hash();

    @Override
    public abstract String toString();

}
