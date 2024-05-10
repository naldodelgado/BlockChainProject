package org.example.Blockchain.Kademlia;

import com.google.protobuf.ByteString;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder;
import kademlia_public_ledger.*;
import org.apache.commons.lang3.tuple.Pair;
import org.example.Blockchain.Block;
import org.example.Blockchain.BlockChain;
import org.example.Client.Transaction;
import org.example.Utils.KeysManager;
import org.example.Utils.NetUtils;
import org.example.poisson.PoissonProcess;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.example.Blockchain.Kademlia.Kademlia.genesisIP;
import static org.example.Blockchain.Kademlia.Kademlia.genesisPort;
import static org.example.Utils.NetUtils.IPtoString;

class RouteTable {
    private final byte[] id;
    private static final int K = 20;
    private final Queue<Pair<KNode,ScheduledFuture<?>>>[] kBuckets;
    private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private final Logger logger = Logger.getLogger(RouteTable.class.getName());
    private final InetAddress myIp = Inet4Address.getLocalHost();
    private final int myPort = 5000;
    private final PoissonProcess poissonProcess = new PoissonProcess(4, new Random((int) (Math.random() * 1000)));
    private final BlockChain blockChain;

    public RouteTable(byte[] id, BlockChain blockChain) throws UnknownHostException {
        this.blockChain = blockChain;
        this.id = id;
        kBuckets = new Queue[id.length * 8 - 1];
        for (int i = 0; i < id.length * 8 - 1; i++) {
            kBuckets[i] = new ArrayBlockingQueue<>(Math.min(K, 1 << Math.min(i, 30)));
        }
    }

    /*

    public void updateKbuckets(){

        byte[] randomKey = createSHA1Hash(Arrays.toString(previousRandomKey) + System.currentTimeMillis() + Math.random());

        previousRandomKey = randomKey;

        try {
            log.info("Finding random node");
            int index = BitSet.valueOf(distance(randomKey, id)).nextSetBit(0) - 1;
            List<KNode> nodes = new ArrayList<>();
            if (index > -1) {
                synchronized (kBuckets[index]){
                    while (kBuckets[index].isEmpty()){
                        index--;
                    }

                    if (index > 0) {
                        nodes = kBuckets[index].stream().map(Pair::getLeft).sorted( (a, b) -> {
                            byte[] aDistance = distance(a.getId(), randomKey);
                            byte[] bDistance = distance(b.getId(), randomKey);

                            for (int i = 0; i < aDistance.length; i++) {
                                if (aDistance[i] != bDistance[i]) {
                                    return aDistance[i] - bDistance[i];
                                }
                            }
                            return 0;
                        }).collect(Collectors.toList());
                    }
                }
            }

            ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(nodes.get(0).getIp()).getHostAddress(), nodes.get(0).getPort()).build();
            ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

            while (!nodes.isEmpty()) {

                log.info(String.format("Finding node with random ID %s in the node %s", Arrays.toString(randomKey), Arrays.toString(nodes.get(0).getId())));
                var response = stub.findKey(Key.parseFrom(randomKey));

                if (response.getFound()){
                    log.info("Found node with random ID:" + Arrays.toString(randomKey));
                    break;
                }

                if (response.getKBucket().getNodesCount() == 0){
                    log.info("No nodes in the route table");
                    nodes.remove(0);
                    continue;
                }

                add(response.getKBucket().getNodesList());

                channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(response.getKBucket().getNodes(0).getIp().toByteArray()).getHostAddress(), response.getKBucket().getNodes(0).getPort()).build();
                stub = ServicesGrpc.newBlockingStub(channel);
            }
        }   catch (UnknownHostException | InvalidProtocolBufferException e) {
            throw new RuntimeException(e);
        }

        long t = (long) poissonProcess.timeForNextEvent();
        log.info("Scheduling next update in " + t + " seconds");
        executorService.schedule(this::updateKbuckets, t, TimeUnit.SECONDS);
    }
     */



