package org.example.Blockchain;

public class Miner implements Runnable{

    private Block block;

    private boolean isMining = true;

    private BlockChain blockChain;

    public Miner(Block block, BlockChain blockChain){
        this.block = block;
        this.blockChain = blockChain;
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
            byte[] hash = block.calculateHash();
            // Check if the hash has the required number of zeros

            int i;
            for (i = 0; i < Block.numZeros; i++) {
                if (hash[i] != 0) {
                    block.setNonce(block.getNonce() + 1);
                    break;
                }
            }

            // If the hash has the required number of zeros, stop mining and  propagate the block
            if (i == Block.numZeros) {
                blockChain.propagateBlock(block);
                break;
            }

        }
    }

}
