package org.example;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.example.Auction.Utils;

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

     public static PublicKey getPublicKeyFromBytes(byte[] bKey){
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(bKey);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            return keyFactory.generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            e.printStackTrace();
        }
        logger.warning("Couldn't retrieve public key from bytes");
        return null;
    }
    public static boolean checkAddress(PublicKey publicKey, String address){
        if(getAddressFromPubKey(publicKey).equals(address))
            return true;
        else {
            logger.warning("public key doesn't correspond to address !");
            return false;
        }
    }
    public static String getAddressFromPubKey(PublicKey publicKey){
        return Utils.bytesToHexString(Utils.getHash(publicKey.getEncoded()));
    }

    @SuppressWarnings("finally")
    public static Boolean verifySignature(byte[] signature, String hash, PublicKey publicKey, Logger logger){
        if(signature==null){
            logger.warning("Hash is not signed");
            return false;
        }
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(Utils.hexStringToBytes(hash));
            ecdsaVerify.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            logger.severe("Signatures don't match");
            e.printStackTrace();
            return false;
        } finally{
            //careful errors that don't include the ones in the catch will go unnoticed
            return true;
        }
    }



    //Getters
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
