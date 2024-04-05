package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import kademlia_public_ledger.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class KademliaAPI extends ServicesGrpc.ServicesImplBase {
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
    public void store(Data request, StreamObserver<Node> responseObserver) {
        //TODO: store data
        log.info("Store request from " + request.getSender().getId().toStringUtf8());
        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        routeTable.add(KNode.fromNode(request.getSender()));

        routeTable.propagate(request.getKey().toByteArray(), request.getValue().toByteArray(), KNode.fromNode(request.getSender()));
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
    public void findValue(Key request, StreamObserver<Data> responseObserver) {

    }

}
