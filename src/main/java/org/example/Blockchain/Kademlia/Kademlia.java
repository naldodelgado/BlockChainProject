package org.example.Blockchain.Kademlia;

import io.grpc.Server;
import io.grpc.ServerInterceptors;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolConfig;
import io.grpc.netty.shaded.io.netty.handler.ssl.ApplicationProtocolNames;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContext;
import io.grpc.netty.shaded.io.netty.handler.ssl.SslContextBuilder;
import org.example.Blockchain.Block;
import org.example.Blockchain.BlockChain;
import org.example.Client.Transaction;
import org.example.Utils.KeysManager;
import org.example.Utils.LogFilter;
import org.example.Utils.NetUtils;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
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
            File certFile = new File("server.crt");
            File keyFile = new File("server.pem");

            SslContext sslContext = SslContextBuilder
                    .forServer(certFile, keyFile)
                    .applicationProtocolConfig(new ApplicationProtocolConfig(
                            ApplicationProtocolConfig.Protocol.ALPN,
                            // NO_ADVERTISE is client mode
                            // ACCEPT is server mode where you accept the client protocols
                            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
                            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
                            ApplicationProtocolNames.HTTP_2))
                    .build();


            byte[] id = KeysManager.createSHA1Hash(Arrays.toString(Inet4Address.getLocalHost().getAddress()) + System.currentTimeMillis() + Math.random());
            assert id.length == 20;

            routeTable = new RouteTable(id, blockChain);
            server = NettyServerBuilder
                    .forPort(port)
                    .sslContext(sslContext)
                    .addService(ServerInterceptors.intercept(new KademliaAPI(routeTable), new IPInterceptor()))
                    .build();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void start() {
        try {
            server.start();
            logger.info("Server started, listening on " + NetUtils.IPtoString(routeTable.getIP().getAddress()) + ":" + server.getPort() + " with id" + KeysManager.hexString(routeTable.getId()));
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
