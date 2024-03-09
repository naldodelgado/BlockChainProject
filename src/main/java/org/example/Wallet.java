package org.example;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.util.logging.Logger;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

public class Wallet {

    private String address;
    private PrivateKey privateKey;
    private PublicKey publicKey;
    private static final Logger logger = Logger.getLogger(Wallet.class.getName());
    
    public Wallet(String address, PublicKey publicKey, PrivateKey privateKey) {
        this.address=address;
        this.privateKey=privateKey;
        this.publicKey=publicKey;
    }


    private void generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider()); //elliptic curve
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());   //recommended curve with 256 bytes of security
            KeyPair keyPair = keyGen.generateKeyPair();
            // Set the public and private keys from the keyPair
            this.privateKey = keyPair.getPrivate();
            this.publicKey = keyPair.getPublic();

        } catch (NoSuchAlgorithmException | InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    @SuppressWarnings("finally")
    public static byte[] signHash(PrivateKey privateKey, String hash, Logger logger){
        byte[] signature = null;
        try {
            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
            ecdsaSign.initSign(privateKey);
            //ecdsaSign.update(Utils.hexStringToBytes(hash));
            signature = ecdsaSign.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.warning("There was an error signing the hash");
            e.printStackTrace();
        } finally {
            //careful errors that don't include the ones in the catch will go unnoticed
            return signature;
        }
    }

    public PrivateKey getPrivateKey() {
        return privateKey;
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }

    public String getAddress() { 
        return address; 
    }
}
