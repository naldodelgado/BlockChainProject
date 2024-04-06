package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import io.grpc.*;
import kademlia_public_ledger.*;
import org.apache.commons.lang3.tuple.Pair;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

class RouteTable {
    private final byte[] id;
    private static final int K = 5;
    private final Queue<Pair<KNode,ScheduledFuture<?>>>[] kBuckets;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final Logger log;
    private Function<Block, Boolean> blockStorageFunction;
    private Function<Transaction, Boolean> transactionStorageFunction;

    public RouteTable(byte[] id, Logger logger) {
        log = logger;
        this.id = id;
        kBuckets = new Queue[id.length * 8 - 1];
        for (int i = 0; i < id.length * 8 - 1; i++) {
            kBuckets[i] = new ArrayBlockingQueue<>(Math.min(K, 1 << Math.min(i, 31)));
        }
    }

    public void start() {
        new Thread(() -> {
            // try to fill k-buckets with nodes from the network
            try {
                Channel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(Kademlia.genesisIP).getHostAddress(), Kademlia.genesisPort).usePlaintext().build();
                ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);
                log.info("Finding nodes in the network");

                KBucket response = stub.findNode(Node.newBuilder().setId(ByteString.copyFrom(id)).build());
                log.info(String.format("Found %d nodes in the network", response.getNodesCount()));

                List<Node> nodes = response.getNodesList();
                add(nodes);

                for (Node node : nodes) {
                    channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(node.getIp().toByteArray()).getHostAddress(), node.getPort()).usePlaintext().build();
                    stub = ServicesGrpc.newBlockingStub(channel);

                    log.info(String.format("Finding nodes in the network from node %s %s", Arrays.toString(node.getIp().toByteArray()), Arrays.toString(node.getId().toByteArray())));

                    add(stub.findNode(node).getNodesList());
                }

            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

    public Iterable<Node> findNode(ByteString id) {
        int index = BitSet.valueOf(distance(id.toByteArray(), this.id)).nextSetBit(0) - 1;

        if (index > -1) {
            return kBuckets[index].stream().map(Pair::getLeft).map(KNode::toNode).collect(Collectors.toCollection(ArrayList::new));
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
        int index = BitSet.valueOf(distance(kNode.getId(), id)).nextSetBit(0) - 1;
        if (index > -1) {
            add(kNode, index);
        }
    }

     private void add(KNode kNode, int index) {
        if (kBuckets[index].stream().map(Pair::getLeft).anyMatch(k -> Arrays.equals(k.getId(), kNode.getId()))){
            sendToFirst(kBuckets[index], kNode);
        }else{
            log.info(String.format("Adding node %s to the route table", Arrays.toString(kNode.getIp())));
            synchronized (kBuckets[index]){
                if (kBuckets[index].size() >= Math.min(K, 1 << Math.min(index, 31))) {
                    kBuckets[index].remove().getRight().cancel(false);
                }
                kBuckets[index].add(Pair.of(kNode, executorService.schedule(() -> refresh(kNode, index), 1, TimeUnit.HOURS)));
            }
        }
    }

    public void propagate(Block request) {
        log.info(String.format("Storing block with hash %s", Arrays.toString(request.getHash().toByteArray())));
        KNode sender = KNode.fromNode(request.getSender());

        int senderDistance = (id.length * 8 - 2) - BitSet.valueOf(distance(sender.getId(), id)).nextSetBit(0);

        blockStorageFunction.apply(request);

        IntStream.range(0, senderDistance).parallel().forEach(i -> {
            while (true) {
                var temp = kBuckets[i].peek();
                if (temp == null)
                    break;

                KNode node = temp.getLeft();

                try {
                    log.info(String.format("Propagating Block with hash %s to node %s %s",
                            Arrays.toString(request.getHash().toByteArray()), Arrays.toString(node.getIp()), Arrays.toString(node.getId())));
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(node.getIp()).getHostAddress(), node.getPort()).build();
                    ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

                    Block block = Block.newBuilder()
                            .setHash(request.getHash())
                            .setSender(request.getSender())
                            .setPrevHash(request.getPrevHash())
                            .setNonce(request.getNonce())
                            .setTimestamp(request.getTimestamp())
                            .build();

                    stub.storeBlock(block);

                    sendToFirst(kBuckets[i], node);

                    break;
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                } catch (StatusRuntimeException e) {
                    log.info(String.format("Node %s %s did not respond to store error %s",
                            Arrays.toString(node.getIp()), Arrays.toString(node.getId()), e.getStatus().getCode()));
                    kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
                }
            }
        });
    }

    public void propagate(Transaction data) {
        log.info(String.format("Storing transaction with signature %s", Arrays.toString(data.getSignature().toByteArray())));
        KNode sender = KNode.fromNode(data.getSenderNode());

        int senderDistance = (id.length * 8 - 2) - BitSet.valueOf(distance(sender.getId(), id)).nextSetBit(0);

        transactionStorageFunction.apply(data);

        IntStream.range(0, senderDistance).parallel().forEach(i -> {
            while (true) {
                var temp = kBuckets[i].peek();
                if (temp == null)
                    break;

                KNode node = temp.getLeft();

                try {
                    log.info(String.format("Propagating Transaction with signature %s to node %s %s",
                            Arrays.toString(data.getSignature().toByteArray()), Arrays.toString(node.getIp()), Arrays.toString(node.getId())));
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(node.getIp()).getHostAddress(), node.getPort()).build();
                    ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

                    Transaction.Builder transaction = Transaction.newBuilder()
                            .setSignature(data.getSignature())
                            .setSender(data.getSender())
                            .setReceiver(data.getReceiver())
                            .setAmount(data.getAmount())
                            .setTimestamp(data.getTimestamp());

                    if (data.hasSenderNode())
                        transaction.setSenderNode(data.getSenderNode());
                    else
                        transaction.setSenderNode(Node.newBuilder().setId(ByteString.copyFrom(id)).setIp(ByteString.copyFrom(Inet4Address.getLocalHost().getAddress())).setPort(5000));

                    stub.storeTransaction(transaction.build());

                    sendToFirst(kBuckets[i], node);

                    break;
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                } catch (StatusRuntimeException e) {
                    log.info(String.format("Node %s %s did not respond to store error %s",
                            Arrays.toString(node.getIp()), Arrays.toString(node.getId()), e.getStatus().getCode()));
                    kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
                }
            }
        });
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
                // doesn't need to cancel the future
                kBuckets[index].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
                return;
            }

