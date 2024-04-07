package org.example.Blockchain;

public class Miner implements Runnable{

    private Block block;

    private boolean isMining = true;

    public Miner(Block block){
        this.block = block;
    }

    public void SetBlock(Block block){
        this.block = block;
    }

    public void stopMining(){
        isMining = false;
    }

    public void startMining(){
        isMining = true;
    }

    public Block getBlock(){
        return block;
    }

    @Override
    public void run() {
        // Mining logic
        while (isMining) {
            block.calculateHash();
            byte[] hash = block.getHash();
            // Check if the hash has the required number of zeros
            for (int i = 0; i < Block.numZeros; i++) {
                if (hash[i] != 0) {
                    block.setNonce(block.getNonce() + 1);
                    break;
                }
            }

            //TODO: stop the mining process and propagate the block to the network

        }
    }



}
