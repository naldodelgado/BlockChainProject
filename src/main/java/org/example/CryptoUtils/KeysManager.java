package org.example.CryptoUtils;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.example.Auction.Utils;

import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

public class KeysManager {

    public static KeyPair generateKeys() {
        try {
            KeyPairGenerator keyGen = KeyPairGenerator.getInstance("EC", new BouncyCastleProvider()); //elliptic curve
            // Initialize the key generator and generate a KeyPair
            keyGen.initialize(new ECGenParameterSpec("secp256r1"), new SecureRandom());   //recommended curve with 256 bytes of security
            // Set the public and private keys from the keyPair
            return keyGen.generateKeyPair();
        } catch (InvalidAlgorithmParameterException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }

    }

    public static byte[] signHash(PrivateKey privateKey, String hash, Logger logger){
        try {
            Signature ecdsaSign = Signature.getInstance("SHA256withECDSA");
            ecdsaSign.initSign(privateKey);
            //ecdsaSign.update(Utils.hexStringToBytes(hash));
            return ecdsaSign.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            throw new RuntimeException(e);
        }
    }

    public static PublicKey getPublicKeyFromBytes(byte[] bKey){
        X509EncodedKeySpec pubKeySpec = new X509EncodedKeySpec(bKey);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            return keyFactory.generatePublic(pubKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }

    public static PrivateKey getPrivateKeyFromBytes(byte[] pKey){
        PKCS8EncodedKeySpec privateKeySpec = new PKCS8EncodedKeySpec(pKey);
        try {
            KeyFactory keyFactory = KeyFactory.getInstance("EC", new BouncyCastleProvider());
            return keyFactory.generatePrivate(privateKeySpec);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }
    }


    public static Boolean verifySignature(byte[] signature, String hash, PublicKey publicKey){
        try {
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(Utils.hexStringToBytes(hash));
            return ecdsaVerify.verify(signature);
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            return false;
        }
    }
}
