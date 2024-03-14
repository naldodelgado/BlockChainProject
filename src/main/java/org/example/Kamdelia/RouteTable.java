package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import kademlia_public_ledger.KBucket;
import kademlia_public_ledger.Node;
import kademlia_public_ledger.ServicesGrpc;
import org.apache.commons.lang3.tuple.Pair;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RouteTable {
    private final byte[] id;
    private static final int K = 20;
    private final Queue<Pair<KNode,ScheduledFuture<?>>>[] kBuckets;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    
    private final Logger log;

    public RouteTable(byte[] id, Logger logger) {
        log = Logger.getLogger(RouteTable.class.getName());
        this.id = id;
        kBuckets = new Queue[id.length * 8 - 1];
        for (int i = 0; i < K; i++) {
            kBuckets[i] = new LinkedBlockingQueue<>(Math.min(K, 2 ^ i));
        }
    }

    public byte[] getId() {
        return id;
    }

    public static byte[] distance(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    public void start() {
        new Thread(() -> {
            // try to fill k-buckets with nodes from the network
            try {
                Channel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(Kademlia.genesisIP).getHostAddress(), 5000).usePlaintext().build();
                ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);
                log.info("Finding nodes in the network");

                KBucket response = stub.findNode(Node.newBuilder().setId(ByteString.copyFrom(id)).build());
                log.info(String.format("Found %d nodes in the network", response.getNodesCount()));

                List<Node> nodes = response.getNodesList();
                add(nodes);

                for (Node node : nodes) {
                    add(stub.findNode(node).getNodesList());
                }

            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    // doesn't mutate the state of the route table, so it's safe to call from multiple threads
    public Iterable<Node> findNode(ByteString id) {
        var distance =  BitSet.valueOf(distance(id.toByteArray(), this.id));
        int index = 0;
        while (index < distance.length() && !distance.get(index)) {
            index++;
        }
        if (index < distance.length()) {
            synchronized (kBuckets[index]){
                if (kBuckets[index].size() < Math.min(K, 20)) {
                    return kBuckets[index].stream().map(Pair::getLeft).map(KNode::toNode).collect(Collectors.toCollection(ArrayList::new));
                }
            }
        }

        try {
            return Collections.singleton(
                Node.newBuilder()
                    .setId(ByteString.copyFrom(id.toByteArray()))
                    .setIp(ByteString.copyFrom(Inet4Address.getLocalHost().getAddress()))
                    //TODO: set port
                    .setPort(5000)
                    .build()
            );
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void add(List<Node> nodes) {
        KNode[] kNodes = nodes.stream().map(KNode::fromNode).toArray(KNode[]::new);


        for (KNode kNode : kNodes){
            add(kNode);
        }
    }

    public void add(KNode kNode) {
        log.info(String.format("Adding node %s to the route table", Arrays.toString(kNode.getIp())));
        byte[] distance = distance(kNode.getId(), id);
        int index = 0;
        BitSet bitSet = BitSet.valueOf(distance);
        while (index < distance.length && !bitSet.get(index)) {
            index++;
        }
        if (index < distance.length) {
            add(kNode, index);
        }
    }

     public void add(KNode kNode, int index) {
        if (kBuckets[index].stream().map(Pair::getLeft).anyMatch(k -> Arrays.equals(k.getId(), kNode.getId()))){
            refresh(kNode, index);
        }else {
            synchronized (kBuckets[index]){
                if (kBuckets[index].size() >= Math.min(K, 20)) {
                    kBuckets[index].remove().getRight().cancel(false);
                }
                kBuckets[index].add(Pair.of(kNode, executorService.schedule(() -> refresh(kNode, index), 3600, java.util.concurrent.TimeUnit.SECONDS)));
            }
        }
    }

    private void refresh(KNode node, int index) {

        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(node.getIp()).getHostAddress(), node.getPort()).usePlaintext().build();
            ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

            stub.ping(
                Node.newBuilder()
                    .setId(ByteString.copyFrom(id))
                    //TODO: set port
                    .setPort(5000)
                    .setIp(ByteString.copyFrom(Inet4Address.getLocalHost().getAddress()))
                    .build()
            );

            synchronized (kBuckets[index]){
                if (kBuckets[index].stream().map(Pair::getLeft).anyMatch(kNode -> Arrays.equals(kNode.getId(), node.getId()))){
                    kBuckets[index].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
                    //TODO: this is wrong
                    kBuckets[index].add(Pair.of(node, executorService.schedule( () -> refresh(node, index), 3600, java.util.concurrent.TimeUnit.SECONDS)));
                }
            }

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    private int getBucketIndex(byte[] distance) {
        int index = 0;
        BitSet bitSet = BitSet.valueOf(distance);
        while (index < distance.length && !bitSet.get(index)) {
            index++;
        }
        return index;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}