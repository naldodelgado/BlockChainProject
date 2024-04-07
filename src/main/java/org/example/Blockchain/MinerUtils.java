package org.example.Blockchain;

public class MinerUtils {

    //find the block from the network

    //find the last block in the public ledger
    public static Block findLastBlock(){ // TODO: return a @BLOCK instead of @VOID
        return new Block(0);
    }
    public static byte[] calculateHash(Block currentBlock){
        // Use SHA-256 as an example hash function
        Block lastBlock = findLastBlock();
        currentBlock.setPreviousHash(lastBlock.getPreviousHash());
        currentBlock.calculateHash();
        return new byte[32];
    }
}
