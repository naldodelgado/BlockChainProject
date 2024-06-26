package org.example.Blockchain.Kademlia;

import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import kademlia_public_ledger.Boolean;
import kademlia_public_ledger.*;
import org.example.Utils.KeysManager;
import org.example.Utils.LogFilter;
import org.example.Utils.NetUtils;

import java.util.logging.Logger;

class KademliaAPI extends ServicesGrpc.ServicesImplBase {
    private final RouteTable routeTable;
    private final Logger logger = Logger.getLogger(KademliaAPI.class.getName());

    public KademliaAPI(RouteTable routeTable) {
        this.routeTable = routeTable;
        logger.setFilter(new LogFilter());
    }

    @Override
    public void ping(Sender request, StreamObserver<Node> responseObserver) {
        routeTable.add(new KNode(request.getKey().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), request.getPort()));

        logger.info("received Ping request from " + KeysManager.hexString(request.getKey().toByteArray()) + " " + Constant.IP_HEADER_KEY.get() + ":" + request.getPort());

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
        logger.info("received StoreBlock request from " + KeysManager.hexString(request.getSender().toByteArray()) + " " + Constant.IP_HEADER_KEY.get() + ":" + request.getPort());
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), request.getPort()));

        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        routeTable.propagate(request);
    }

    @Override
    public void storeTransaction(kTransaction request, StreamObserver<Node> responseObserver){
        logger.info("received StoreTransaction request from " + KeysManager.hexString(request.getSender().toByteArray()) + " " + Constant.IP_HEADER_KEY.get() + ":" + request.getPort());
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), request.getPort()));
        Node response = Node.newBuilder()
                .setId(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();

        routeTable.propagate(request);
    }

    @Override
    public void findNode(KeyWithSender request, StreamObserver<KBucket> responseObserver) {
        logger.info("received FindNode request from " + KeysManager.hexString(request.getKey().toByteArray()) + " " + Constant.IP_HEADER_KEY.get() + ":" + request.getPort());

        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), request.getPort()));

        KBucket response = KBucket.newBuilder()
                .addAllNodes(routeTable.findNode(request.getKey().toByteArray()))
                .setSender(ByteString.copyFrom(routeTable.getId()))
                .build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void findBlock(KeyWithSender request, StreamObserver<BlockOrKBucket> responseObserver) {
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), request.getPort()));

        responseObserver.onNext(routeTable.getValues(request));
        responseObserver.onCompleted();
    }

    @Override
    public void findTransaction(TransactionKey request, StreamObserver<TransactionOrBucket> responseObserver) {
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), request.getPort()));

        responseObserver.onNext(routeTable.getValues(request));
        responseObserver.onCompleted();
    }

    @Override
    public void hasTransaction(TransactionKey request, StreamObserver<Boolean> responseObserver) {
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), request.getPort()));

        responseObserver.onNext(Boolean.newBuilder().setValue(routeTable.hasTransaction(request)).build());
    }

    @Override
    public void hasBlock(KeyWithSender request, StreamObserver<Boolean> responseObserver) {
        routeTable.add(new KNode(request.getSender().toByteArray(), NetUtils.IPfromString(Constant.IP_HEADER_KEY.get()), request.getPort()));

        responseObserver.onNext(Boolean.newBuilder().setValue(routeTable.hasBlock(request)).build());
    }
}
