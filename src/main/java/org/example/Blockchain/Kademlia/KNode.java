package org.example.Blockchain.Kademlia;

import com.google.protobuf.ByteString;
import kademlia_public_ledger.Node;
import org.example.Utils.KeysManager;
import org.example.Utils.NetUtils;

import java.util.Arrays;

class KNode {
    private final byte[] id;
    private final byte[] ip;
    private final int port;
    public static byte[] myId;
    public static int myPort;

    public KNode(byte[] id, byte[] ip, int port) {
        this.id = id;
        this.ip = ip;
        this.port = port;
    }

    public static KNode fromNode(kademlia_public_ledger.Node node) {
        return new KNode(node.getId().toByteArray(), node.getIp().toByteArray() , node.getPort());
    }

    public Node toNode() {
        return Node.newBuilder().setId(ByteString.copyFrom(id)).setIp(ByteString.copyFrom(ip)).setPort(port).build();
    }

    public byte[] getId() {
        return id;
    }

    public byte[] getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof KNode))
            return false;

        return Arrays.equals(ip, ((KNode) obj).ip) && port == ((KNode) obj).port;
    }

    @Override
    public String toString() {
        return NetUtils.IPtoString(ip) + ":" + port + " " + KeysManager.hexString(id);
    }

    public Client toClient() {
        return new Client(this, myId, myPort);
    }
}
