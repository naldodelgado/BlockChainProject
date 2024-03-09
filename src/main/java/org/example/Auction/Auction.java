package org.example.Auction;

import java.security.PublicKey;
import java.util.logging.Logger;

import org.example.Wallet;

public class Auction {

    private final String idItem;
    private final String idSeller;
    private long minAmount;
    private float minIncrement;
    private long fee;
    private long timeout;
    private  final PublicKey sellerPublicKey;
    private final String hash;
    private final byte[] signature;
    private static final Logger logger = Logger.getLogger(Auction.class.getName());
    
    public Auction(String idItem, String idSeller, long minAmount, float minIncrement, long fee, long timeout, PublicKey sellerPublicKey, String hash, byte[] signature) {
        this.idItem = idItem;
        this.idSeller = idSeller;
        this.minAmount = minAmount;
        this.minIncrement = minIncrement;
        this.fee = fee;
        this.timeout = timeout;
        this.sellerPublicKey = sellerPublicKey;
        this.hash = hash;
        this.signature = signature;
    }
    public Auction(String idItem, long minAmount, float minIncrement, long fee, long timeout, Wallet seller){
        this.idItem=idItem;
        this.idSeller=seller.getAddress();
        this.minAmount=minAmount;
        this.minIncrement=minIncrement;
        this.fee = fee;
        this.timeout = timeout;
        this.sellerPublicKey= seller.getPublicKey();
        this.hash= this.getHashToBeSigned();
        this.signature = Wallet.signHash(seller.getPrivateKey(), this.hash, logger);
    }

    public Boolean verifyAuction(){
        return false;
        /*return  isHashValid()
                && Wallet.verifySignature(this.signature, this.hash, this.sellerPublicKey, logger)
                && Wallet.checkAddress(this.sellerPublicKey, this.idSeller);*/
    }
    
    private String getHashToBeSigned() {
        return "";
        //return Utils.getHash("" + this.idItem + this.idSeller + this.minAmount + this.minIncrement + this.fee + this.timeout + this.sellerPublicKey.hashCode());
    }

    private Boolean isHashValid() {
        if(!this.hash.equals(this.getHashToBeSigned())){
            logger.warning("Auction Hashes don't match");
            return false;
        }
        else return true;
    }

    public PublicKey getSellerPublicKey() {
        return sellerPublicKey;
    }

    public String getHash() {
        return hash;
    }

    public byte[] getSignature() {
        return signature;
    }

    public String get_idItem() {
        return idItem;
    }

    public String get_idSeller() {
        return idSeller;
    }

    public long getMinAmount() {
        return minAmount;
    }

    public float getMinIncrement() {
        return minIncrement;
    }

    public long getFee() {
        return fee;
    }

    public long getTimeout() {
        return timeout;
    }
}
