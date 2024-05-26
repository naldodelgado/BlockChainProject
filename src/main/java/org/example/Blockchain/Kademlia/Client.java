package org.example.Blockchain.Kademlia;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import kademlia_public_ledger.*;
import org.example.Utils.NetUtils;

import java.lang.Boolean;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

import static org.example.Utils.NetUtils.IPtoString;

class Client {

    private final KNode node;
    private final byte[] id;
    private final int port;
    private static final Logger log = Logger.getLogger(Client.class.getName());

    Client(KNode node, byte[] senderID, int port) {
        this.node = node;
        this.id = senderID;
        this.port = port;
    }

    public Optional<TransactionOrBucket> findTransaction(byte[] key, Type type) {
        return makeCall(
                stub -> stub.findTransaction(TransactionKey.newBuilder()
                        .setSender(ByteString.copyFrom(id))
                        .setPort(port)
                        .setKey(ByteString.copyFrom(key))
                        .setType(type)
                        .build()),
                Validator::validate
        );
    }

    public Optional<Node> storeTransaction(kTransaction transaction) {
        return makeCall(
                stub -> stub.storeTransaction(transaction),
                Validator::validate
        );
    }

    public Optional<kademlia_public_ledger.Boolean> hasTransaction(TransactionKey key) {
        return makeCall(
                stub -> stub.hasTransaction(key),
                Validator::validate
        );
    }

    public Optional<BlockOrKBucket> findBlock(KeyWithSender key) {
        return makeCall(
                stub -> stub.findBlock(key),
                Validator::validate
        );
    }

    public Optional<KBucket> findNode(KeyWithSender key) {
        return makeCall(
                stub -> stub.findNode(key),
                Validator::validate
        );
    }

    public Optional<Node> ping(Sender sender) {
        return makeCall(
                stub -> stub.ping(sender),
                Validator::validate
        );
    }

    public Optional<Node> storeBlock(kBlock block) {
        return makeCall(
                stub -> stub.storeBlock(block),
                Validator::validate
        );
    }

    public Optional<kademlia_public_ledger.Boolean> hasBlock(KeyWithSender key) {
        return makeCall(
                stub -> stub.hasBlock(key),
                Validator::validate
        );
    }

    private <T> Optional<T> makeCall(Function<ServicesGrpc.ServicesBlockingStub, T> callFunction, Function<T, Boolean> Validator) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(IPtoString(node.getIp()), port).usePlaintext().build();
        ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);
        T response = null;

        for (int i = 0; i < 3; i++) {
            try {
                response = callFunction.apply(stub);
                if (Validator.apply(response)) break;
            } catch (StatusRuntimeException e) {
                log.info(String.format("client %s:%s this not respond. Error: %s", NetUtils.IPtoString(node.getIp()), node.getPort(), e.getStatus()));
                channel.shutdown();
                channel = ManagedChannelBuilder.forAddress(IPtoString(node.getIp()), port).usePlaintext().build();
            }
        }

        channel.shutdown();
        return Optional.ofNullable(response);
    }
}
