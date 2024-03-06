package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import kademlia_public_ledger.*;

import java.net.Inet4Address;
import java.net.UnknownHostException;

public class KademliaAPI extends ServicesGrpc.ServicesImplBase {

    RouteTable routeTable;

    public KademliaAPI(RouteTable routeTable) {
        this.routeTable = routeTable;
    }

    @Override
    public void ping(Node request, StreamObserver<Node> responseObserver) {
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
        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        routeTable.add(KNode.fromNode(request.getSender()));
    }

    @Override
    public void findNode(Node request, StreamObserver<KBucket> responseObserver) {
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
