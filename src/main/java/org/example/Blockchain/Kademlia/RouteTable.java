package org.example.Blockchain.Kademlia;

import com.google.protobuf.ByteString;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import kademlia_public_ledger.Boolean;
import kademlia_public_ledger.*;
import org.apache.commons.lang3.tuple.Pair;
import org.example.Blockchain.Block;
import org.example.Blockchain.BlockChain;
import org.example.Client.Transaction;
import org.example.Utils.KeysManager;
import org.example.Utils.LogFilter;

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
    private final BlockChain blockChain;

    public RouteTable(byte[] id, BlockChain blockChain) throws UnknownHostException {
        this.blockChain = blockChain;
        this.id = id;
        KNode.myId = id;
        KNode.myPort = myPort;
        logger.setFilter(new LogFilter());
        kBuckets = new Queue[id.length * 8 - 1];
        for (int i = 0; i < id.length * 8 - 1; i++) {
            kBuckets[i] = new ArrayBlockingQueue<>(Math.min(K, 1 << Math.min(i, 30)));
        }
    }

    public void start() {
        logger.info("started with id: " + KeysManager.hexString(id) + " " + IPtoString(myIp.getAddress()) + ":" + myPort);
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


                    var newNodes = KNode.fromNode(node).toClient().findNode(
                            KeyWithSender.newBuilder()
                                    .setKey(ByteString.copyFrom(id))
                                    .setSender(ByteString.copyFrom(id))
                                    .setPort(myPort)
                                    .build()
                    );

                    if (newNodes.isPresent()) {
                        logger.info("response from" + KeysManager.hexString(newNodes.get().getSender().toByteArray()) + " " + IPtoString(node.getIp().toByteArray()) + ":" + node.getPort());
                        logger.info(newNodes.get().getNodesList().stream().map(KNode::fromNode).collect(Collectors.toList()).toString());

                        add(newNodes.get().getNodesList());
                    } else {
                        logger.info(String.format("node %s %s:%s did not respond", node.getId(), node.getIp(), node.getId()));
                    }
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
        int index = getBucketIndex(id);

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
        int index = getBucketIndex(kNode.getId());
        if (!Arrays.equals(kNode.getId(), id)) {
            add(kNode, index);
        }
    }

    private void add(KNode kNode, int index) {
        if (kBuckets[index].stream().map(Pair::getLeft).anyMatch(k -> Arrays.equals(k.getId(), kNode.getId()))) {
            logger.info("updating node" + kNode);
            update(kBuckets[index], kNode);
        } else {
            logger.info(String.format("added node %s to the route table to Kbucket %s", IPtoString(kNode.getIp()), index));
            ScheduledFuture<?> future = executorService.schedule(() -> refresh(kNode, index), 1, TimeUnit.HOURS);
            if (!kBuckets[index].offer(Pair.of(kNode, future))) {
                future.cancel(false);
            }
        }
    }

    private void refresh(KNode node, int index) {
        logger.info(String.format("Refreshing node %s %s", IPtoString(node.getIp()), KeysManager.hexString(node.getId())));

        var a = node.toClient().ping(Sender.newBuilder().setKey(ByteString.copyFrom(id)).build());

        if (a.isEmpty()) {
            logger.info(String.format("Node %s %s did not respond to ping", IPtoString(node.getIp()), KeysManager.hexString(node.getId())));
            kBuckets[index].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
            return;
        }

        update(kBuckets[index], node);

    }

    public void propagate(kBlock data) {
        if (!blockChain.addBlock(Block.fromGrpc(data))) {
            logger.info(String.format("Block with hash %s is invalid", KeysManager.hexString(data.getHash().toByteArray())));
            return;
        }

        checkedPropagate(data);
    }

    public void checkedPropagate(kBlock data) {
        int senderDistance = getBucketIndex(data.getSender().toByteArray());
        logger.info(String.format("Storing block with hash %s to nodes with distance less than %s", KeysManager.hexString(data.getHash().toByteArray()), senderDistance));
        IntStream.range(0, senderDistance).parallel().forEach(i -> propagate(data, i));
    }

    private void propagate(kBlock data, int i) {
        var temp = kBuckets[i].peek();
        if (temp != null) {
            KNode node = temp.getLeft();

            logger.info(
                    String.format(
                            "Propagating Block with hash %s to node %s %s",
                            KeysManager.hexString(data.getHash().toByteArray()),
                            IPtoString(node.getIp()),
                            KeysManager.hexString(node.getId())
                    )
            );

            var response = node.toClient().storeBlock(data.toBuilder().setSender(data.getSender()).setPort(myPort).build());

            if (response.isEmpty()) {
                logger.info(String.format("Node %s %s did not respond",
                        IPtoString(node.getIp()), KeysManager.hexString(node.getId())));
                kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));

            }

            update(kBuckets[i], node);

            executorService.schedule(() -> {
                if (kBuckets[i].size() > 1 && !checkPropagate(data, i, node.getId())) {
                    kBuckets[i].removeIf((t) -> Arrays.equals(t.getLeft().getId(), node.getId()));
                    propagate(data, i);
                }
            }, 1, TimeUnit.SECONDS);
        }
    }

    private boolean checkPropagate(kBlock data, int i, byte[] id) {
        while (kBuckets[i].size() > 2) {
            var checkNode = kBuckets[i].stream().filter((t) -> !Arrays.equals(t.getLeft().getId(), id)).findFirst();

            if (checkNode.isEmpty())
                return true;

            var a = checkNode.get().getLeft().toClient().hasBlock(
                    KeyWithSender.newBuilder()
                            .setSender(ByteString.copyFrom(id))
                            .setKey(data.getHash())
                            .setPort(myPort)
                            .build()
            );

            if (a.isPresent()) {
                return a.get().getValue();
            }

            kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), checkNode.get().getLeft().getId()));
        }

        return true;
    }

    public void propagate(kTransaction data) {
        if (!blockChain.addPropagatedTransaction(Transaction.fromGrpc(data))) {
            logger.info(String.format("Transaction with hash %s is invalid", data));
            return;
        }

        checkedPropagate(data);
    }

    public void checkedPropagate(kTransaction data) {
        int senderDistance = getBucketIndex(data.getSender().toByteArray());
        logger.info(String.format("Sending transaction with hash %s to nodes with distance less than %s ", KeysManager.hexString(Transaction.fromGrpc(data).hash()), senderDistance));

        IntStream.range(0, senderDistance).parallel().forEach(i -> propagate(data, i));
    }

    private void propagate(kTransaction data, int i) {
        var temp = kBuckets[i].peek();
        if (temp != null) {
            KNode node = temp.getLeft();

            logger.info(
                    String.format(
                            "Propagating Transaction with signature %s to node %s %s in the kbucket %s",
                            KeysManager.hexString(Transaction.fromGrpc(data).hash()),
                            IPtoString(node.getIp()),
                            KeysManager.hexString(node.getId()),
                            i
                    )
            );
            var response = node.toClient().storeTransaction(data.toBuilder().setSender(ByteString.copyFrom(id)).setPort(myPort).build());

            if (response.isEmpty()) {
                logger.info(String.format("Node %s %s did not respond to store",
                        IPtoString(node.getIp()), KeysManager.hexString(node.getId())));
                kBuckets[i].removeIf(pair -> Arrays.equals(pair.getLeft().getId(), node.getId()));
            } else {
                update(kBuckets[i], node);

                executorService.schedule(() -> {
                    if (kBuckets[i].size() > 1 && !checkPropagate(data, i, node.getId())) {
                        kBuckets[i].removeIf((t) -> Arrays.equals(t.getLeft().getId(), node.getId()));
                        propagate(data, i);
                    }
                }, 1, TimeUnit.SECONDS);
            }

        }
    }

    private boolean checkPropagate(kTransaction transaction, int i, byte[] nodeID) {
        var checkNode = kBuckets[i].stream().filter((t) -> !Arrays.equals(t.getLeft().getId(), nodeID)).findFirst();

        if (checkNode.isEmpty())
            return true;

        ByteString key = transaction.hasAuction() ? transaction.getAuction().getKey() : transaction.getBid().getHash();

        var response = checkNode.get().getKey().toClient().hasTransaction(
                TransactionKey.newBuilder()
                        .setSender(ByteString.copyFrom(id))
                        .setKey(key)
                        .setType(transaction.hasAuction() ? Type.auction : Type.bid)
                        .setPort(myPort)
                        .build()
        );

        return response.map(Boolean::getValue).orElse(false);
    }

    public TransactionOrBucket getValues(TransactionKey transactionKey) {
        int kBucketIndex = getBucketIndex(transactionKey.getKey().toByteArray());

        while (kBucketIndex > 0) {
            if (!kBuckets[kBucketIndex].isEmpty()) {
                List<Node> nodes = kBuckets[kBucketIndex].stream()
                        .map((t) -> t.getLeft().toNode())
                        .collect(Collectors.toList());

                KBucket kBucket = KBucket.newBuilder().addAllNodes(nodes).build();

                return TransactionOrBucket.newBuilder().setIsNone(true).setBucket(kBucket).build();
            }
            kBucketIndex--;
        }
        var a = Transaction.load(transactionKey);
        if (a.isEmpty())
            return TransactionOrBucket.newBuilder().setIsNone(true).build();

        return TransactionOrBucket.newBuilder().setTransaction(a.get().toGrpc()).build();
    }

    public BlockOrKBucket getValues(KeyWithSender blockKey) {
        byte[] key = blockKey.getKey().toByteArray();
        int kBucketIndex = getBucketIndex(key);

        while (kBucketIndex >= 0) {
            if (!kBuckets[kBucketIndex].isEmpty()) {
                List<Node> nodes = kBuckets[kBucketIndex].stream()
                        .map((t) -> t.getLeft().toNode())
                        .collect(Collectors.toList());

                KBucket kBucket = KBucket.newBuilder().addAllNodes(nodes).build();

                return BlockOrKBucket.newBuilder().setIsNone(true).setBucket(kBucket).build();
            }
            kBucketIndex--;
        }

        Optional<Block> block = Block.load(key);

        return block.map(value -> BlockOrKBucket.newBuilder().setBlock(value.toGrpc(id))).orElseGet(() -> BlockOrKBucket.newBuilder().setIsNone(true)).build();
    }

    public Optional<Block> getBlock(byte[] key) {
        int index = getBucketIndex(key);

        var a = kBuckets[index].peek();
        if (a != null) return getBlock(key, a.getLeft());

        for (int range = 1; index - range >= 0 || index + range < kBuckets.length; range++) {
            if (index - range >= 0) {
                a = kBuckets[index].peek();
                if (a != null) return getBlock(key, a.getLeft());
            }

            if ((index + range < kBuckets.length)) {
                a = kBuckets[index].peek();
                if (a != null) return getBlock(key, a.getLeft());
            }
        }
        logger.info("No nodes found in the routeTable");

        return Optional.empty();
    }

    private Optional<Block> getBlock(byte[] key, KNode node) {
        byte[] minDistance = distance(key, node.getId());

        var blockOrKBucket = node.toClient().findBlock(
                KeyWithSender.newBuilder()
                        .setKey(ByteString.copyFrom(key))
                        .setSender(ByteString.copyFrom(id))
                        .setPort(5000)
                        .build()
        );

        while (blockOrKBucket.isPresent() && !blockOrKBucket.get().hasBlock()) {
            KBucket bucket = blockOrKBucket.get().getBucket();
            add(bucket.getNodesList());

            Optional<Node> a = bucket.getNodesList().stream().min((t1, t2) -> compareDistance(t1.toByteArray(), t2.toByteArray()));

            if (a.isEmpty() || compareDistance(minDistance, a.get().toByteArray()) > 0) {
                return Optional.empty();
            }

            blockOrKBucket = node.toClient().findBlock(KeyWithSender.newBuilder()
                    .setKey(ByteString.copyFrom(key))
                    .setSender(ByteString.copyFrom(id))
                    .setPort(5000)
                    .build());
        }

        return blockOrKBucket.map(orKBucket -> Block.fromGrpc(orKBucket.getBlock()));

    }

    public Optional<Transaction> getTransaction(byte[] key, Type type) {
        int index = getBucketIndex(key);

        var a = kBuckets[index].peek();
        if (a != null) return getTransaction(key, a.getLeft(), type);

        for (int range = 1; index - range >= 0 || index + range < kBuckets.length; range++) {
            if (index - range >= 0) {
                a = kBuckets[index].peek();
                if (a != null) return getTransaction(key, a.getLeft(), type);
            }

            if ((index + range < kBuckets.length)) {
                a = kBuckets[index].peek();
                if (a != null) return getTransaction(key, a.getLeft(), type);
            }
        }
        logger.info("No nodes found in the routeTable");

        return Optional.empty();
    }

    private Optional<Transaction> getTransaction(byte[] key, KNode node, Type type) {
        byte[] minDistance = distance(key, node.getId());

        Optional<TransactionOrBucket> response = node.toClient().findTransaction(key, type);

        if (response.isEmpty()) return Optional.empty();
        TransactionOrBucket message = response.get();

        while (!message.hasTransaction()) {
            if (message.getIsNone())
                return Optional.empty();

            KBucket bucket = message.getBucket();
            add(bucket.getNodesList());

            Optional<Node> a = bucket.getNodesList().stream().min((t1, t2) -> compareDistance(t1.toByteArray(), t2.toByteArray()));

            if (a.isEmpty() || compareDistance(minDistance, a.get().toByteArray()) > 0)
                return Optional.empty();

            node.toClient().findTransaction(key, type);
        }

        return Optional.of(Transaction.fromGrpc(message.getTransaction()));
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

    private int compareDistance(byte[] a, byte[] b) {
        for (int i = 0; i < a.length; i++) {
            int cmp = Byte.compare(a[i], b[i]);
            if (cmp != 0) {
                return cmp;
            }
        }
        return 0;
    }

    private int getBucketIndex(byte[] key) {
        return kBuckets.length - Math.max(0, BitSet.valueOf(distance(key, this.id)).nextSetBit(0)) - 1;
    }

    private boolean checkEquality(byte[] ip1, byte[] ip2) {
        for (int i = 0; i < ip1.length; i++) {
            if ((ip1[i] ^ ip2[i]) != 0)
                return false;
        }
        return true;
    }

    private byte[] distance(byte[] a, byte[] b) {
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
