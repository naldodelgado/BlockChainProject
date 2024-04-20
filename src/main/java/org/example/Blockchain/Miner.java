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
        while (!block.isNonceValid() && isMining) {
            block.setNonce(block.getNonce() + 1);
        }

        if(block.isNonceValid()){
            blockChain.addBlock(block);
        }
    }

}
