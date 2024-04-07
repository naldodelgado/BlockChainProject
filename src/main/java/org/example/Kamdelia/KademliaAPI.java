package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import kademlia_public_ledger.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.logging.Logger;

class KademliaAPI extends ServicesGrpc.ServicesImplBase {
    private final RouteTable routeTable;
    private final Logger log;

    public KademliaAPI(RouteTable routeTable, Logger log) {
        this.routeTable = routeTable;
        this.log = log;
    }

    @Override
    public void ping(Node request, StreamObserver<Node> responseObserver) {
        log.info("Ping from " + request.getId().toStringUtf8());
        try{
            Node response = Node.newBuilder()
                    .setId(ByteString.copyFrom(routeTable.getId()))
                    .setIp(ByteString.copyFrom(Inet4Address.getLocalHost().getAddress()))
                    //TODO: set port
                    .setPort(5000)
                    .build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();

            routeTable.add(KNode.fromNode(request));
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void findNode(Node request, StreamObserver<KBucket> responseObserver) {
        log.info("FindNode request from " + request.getId().toStringUtf8());
        KBucket response = KBucket.newBuilder()
                .addAllNodes(routeTable.findNode(request.getId()))
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

        routeTable.add(KNode.fromNode(request));
    }

    @Override
    public void storeBlock(kBlock request, StreamObserver<Node> responseObserver){
        log.info("StoreBlock request from " + request.getSender().getId().toStringUtf8());
        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        routeTable.add(KNode.fromNode(request.getSender()));

        routeTable.propagate(request);
    }

    @Override
    public void storeTransaction(kTransaction request, StreamObserver<Node> responseObserver){
        log.info("StoreTransaction request from " + Arrays.toString(request.getSender().toByteArray()));
        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        routeTable.add(KNode.fromNode(request.getSenderNode()));

        routeTable.propagate(request);
    }

}
