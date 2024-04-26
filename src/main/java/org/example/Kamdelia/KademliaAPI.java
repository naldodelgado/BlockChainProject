package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import kademlia_public_ledger.*;
import org.example.CryptoUtils.KeysManager;

import java.util.logging.Logger;

class KademliaAPI extends ServicesGrpc.ServicesImplBase {
    private final RouteTable routeTable;
    private final Logger log;

    public KademliaAPI(RouteTable routeTable, Logger log) {
        this.routeTable = routeTable;
        this.log = log;
    }

    @Override
    public void ping(Sender request, StreamObserver<Node> responseObserver) {
        routeTable.add(new KNode(request.getKey().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), Constant.PORT_HEADER_KEY.get()));

        log.info("Ping request from " + KeysManager.hexString(request.getKey().toByteArray()) + " " + Constant.IP_HEADER_KEY.get() + ":" + Constant.PORT_HEADER_KEY.get());

        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .setIp(ByteString.copyFrom(routeTable.getIP().getAddress()))
                .setPort(routeTable.getPort())
                .build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void storeBlock(kBlock request, StreamObserver<Node> responseObserver) {
        log.info("StoreBlock request from " + KeysManager.hexString(request.getSender().toByteArray()) + " " + Constant.IP_HEADER_KEY.get() + ":" + Constant.PORT_HEADER_KEY.get());
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), Constant.PORT_HEADER_KEY.get()));

        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void findNode(KeyWithSender request, StreamObserver<KBucket> responseObserver) {
        log.info("FindNode request from " + KeysManager.hexString(request.getKey().toByteArray()) + " " + Constant.IP_HEADER_KEY.get() + ":" + Constant.PORT_HEADER_KEY.get());

        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), Constant.PORT_HEADER_KEY.get()));

        KBucket response = KBucket.newBuilder()
                .addAllNodes(routeTable.findNode(request.getKey().toByteArray()))
                .setSender(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void storeTransaction(kTransaction request, StreamObserver<Node> responseObserver){
        log.info("StoreTransaction request from " + KeysManager.hexString(request.getSender().toByteArray()) + " " + Constant.IP_HEADER_KEY.get() + ":" + Constant.PORT_HEADER_KEY.get());

        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), Constant.PORT_HEADER_KEY.get()));
        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        routeTable.propagate(request);
    }

    @Override
    public void findBlock(KeyWithSender request, StreamObserver<kBlock> responseObserver) {
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), Constant.PORT_HEADER_KEY.get()));

    }

    @Override
    public void findTransaction(KeyWithSender request, StreamObserver<kTransaction> responseObserver) {
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), Constant.PORT_HEADER_KEY.get()));

    }

}
