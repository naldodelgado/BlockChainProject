package org.example.Blockchain.Kademlia;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import kademlia_public_ledger.*;
import org.apache.commons.lang3.tuple.Pair;
import org.example.Blockchain.Block;
import org.example.Blockchain.BlockChain;
import org.example.Client.Transaction;
import org.example.Utils.KeysManager;
import org.example.Utils.LogFilter;
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
        logger.setFilter(new LogFilter());
        kBuckets = new Queue[id.length * 8 - 1];
        for (int i = 0; i < id.length * 8 - 1; i++) {
            kBuckets[i] = new ArrayBlockingQueue<>(Math.min(K, 1 << Math.min(i, 30)));
        }
    }

    public void start() {
        logger.info("started with id: " + KeysManager.hexString(id) + " " + NetUtils.IPtoString(myIp.getAddress()) + ":" + myPort);
        //executorService.schedule(this::updateKbuckets, (long) poissonProcess.timeForNextEvent() * 1000, TimeUnit.SECONDS);

        if (checkEquality(myIp.getAddress(), genesisIP)) return;

        new Thread(() -> {
            // try to fill k-buckets with nodes from the network
            try {
                logger.info("Finding nodes in the network from: " + IPtoString(genesisIP));
                ManagedChannel channel = ManagedChannelBuilder.forAddress(InetAddress.getByAddress(genesisIP).getHostAddress(), genesisPort).usePlaintext().build();
                KBucket response = ServicesGrpc.newBlockingStub(channel).findNode(
                        KeyWithSender
                                .newBuilder()
                                .setKey(ByteString.copyFrom(id))
                                .setSender(ByteString.copyFrom(id))
                                .setPort(myPort)
                                .build()
                );
                channel.shutdown();

                logger.info(String.format("Found %d nodes in the network", response.getNodesCount()));
                List<Node> nodes = response.getNodesList();

                add(nodes);
                logger.info("received nodes: " + nodes.stream().map(KNode::fromNode));

                for (Node node : nodes) {
                    if (Arrays.equals(node.getId().toByteArray(), id) || (Arrays.equals(node.getIp().toByteArray(), genesisIP) && node.getPort() == genesisPort))
                        continue;

                    logger.info(String.format(
                            "Finding nodes in the network from node %s %s",
                            IPtoString(InetAddress.getByAddress(node.getIp().toByteArray()).getAddress()),
                            KeysManager.hexString(node.getId().toByteArray())
                    ));

                    channel = ManagedChannelBuilder.forAddress(
                            InetAddress.getByAddress(node.getIp().toByteArray()).getHostAddress(),
                            node.getPort()
                    ).usePlaintext().build();
                    var newNodes = ServicesGrpc.newBlockingStub(channel).findNode(
                            KeyWithSender.newBuilder()
                                    .setKey(ByteString.copyFrom(id))
                                    .setSender(ByteString.copyFrom(id))
                                    .setPort(myPort)
                                    .build()
                    );
                    channel.shutdown();

                    logger.info("response from" + KeysManager.hexString(newNodes.getSender().toByteArray()) + " " + IPtoString(node.getIp().toByteArray()) + ":" + node.getPort());
                    logger.info(newNodes.getNodesList().stream().map(KNode::fromNode).collect(Collectors.toList()).toString());

                    add(newNodes.getNodesList());
                }
            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            }
        }).start();
    }

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
        int index = kBuckets.length - Math.max(0, BitSet.valueOf(distance(id, this.id)).nextSetBit(0)) - 1;

        if (index == kBuckets.length)
            index = 0;

        List<KNode> nodes = new ArrayList<>(K);

        if (!kBuckets[index].isEmpty())
            nodes.addAll(kBuckets[index].stream().map(Pair::getLeft).limit(K).collect(Collectors.toList()));

        int range = 1;
        while (nodes.size() < K && (index - range >= 0 || (index + range < kBuckets.length))) {
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


    public void add(List<Node> nodes) {
        nodes.stream().map(KNode::fromNode).forEach(this::add);
    }

    public void add(KNode kNode) {
        int index = (id.length * 8 - 1) - Math.max(0, BitSet.valueOf(distance(kNode.getId(), id)).nextSetBit(0)) - 1;
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
            ManagedChannel channel = ManagedChannelBuilder.forAddress(
                    InetAddress.getByAddress(node.getIp())
                            .getHostAddress(), node.getPort()
            ).build();
            ServicesGrpc.ServicesBlockingStub stub = ServicesGrpc.newBlockingStub(channel);
            channel.shutdown();

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
        if (!blockChain.addBlock(Block.fromGrpc(data), 1)) {
            logger.info(String.format("Block with hash %s is invalid", KeysManager.hexString(data.getHash().toByteArray())));
            return;
        }

        checkedPropagate(data);
    }

    public void checkedPropagate(kBlock data) {
        int senderDistance = (id.length * 8 - 2) - Math.max(0, BitSet.valueOf(distance(data.getSender().toByteArray(), id)).nextSetBit(0));
        logger.info(String.format("Storing block with hash %s to nodes with distance less than %s", KeysManager.hexString(data.getHash().toByteArray()), senderDistance));

        IntStream.range(0, senderDistance).parallel().forEach(i -> propagate(data, i));
    }

    private void propagate(kBlock data, int i) {
        var temp = kBuckets[i].peek();
        if (temp != null) {

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
                stub.storeBlock(data.toBuilder().setSender(data.getSender()).setPort(myPort).build());
                channel.shutdown();

                update(kBuckets[i], node);

                executorService.schedule(() -> {
                    if (kBuckets[i].size() > 1 && !checkPropagate(data, i, node.getId())) {
                        kBuckets[i].removeIf((t) -> Arrays.equals(t.getLeft().getId(), node.getId()));
                        propagate(data, i);
                    }
                }, 1, TimeUnit.SECONDS);

            } catch (UnknownHostException e) {
                throw new RuntimeException(e);
            } catch (StatusRuntimeException e) {
                logger.info(String.format("Node %s %s did not respond to store error %s",
                        NetUtils.IPtoString(node.getIp()), KeysManager.hexString(node.getId()), e.getStatus().getCode()));
                kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
            }
        }
    }

    private boolean checkPropagate(kBlock data, int i, byte[] id) {
        var checkNode = kBuckets[i].stream().filter((t) -> !Arrays.equals(t.getLeft().getId(), id)).findFirst();

        if (checkNode.isEmpty())
            return true;

        try {
            ManagedChannel channel = ManagedChannelBuilder.forAddress(IPtoString(checkNode.get().getKey().getIp()), checkNode.get().getKey().getPort()).usePlaintext().build();
            boolean checkTransaction = ServicesGrpc
                    .newBlockingStub(channel)
                    .hasBlock(
                            KeyWithSender.newBuilder()
                                    .setSender(ByteString.copyFrom(id))
                                    .setKey(data.getHash())
                                    .setPort(myPort)
                                    .build()
                    ).getValue();
            channel.shutdown();

            return checkTransaction;
        } catch (StatusRuntimeException e) {
            return false;
        }
    }

    public void propagate(kTransaction data) {
        if (!blockChain.addPropagatedTransaction(Transaction.fromGrpc(data))) {
            logger.info(String.format("Transaction with hash %s is invalid", data));
            return;
        }

        checkedPropagate(data);
    }

    public void checkedPropagate(kTransaction data) {
        int senderDistance = (id.length * 8 - 2) - Math.max(0, BitSet.valueOf(distance(data.getSender().toByteArray(), id)).nextSetBit(0));
        logger.info(String.format("Sending transaction with hash %s to nodes with distance less than %s ", KeysManager.hexString(Transaction.fromGrpc(data).hash()), senderDistance));

        IntStream.range(0, senderDistance).parallel().forEach(i -> propagate(data, i));
    }

    private void propagate(kTransaction data, int i) {
        var temp = kBuckets[i].peek();
        if (temp != null) {
            KNode node = temp.getLeft();

            try {
                logger.info(
                        String.format(
                                "Propagating Transaction with signature %s to node %s %s in the kbucket %s",
                                KeysManager.hexString(Transaction.fromGrpc(data).hash()),
                                NetUtils.IPtoString(node.getIp()),
                                KeysManager.hexString(node.getId()),
                                i
                        )
                );

                ManagedChannel channel = ManagedChannelBuilder
                        .forAddress(IPtoString(node.getIp()), node.getPort())
                        .usePlaintext()
                        .build();

                ServicesGrpc.newBlockingStub(channel)
                        .storeTransaction(data.toBuilder().setSender(ByteString.copyFrom(id)).setPort(myPort).build());
                channel.shutdown();

                update(kBuckets[i], node);

                executorService.schedule(() -> {
                    if (kBuckets[i].size() > 1 && !checkPropagate(data, i, node.getId())) {
                        kBuckets[i].removeIf((t) -> Arrays.equals(t.getLeft().getId(), node.getId()));
                        propagate(data, i);
                    }
                }, 1, TimeUnit.SECONDS);
            } catch (StatusRuntimeException e) {
                logger.info(String.format("Node %s %s did not respond to store error %s",
                        NetUtils.IPtoString(node.getIp()), KeysManager.hexString(node.getId()), e.getStatus().getCode()));
                kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
            }
        }
    }

    private boolean checkPropagate(kTransaction transaction, int i, byte[] nodeID) {
        var checkNode = kBuckets[i].stream().filter((t) -> !Arrays.equals(t.getLeft().getId(), nodeID)).findFirst();

        if (checkNode.isEmpty())
            return true;


        try {
            ByteString key = transaction.hasAuction() ? transaction.getAuction().getKey() : transaction.getBid().getHash();

            ManagedChannel channel = ManagedChannelBuilder.forAddress(IPtoString(checkNode.get().getKey().getIp()), checkNode.get().getKey().getPort()).usePlaintext().build();
            boolean checkTransaction = ServicesGrpc
                    .newBlockingStub(channel)
                    .hasTransaction(
                            TransactionKey.newBuilder()
                                    .setSender(ByteString.copyFrom(id))
                                    .setKey(key)
                                    .setType(transaction.hasAuction() ? Type.auction : Type.bid)
                                    .setPort(myPort)
                                    .build()
                    ).getValue();
            channel.shutdown();

            return checkTransaction;
        } catch (StatusRuntimeException e) {
            return false;
        }
    }

    public TransactionOrBucket getValues(TransactionKey transactionKey) {
        byte[] key = transactionKey.getKey().toByteArray();
        int kBucketIndex = (id.length * 8 - 1) - Math.max(0, BitSet.valueOf(distance(key, id)).nextSetBit(0)) - 1;

        while (kBucketIndex > 0) {
            if (!kBuckets[kBucketIndex].isEmpty()) {
                List<Node> nodes = kBuckets[kBucketIndex].stream()
                        .map((t) -> t.getLeft().toNode())
                        .collect(Collectors.toList());

                KBucket kBucket = KBucket.newBuilder().addAllNodes(nodes).build();

                return TransactionOrBucket.newBuilder().setBucket(kBucket).build();
            }
            kBucketIndex--;
        }
        var a = Transaction.load(transactionKey);
        if (a.isEmpty())
            return TransactionOrBucket.newBuilder().build();

        return TransactionOrBucket.newBuilder().setTransaction(a.get().toGrpc()).build();
    }

    public BlockOrKBucket getValues(KeyWithSender blockKey) {
        byte[] key = blockKey.getKey().toByteArray();
        int kBucketIndex = (id.length * 8 - 1) - Math.max(0, BitSet.valueOf(distance(key, id)).nextSetBit(0)) - 1;

        while (kBucketIndex > 0) {
            if (!kBuckets[kBucketIndex].isEmpty()) {
                List<Node> nodes = kBuckets[kBucketIndex].stream()
                        .map((t) -> t.getLeft().toNode())
                        .collect(Collectors.toList());

                KBucket kBucket = KBucket.newBuilder().addAllNodes(nodes).build();

                return BlockOrKBucket.newBuilder().setBucket(kBucket).build();
            }
            kBucketIndex--;
        }

        Optional<Block> block = Block.load(key);

        return block.map(value -> BlockOrKBucket.newBuilder().setBlock(value.toGrpc(id))).orElseGet(BlockOrKBucket::newBuilder).build();

    }

    public boolean hasTransaction(TransactionKey key) {
        return Transaction.load(key).isPresent();
    }

    public boolean hasBlock(KeyWithSender key) {
        return Block.load(key.getKey().toByteArray()).isPresent();
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

    private static byte[] distance(byte[] a, byte[] b) {
        byte[] result = new byte[a.length];
        for (int i = 0; i < a.length; i++) {
            result[i] = (byte) (a[i] ^ b[i]);
        }
        return result;
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
