package org.example.Blockchain.Kamdelia;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import org.example.Blockchain.Block;
import org.example.Blockchain.BlockChain;
import org.example.Client.Transaction;
import org.example.Utils.KeysManager;
import org.example.Utils.LogFilter;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

public class Kademlia {
    private final RouteTable routeTable;
    private final Server server;
    private final Logger logger = Logger.getLogger(Kademlia.class.getName());
    private final static ExecutorService executor = Executors.newScheduledThreadPool(1);
    public final static byte[] genesisIP = new byte[]{(byte) 172, 17, 0, 2};
    public final static int genesisPort = 5000;
    public final BlockChain blockChain;

    public Kademlia(int port, BlockChain blockChain) {
        this.blockChain = blockChain;
        logger.setFilter(new LogFilter());
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }

        try {
            byte[] id = KeysManager.createSHA1Hash(Arrays.toString(Inet4Address.getLocalHost().getAddress()) + System.currentTimeMillis() + Math.random());
            assert id.length == 20;

            routeTable = new RouteTable(id, blockChain);
            server = ServerBuilder
                    .forPort(port)
                    .addService(ServerInterceptors.intercept(new KademliaAPI(routeTable), new IPInterceptor()))
                    .build();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {
            server.start();
            logger.info("Server started, listening on " + server.getPort());
            routeTable.start();
        } catch (IOException e) {
            logger.severe("Server failed to start: " + e.getMessage());
        }
    }

    // this should asynchronously propagate the block
    public void propagate(Block data) {
        executor.submit(() -> routeTable.checkedPropagate(data.toGrpc(routeTable.getId())));
    }

    // this should asynchronously propagate the transaction
    public void propagate(Transaction data) {
        executor.submit(() -> routeTable.checkedPropagate(data.toGrpc(routeTable.getId())));
    }

    public Block getBlock(byte[] hash, long index) {
        //TODO
        throw new UnsupportedOperationException();
    }

    public Transaction getTransaction(byte[] hash) {
        //TODO
        throw new UnsupportedOperationException();
    }

    public void stop() {
        server.shutdown();
        routeTable.shutdown();
    }

}