    public void start() {
        logger.info("started with id: " + KeysManager.hexString(id) + " " + NetUtils.IPtoString(myIp.getAddress()) + ":" + myPort);
        //executorService.schedule(this::updateKbuckets, (long) poissonProcess.timeForNextEvent() * 1000, TimeUnit.SECONDS);

        if (checkEquality(myIp.getAddress(), genesisIP)) return;

        new Thread(() -> {
            // try to fill k-buckets with nodes from the network
            try {
                Channel channel = NettyChannelBuilder.forAddress(InetAddress.getByAddress(genesisIP).getHostAddress(), genesisPort).usePlaintext().build();
                ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);
                logger.info("Finding nodes in the network from: " + IPtoString(genesisIP));

                KBucket response = stub.findNode(
                        KeyWithSender
                                .newBuilder()
                                .setKey(ByteString.copyFrom(id))
                                .setSender(ByteString.copyFrom(id))
                                .setPort(myPort)
                                .build()
                );
                logger.info(String.format("Found %d nodes in the network", response.getNodesCount()));
                List<Node> nodes = response.getNodesList();

                add(nodes);
                logger.info("received nodes: " + nodes.stream().map(KNode::fromNode));

                for (Node node : nodes) {
                    if (Arrays.equals(node.getId().toByteArray(), id) || (Arrays.equals(node.getIp().toByteArray(), genesisIP) && node.getPort() == genesisPort))
                        continue;

                    logger.info(String.format(
                            "Finding nodes in the network from node %s %s",
                            NetUtils.IPtoString(InetAddress.getByAddress(node.getIp().toByteArray()).getAddress()),
                            KeysManager.hexString(node.getId().toByteArray())
                    ));

                    channel = ManagedChannelBuilder.forAddress(
                            InetAddress.getByAddress(node.getIp().toByteArray()).getHostAddress(),
                            node.getPort()
                    ).usePlaintext().build();
                    stub = ServicesGrpc.newBlockingStub(channel);

                    var newNodes = stub.findNode(
                            KeyWithSender
                                    .newBuilder()
                                    .setKey(ByteString.copyFrom(id))
                                    .setSender(ByteString.copyFrom(id))
                                    .setPort(myPort)
                                    .build()
                    );

                    logger.info("response from" + KeysManager.hexString(newNodes.getSender().toByteArray()) + " " + NetUtils.IPtoString(node.getIp().toByteArray()) + ":" + node.getPort());
                    logger.info(newNodes.getNodesList().stream().map(KNode::fromNode).collect(Collectors.toList()).toString());

                    add(newNodes.getNodesList());
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }).start();
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
            Node thisNode = Node.newBuilder().setId(ByteString.copyFrom(id)).setIp(ByteString.copyFrom(Inet4Address.getLocalHost().getAddress())).setPort(myPort).build();

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

    private void update(Queue<Pair<KNode,ScheduledFuture<?>>> kBucket, KNode node){
        synchronized (kBucket) {
            kBucket.stream().filter(pair -> Arrays.equals(pair.getLeft().getId(), node.getId())).forEach(pair -> pair.getRight().cancel(false));
            if (kBucket.removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()))) {
                var event = executorService.schedule(() -> refresh(node, 0), 1, TimeUnit.HOURS);
                if (kBucket.offer(Pair.of(node, event)))
                    event.cancel(false);
            }
        }
    }



    public Iterable<Node> findNode(byte[] id) {
        int index = kBuckets.length - BitSet.valueOf(distance(id, this.id)).nextSetBit(0) - 1;

        if (index == kBuckets.length)
            index = 0;

        List<KNode> nodes = new ArrayList<>(K);

        if (!kBuckets[index].isEmpty())
            nodes.addAll(kBuckets[index].stream().map(Pair::getLeft).limit(K).collect(Collectors.toList()));

        int range = 1;
        while (nodes.size() < K && (index - range >= 0 || (index + range < kBuckets.length))) {
            //log.info(String.format("low %d high %d",index - range,index + range));
            if (index - range >= 0)
                nodes.addAll(kBuckets[index - range].stream().map(Pair::getLeft).limit(K - nodes.size()).collect(Collectors.toList()));

            if ((index + range < kBuckets.length) && nodes.size() < K)
                nodes.addAll(kBuckets[index + range].stream().map(Pair::getLeft).limit(K - nodes.size()).collect(Collectors.toList()));

            range++;
        }

        if (nodes.size() < K) nodes.add(new KNode(this.id, myIp.getAddress(), myPort));

        logger.info(String.format("findNode %s response :\n%s  ", KeysManager.hexString(id), nodes));

        return nodes.stream().map(KNode::toNode).collect(Collectors.toList());
    }

