package org.example.Kamdelia;

import com.google.protobuf.ByteString;
import kademlia_public_ledger.Node;

class KNode {
    private final byte[] id;

    private final byte[] ip;

    private final int port;

    public byte[] getId() {
        return id;
    }

    public byte[] getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }

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
}
