package org.example.Blockchain;

class Miner implements Runnable {

    private final Block block;
    private boolean isMining = true;
    private final BlockChain blockChain;

    public Miner(Block block, BlockChain blockChain){
        this.block = block;
        this.blockChain = blockChain;
    }

    public void stopMining(){
        isMining = false;
    }

    public Block getBlock(){
        return block;
    }

    @Override
    public void run() {
        while (!block.isNonceValid() && isMining) {
            block.setNonce(block.getNonce() + 1);
        }

        if(block.isNonceValid()){
            blockChain.propagateBlock(block);
        }
    }

}