    private static byte[] distance(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
    }

    public void add(KNode kNode) {
        int index = (id.length * 8 - 1) - BitSet.valueOf(distance(kNode.getId(), id)).nextSetBit(0) - 1;
        if (index != id.length * 8 - 1) {
            add(kNode, index);
        }
    }

    private void add(KNode kNode, int index) {
        if (kBuckets[index].stream().map(Pair::getLeft).anyMatch(k -> Arrays.equals(k.getId(), kNode.getId()))) {
            logger.info("updating node" + kNode);
            update(kBuckets[index], kNode);
        } else {
            logger.info(String.format("added node %s to the route table to Kbucket %s", NetUtils.IPtoString(kNode.getIp()), index));
            ScheduledFuture<?> future = executorService.schedule(() -> refresh(kNode, index), 1, TimeUnit.HOURS);
            if (!kBuckets[index].offer(Pair.of(kNode, future))) {
                future.cancel(false);
            }
        }
    }

    private void refresh(KNode node, int index) {
        try {
            logger.info(String.format("Refreshing node %s %s", NetUtils.IPtoString(node.getIp()), KeysManager.hexString(node.getId())));
            ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(node.getIp()).getHostAddress(), node.getPort()).build();
            ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

            try {
                stub.ping(Sender.newBuilder().setKey(ByteString.copyFrom(id)).build());
                logger.info(String.format("Node %s %s responded to ping", NetUtils.IPtoString(node.getIp()), KeysManager.hexString(node.getId())));
            } catch (StatusRuntimeException e) {
                logger.info(String.format("Node %s %s did not respond to ping", NetUtils.IPtoString(node.getIp()), KeysManager.hexString(node.getId())));
                // doesn't need to cancel the future
                kBuckets[index].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId())); //TODO: maybe try to update the kBucket
                return;
            }

            update(kBuckets[index], node);

        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void propagate(kBlock data) {
        if (!blockChain.addBlock(Block.fromGrpc(data))) {
            logger.info(String.format("Block with hash %s is invalid", KeysManager.hexString(data.getHash().toByteArray())));
            return;
        }

        checkedPropagate(data);
    }

    public void checkedPropagate(kBlock data) {
        int senderDistance = (id.length * 8 - 1) - BitSet.valueOf(distance(data.getSender().toByteArray(), id)).nextSetBit(0);
        logger.info(String.format("Storing block with hash %s to nodes with distance less than %s", KeysManager.hexString(data.getHash().toByteArray()), senderDistance));

        IntStream.range(0, senderDistance).parallel().forEach(i -> {
            while (true) {
                var temp = kBuckets[i].peek();
                if (temp == null)
                    break;

                KNode node = temp.getLeft();

                try {
                    logger.info(
                            String.format(
                                    "Propagating Block with hash %s to node %s %s",
                                    KeysManager.hexString(data.getHash().toByteArray()),
                                    NetUtils.IPtoString(node.getIp()),
                                    KeysManager.hexString(node.getId())
                            )
                    );
                    ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(node.getIp()).getHostAddress(), node.getPort()).build();
                    ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

                    kBlock block = kBlock.newBuilder()
                            .setHash(data.getHash())
                            .setSender(data.getSender())
                            .setPort(myPort)
                            .setPrevHash(data.getPrevHash())
                            .setNonce(data.getNonce())
                            .setTimestamp(data.getTimestamp())
                            .build();

                    stub.storeBlock(block);

                    update(kBuckets[i], node);

                    break;
                } catch (UnknownHostException e) {
                    throw new RuntimeException(e);
                } catch (StatusRuntimeException e) {
                    logger.info(String.format("Node %s %s did not respond to store error %s",
                            NetUtils.IPtoString(node.getIp()), KeysManager.hexString(node.getId()), e.getStatus().getCode()));
                    kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
                }
            }
        });
    }

    public void propagate(kTransaction data) {
        if (!blockChain.addTransaction(Transaction.fromGrpc(data))) {
            logger.info(String.format("Transaction with signature %s is invalid", data));
            return;
        }

        checkedPropagate(data);
    }

    public void checkedPropagate(kTransaction data) {
        int senderDistance = (id.length * 8 - 1) - BitSet.valueOf(distance(data.getSender().toByteArray(), id)).nextSetBit(0);
        logger.info(String.format("Sending transaction with signature %s to nodes with distance less than %s ", KeysManager.hexString(Transaction.fromGrpc(data).hash()), senderDistance));

        IntStream.range(0, senderDistance).parallel().forEach(i -> {
            while (!kBuckets[i].isEmpty()) {
                var temp = kBuckets[i].peek();
                if (temp == null)
                    break;

                KNode node = temp.getLeft();

                try {
                    logger.info(
                            String.format(
                                    "Propagating Transaction with signature %s to node %s %s",
                                    KeysManager.hexString(Transaction.fromGrpc(data).hash()),
                                    NetUtils.IPtoString(node.getIp()),
                                    KeysManager.hexString(node.getId())
                            )
                    );

                    ManagedChannel channel = ManagedChannelBuilder.forAddress(IPtoString(node.getIp()), node.getPort()).build();
                    ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

                    stub.storeTransaction(data.newBuilderForType().setSender(ByteString.copyFrom(id)).setPort(myPort).build());

                    logger.info(
                            String.format(
                                    "Sent Transaction with signature %s to node %s %s",
                                    KeysManager.hexString(Transaction.fromGrpc(data).hash()),
                                    NetUtils.IPtoString(node.getIp()),
                                    KeysManager.hexString(node.getId())
                            )
                    );

                    update(kBuckets[i], node);

                    Thread.sleep(1000);

                    logger.info("Checking that the block was effectively");

                    if (checkPropagate(i, node.getId(), data)) {
                        break;
                    }

                    kBuckets[i].removeIf((t) -> Arrays.equals(t.getLeft().getId(), node.getId()));
                } catch (StatusRuntimeException e) {
                    logger.info(String.format("Node %s %s did not respond to store error %s",
                            NetUtils.IPtoString(node.getIp()), KeysManager.hexString(node.getId()), e.getStatus().getCode()));
                    kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }

    public boolean checkPropagate(int i, byte[] nodeID, kTransaction transaction) {
        var checkNode = kBuckets[i].stream().filter((t) -> !Arrays.equals(t.getLeft().getId(), nodeID)).findFirst();

        if (checkNode.isEmpty())
            return true;

        ManagedChannel channel = ManagedChannelBuilder.forAddress(IPtoString(checkNode.get().getKey().getIp()), checkNode.get().getKey().getPort()).build();
        ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);

        kTransaction checkTransaction = stub.findTransaction(KeyWithSender.newBuilder().setSender(ByteString.copyFrom(id)).setKey(transaction.getBid().getHash()).build());

        //TODO: check if the node has propagated the block to a random node in the range

        return false;
    }

    public void shutdown() {
        executorService.shutdown();
    }

    public static boolean checkEquality(byte[] ip1, byte[] ip2) {
        for (int i = 0; i < ip1.length; i++) {
            if ((ip1[i] ^ ip2[i]) != 0)
                return false;
        }
        return true;
    }

    public void add(List<Node> nodes) {
        nodes.stream().map(KNode::fromNode).forEach(this::add);
    }

    public InetAddress getIP() {
        return myIp;
    }

    public int getPort() {
        return myPort;
    }

    public byte[] getId() {
        return id;
    }
}
