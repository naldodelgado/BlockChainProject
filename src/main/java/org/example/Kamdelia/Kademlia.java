package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import kademlia_public_ledger.kBlock;
import kademlia_public_ledger.kTransaction;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.example.Blockchain.Block;
import org.example.Blockchain.Transaction;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.Arrays;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class Kademlia {
    private final RouteTable routeTable;
    private final Server server;
    private final Logger logger = Logger.getLogger(Kademlia.class.getName());
    public final static byte[] genesisIP = new byte[]{127, 0, 0, 1};
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
        server = ServerBuilder.forPort(port).addService(new KademliaAPI(routeTable,logger)).build();
    }

    private byte[] createSHA1Hash(String s) {
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

    public void propagate(Block data) {
        kBlock block = kBlock.newBuilder()
                .setHash(ByteString.copyFrom(data.getHash()))
                .setPrevHash(data.getPreviousHash() == null ? ByteString.EMPTY : ByteString.copyFrom(data.getPreviousHash()))
                .setTimestamp(data.getTimestamp())
                .setNonce(data.getNonce())
                .addAllTransactions(
                    data.getTransactions().stream().map(t ->
                        kTransaction.newBuilder()
                            .setSender(ByteString.copyFrom(t.getRecipientAddress()))
                            .setReceiver(ByteString.copyFrom(t.getRecipientAddress()))
                            .setAmount(t.getAmount())
                            .setTimestamp(t.getTimestamp())
                            .build()
                    ).collect(Collectors.toList())
                ).build();

        routeTable.propagate(block);
    }

    public void propagate(Transaction data) {
        kTransaction transaction = kTransaction.newBuilder()
                .setSender(ByteString.copyFrom(data.getSenderAddress()))
                .setReceiver(ByteString.copyFrom(data.getRecipientAddress()))
                .setAmount(data.getAmount())
                .setTimestamp(data.getTimestamp())
                .build();

        routeTable.propagate(transaction);
    }

    public void setBlockStorageFunction(Function<kBlock, Boolean> blockStorageFunction) {
        // the function should return true if the block is valid and stored, false otherwise
        routeTable.setBlockStorageFunction(blockStorageFunction);
    }

    public void setTransactionStorageFunction(Function<kTransaction, Boolean> transactionStorageFunction) {
        // the function should return true if the transaction is valid and stored, false otherwise
        routeTable.setTransactionStorageFunction(transactionStorageFunction);
    }

    public void stop() {
        server.shutdown();
        routeTable.shutdown();
    }

}
