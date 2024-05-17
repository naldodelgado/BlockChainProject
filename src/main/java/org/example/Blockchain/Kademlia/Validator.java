package org.example.Blockchain.Kademlia;

import kademlia_public_ledger.*;

public class Validator {

    public boolean validate(Sender request) {
        if (request.getPort() < 0 || request.getPort() > 65535)
            return false;

        return !request.getKey().isEmpty() && request.getKey().size() == 160;
    }

    public boolean validate(Node request) {
        if (request.getPort() < 0 || request.getPort() > 65535)
            return false;

        if (request.getIp().isEmpty() || request.getIp().size() != 4)
            return false;

        return !request.getId().isEmpty() && request.getId().size() == 160;
    }

    public boolean validate(kBlock request) {
        if (request.getPort() < 0 || request.getPort() > 65535)
            return false;

        if (request.getSender().isEmpty())
            return false;

        if (request.getSender().size() != 160)
            return false;

        return !request.getHash().isEmpty() && request.getHash().size() == 160;
        //the other attributes are validated in the blockchain
    }

    public boolean validate(kTransaction request) {
        if (request.getPort() < 0 || request.getPort() > 65535)
            return false;

        return !request.getSender().isEmpty() && request.getSender().size() == 160;
    }


    public boolean validate(KeyWithSender request) {
        if (request.getPort() < 0 || request.getPort() > 65535)
            return false;

        return !request.getSender().isEmpty() && request.getSender().size() == 160;
    }

    public boolean validate(KBucket request) {
        if (request.getPort() < 0 || request.getPort() > 65535)
            return false;

        if (request.getSender().isEmpty() || request.getSender().size() != 160) {
            return false;
        }

        return request.getNodesList().stream().map(this::validate).reduce(true, (t1, t2) -> t1 && t2);
    }

    public boolean validate(BlockOrKBucket request) {
        if (request.getIsNone())
            return true;

        if (request.hasBlock())
            return validate(request.getBlock());

        if (request.hasBucket())
            return validate(request.getBucket());

        return false;
    }

    public boolean validate(TransactionKey request) {
        if (request.getType() == Type.UNRECOGNIZED) {
            return false;
        }

        if (request.getKey().isEmpty() || request.getKey().size() != 160)
            return false;

        if (request.getPort() < 0 || request.getPort() > 65535)
            return false;

        return !request.getSender().isEmpty() && request.getKey().size() == 160;
    }

    public boolean validate(TransactionOrBucket request) {
        if (request.hasBucket()) {
            return validate(request.getBucket());
        }

        if (request.hasTransaction()) {
            return validate(request.getTransaction());
        }

        return false;
    }
}