            sendToFirst(kBuckets[index], node);

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }


    /*
    public byte[] getValue(byte[] key) {
        byte[] distance = distance(key, id);
        if (BitSet.valueOf(distance).nextSetBit(0) == -1) {
            //TODO: return value
            return null;
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


                if (!value.getValue().isEmpty() && value.getKey().equals(ByteString.copyFrom(key))){
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
    */

    private void sendToFirst(Queue<Pair<KNode,ScheduledFuture<?>>> kBucket, KNode node){
        synchronized (kBuckets) {
            kBucket.stream().filter(pair -> Arrays.equals(pair.getLeft().getId(), node.getId())).forEach(pair -> pair.getRight().cancel(false));
            if(kBucket.removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId())))
                kBucket.add(Pair.of(node, executorService.schedule(() -> refresh(node, 0), 1, TimeUnit.HOURS)));
        }
    }

    public static byte[] distance(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    public byte[] getId() {
        return id;
    }

    public void setBlockStorageFunction(Function<Block, Boolean> blockStorageFunction) {
        this.blockStorageFunction = blockStorageFunction;
    }

    public void setTransactionStorageFunction(Function<Transaction, Boolean> transactionStorageFunction) {
        this.transactionStorageFunction = transactionStorageFunction;
    }

    public void shutdown() {
        executorService.shutdown();
    }

}
