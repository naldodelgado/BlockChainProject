package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import kademlia_public_ledger.Data;
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
        log = logger;
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
        int index = BitSet.valueOf(distance(id.toByteArray(), this.id)).nextSetBit(0) - 1;

        if (index > -1) {
            synchronized (kBuckets[index]){
                if (kBuckets[index].size() < Math.min(K, 20)) {
                    return kBuckets[index].stream().map(Pair::getLeft).map(KNode::toNode).collect(Collectors.toCollection(ArrayList::new));
                }
            }
        }

        try {
            if (index == -1)
                return Collections.singleton(
                    Node.newBuilder()
                        .setId(ByteString.copyFrom(this.id))
                        .setIp(ByteString.copyFrom(Inet4Address.getLocalHost().getAddress()))
                        .setPort(5000)
                        .build());
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return Collections.emptyList();
    }

    public void add(List<Node> nodes) {
        nodes.stream().map(KNode::fromNode).forEach(this::add);
    }

    public void add(KNode kNode) {
        log.info(String.format("Adding node %s to the route table", Arrays.toString(kNode.getIp())));

        int index = BitSet.valueOf(distance(kNode.getId(), id)).nextSetBit(0) - 1;
        if (index > -1) {
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
            log.info(String.format("Refreshing node %s %s", Arrays.toString(node.getIp()), Arrays.toString(node.getId())));
            ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(node.getIp()).getHostAddress(), node.getPort()).build();
            ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

            try {
                stub.ping(
                        Node.newBuilder()
                                .setId(ByteString.copyFrom(id))
                                //TODO: set port
                                .setPort(5000)
                                .setIp(ByteString.copyFrom(Inet4Address.getLocalHost().getAddress()))
                                .build()
                );
            } catch (StatusRuntimeException e) {
                log.info(String.format("Node %s %s did not respond to ping", Arrays.toString(node.getIp()), Arrays.toString(node.getId())));
                kBuckets[index].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
                return;
            }

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


    public byte[] getValue(byte[] key) {
        byte[] distance = distance(key, id);
        if (BitSet.valueOf(distance).nextSetBit(0) == -1) {
            return null;
            //TODO: get value from local storage
        }

        try {
            log.info(String.format("Finding value for key %s", Arrays.toString(key)));
            int index = BitSet.valueOf(distance(key, id)).nextSetBit(0) - 1;
            List<Node> nodes = new ArrayList<>();
            if (index > -1) {
                synchronized (kBuckets[index]){
                    if (!kBuckets[index].isEmpty()) {
                        nodes = kBuckets[index].stream().map(Pair::getLeft).sorted( (a, b) -> {
                            byte[] aDistance = distance(a.getId(), key);
                            byte[] bDistance = distance(b.getId(), key);

                            for (int i = 0; i < aDistance.length; i++) {
                                if (aDistance[i] != bDistance[i]) {
                                    return aDistance[i] - bDistance[i];
                                }
                            }
                            return 0;
                        }).map(KNode::toNode).collect(Collectors.toList());
                    }
                }
            }
            Node thisNode = Node.newBuilder().setId(ByteString.copyFrom(id)).setIp(ByteString.copyFrom(Inet4Address.getLocalHost().getAddress())).setPort(5000).build();

            while (!nodes.isEmpty()) {
                log.info(String.format("Retrieving data with key %s from node %s %s", Arrays.toString(key), Arrays.toString(nodes.get(0).getId().toByteArray()), Arrays.toString(nodes.get(0).getIp().toByteArray())));
                ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(nodes.get(0).getIp().toByteArray()).getHostAddress(), nodes.get(0).getPort()).build();
                ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

                Data value;
                try {
                     value = stub.findValue(kademlia_public_ledger.Key.newBuilder().setKey(ByteString.copyFrom(key)).setSender(thisNode).build());
                }catch (StatusRuntimeException e){
                    log.info(String.format("Node %s did not respond to findValue", Arrays.toString(nodes.get(0).getId().toByteArray())));
                    nodes.remove(0);
                    continue;
                }

                if (value.getValue() != null && !value.getValue().isEmpty() && value.getKey().equals(ByteString.copyFrom(key))){
                    log.info(String.format("Found value for key %s", Arrays.toString(key)));
                    return value.getValue().toByteArray();
                }

                log.info(String.format("Node %s did not have value for key %s", Arrays.toString(nodes.get(0).getId().toByteArray()), Arrays.toString(key)));
                nodes.remove(0);
            }

            log.info(String.format("No nodes had value for key %s", Arrays.toString(key)));
        }   catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }

        return null;
    }

    public void shutdown() {
        executorService.shutdown();
    }
}
