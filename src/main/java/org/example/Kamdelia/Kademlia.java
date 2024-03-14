package org.example.Kamdelia;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

import java.io.IOException;
import java.net.Inet4Address;
import java.util.Arrays;
import java.util.logging.Logger;

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
        server = ServerBuilder.forPort(port).addService(new KademliaAPI(routeTable)).build();
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

}
