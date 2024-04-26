package org.example.Kamdelia;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerInterceptors;
import kademlia_public_ledger.kBlock;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.example.Blockchain.Block;
import org.example.Client.Transaction;
import org.example.CryptoUtils.KeysManager;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.logging.Logger;

public class Kademlia {
    private final RouteTable routeTable;
    private final Server server;
    private final Logger logger = Logger.getLogger(Kademlia.class.getName());
    private final static ExecutorService executor = Executors.newScheduledThreadPool(1);
    public final static byte[] genesisIP = new byte[]{(byte) 172, 17, 0, 2};
    public final static int genesisPort = 5000;

    public Kademlia(int port) throws IOException {
        if (port < 0 || port > 65535) {
            throw new IllegalArgumentException("Port must be between 0 and 65535");
        }
        // SHA1 id
        // 160 bit id   (20 bytes)
        byte[] id = createSHA1Hash(Arrays.toString(Inet4Address.getLocalHost().getAddress()) + System.currentTimeMillis() + Math.random());
        assert id.length == 20;

        routeTable = new RouteTable(id,logger);
        server = ServerBuilder
                .forPort(port)
                .addService(ServerInterceptors.intercept(new KademliaAPI(routeTable, logger), new Interceptor()))
                .build();
    }

    static byte[] createSHA1Hash(String s) {
        Digest digest = new SHA1Digest();
        byte[] data = s.getBytes();
        byte[] hash = new byte[digest.getDigestSize()];

        assert hash.length == 20;

        digest.update(data, 0, data.length);
        digest.doFinal(hash, 0);

        return hash;
    }

    public void start() {
        try {
            server.start();

            logger.info("Server started, listening on " + server.getPort());
            System.out.println("Server started, listening on " + server.getPort());

            routeTable.start();

        } catch (IOException e) {
            logger.severe("Server failed to start: " + e.getMessage());
        }
    }

    // this should asynchronously propagate the block
    public void propagate(Block data) {
        executor.submit(() -> {
            logger.info("Propagating block: " + KeysManager.hexString(data.getHash()));
            routeTable.propagate(data.toGrpc());
        });
    }

    // this should asynchronously propagate the transaction
    public void propagate(Transaction data) {
        executor.submit(() -> {
            logger.info("Propagating transaction: " + data);
            routeTable.propagate(data.toGrpc());
        });
    }

    public void setBlockStorageFunction(Function<kBlock, Boolean> blockStorageFunction) {
        // the function should return true if the block is valid and stored, false otherwise
        routeTable.setBlockStorageFunction(blockStorageFunction);
    }

    public void setTransactionStorageFunction(Function<kademlia_public_ledger.kTransaction, Boolean> transactionStorageFunction) {
        // the function should return true if the transaction is valid and stored, false otherwise
        routeTable.setTransactionStorageFunction(transactionStorageFunction);
    }

    public void setBlockStorageGetter(Function<String, Optional<Block>> blockStorageFunction) {
        // the function should return true if the block is valid and stored, false otherwise
        routeTable.getBlockStorageFunction(blockStorageFunction);
    }

    public void setTransactionStorageGetter(Function<String, Optional<Transaction>> transactionStorageFunction) {
        // the function should return true if the transaction is valid and stored, false otherwise
        routeTable.getTransactionStorageFunction(transactionStorageFunction);
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
