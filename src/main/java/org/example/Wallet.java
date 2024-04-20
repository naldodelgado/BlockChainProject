package org.example;

import org.example.Auction.Utils;
import org.example.CryptoUtils.KeysManager;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.logging.Logger;

public class Wallet {

    private PrivateKey privateKey;
    private PublicKey publicKey;
    private static final Logger logger = Logger.getLogger(Wallet.class.getName());

    public Wallet(PublicKey publicKey, PrivateKey privateKey) {
        this.privateKey=privateKey;
        this.publicKey=publicKey;
    }

    public Wallet() {
        KeysManager.generateKeys();
    }

    public static String getAddressFromPubKey(PublicKey publicKey){
        return Utils.bytesToHexString(Utils.getHash(publicKey.getEncoded()));
    }

    public void printKeys(){
        logger.info("public key:" + this.publicKey.toString());
        logger.info("private key:" + this.privateKey.toString());
    }

    //Getters
    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
