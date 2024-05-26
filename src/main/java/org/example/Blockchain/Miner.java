package org.example.Blockchain;

import java.util.Date;
import java.util.logging.Logger;

class Miner implements Runnable {

    private final Block block;
    private boolean isMining = true;
    private final BlockChain blockChain;

    private static final Logger logger = Logger.getLogger(Miner.class.getName());

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
        logger.info("StartedMining");

        while (!block.isNonceValid() && isMining) {
            block.setNonce(block.getNonce() + 1);
        }

        if(block.isNonceValid()){
            blockChain.propagateBlock(block);
            BlockChain.adjustDifficulty( new Date().getTime());
        }
    }

}
