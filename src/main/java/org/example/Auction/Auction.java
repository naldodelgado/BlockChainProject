package org.example.Auction;

import org.example.CryptoUtils.KeysManager;
import org.example.Wallet;

import java.security.PublicKey;
import java.util.Arrays;
import java.util.logging.Logger;

public class Auction {

    private final String idItem;
    private final PublicKey idSeller;
    private final long minAmount;
    private final long minIncrement;
    private final long timeout;
    private  final PublicKey sellerPublicKey;
    private final byte[] hash;
    private final byte[] signature;
    private static final Logger logger = Logger.getLogger(Auction.class.getName());
    
    public Auction(String idItem, PublicKey idSeller, long minAmount, long minIncrement, long timeout, PublicKey sellerPublicKey, byte[] hash, byte[] signature) {
        this.idItem = idItem;
        this.idSeller = idSeller;
        this.minAmount = minAmount;
        this.minIncrement = minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey = sellerPublicKey;
        this.hash = hash;
        this.signature = signature;
    }
    public Auction(String idItem, long minAmount, long minIncrement, long fee, long timeout, Wallet seller){
        this.idItem=idItem;
        this.idSeller=seller.getPublicKey();
        this.minAmount=minAmount;
        this.minIncrement=minIncrement;
        this.timeout = timeout;
        this.sellerPublicKey= seller.getPublicKey();
        this.hash= KeysManager.hash(new Object[]{this.idItem, this.idSeller, this.minAmount, this.minIncrement, this.timeout, this.sellerPublicKey.hashCode()});

        Object[] objects = {this.idItem, this.idSeller, this.minAmount, this.minIncrement, this.timeout, this.sellerPublicKey.hashCode()};

        this.signature = KeysManager.sign(seller.getPrivateKey(), objects);
    }

    public Boolean verifyAuction(){
        Object[] objects = {this.idItem, this.idSeller, this.minAmount, this.minIncrement, this.timeout, this.sellerPublicKey.hashCode()};
        if(
                !Arrays.equals(this.hash, KeysManager.hash(objects))
                &&  KeysManager.verifySignature(this.signature, this.hash, this.sellerPublicKey)
                &&  this.sellerPublicKey.equals(this.idSeller)
        ){
            logger.warning("Auction Hashes don't match");
            return false;
        } else
            return true;
    }

    //Getters
    public PublicKey getSellerPublicKey() {
        return sellerPublicKey;
    }

    public byte[] getHash() {
        return hash;
    }

    public byte[] getSignature() {
        return signature;
    }

    public String get_idItem() {
        return idItem;
    }

    public PublicKey get_idSeller() {
        return idSeller;
    }

    public long getMinAmount() {
        return minAmount;
    }

    public float getMinIncrement() {
        return minIncrement;
    }

    public long getTimeout() {
        return timeout;
    }
}
