package org.example.Blockchain.Kademlia;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import kademlia_public_ledger.ServicesGrpc;
import kademlia_public_ledger.TransactionKey;
import kademlia_public_ledger.TransactionOrBucket;
import kademlia_public_ledger.Type;

import java.util.Optional;
import java.util.function.Function;

import static org.example.Utils.NetUtils.IPtoString;

class Client {

    private final KNode node;
    private final byte[] id;
    private final int port;

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


    private <T> Optional<T> makeCall(Function<ServicesGrpc.ServicesBlockingStub, T> callFunction, Function<T, Boolean> Validator) {
        ManagedChannel channel = ManagedChannelBuilder.forAddress(IPtoString(node.getIp()), port).usePlaintext().build();
        ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);
        T response = null;

        for (int i = 0; i < 3; i++) {
            try {
                response = callFunction.apply(stub);
                if (Validator.apply(response)) break;
            } catch (StatusRuntimeException ignored) {
            }
        }

        channel.shutdown();
        return Optional.ofNullable(response);
    }
}
