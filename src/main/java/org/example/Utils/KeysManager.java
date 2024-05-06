package org.example.Utils;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.security.*;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.logging.Logger;

public class KeysManager {

    private static final Logger logger = Logger.getLogger(KeysManager.class.getName());

    static {
        Security.addProvider(new BouncyCastleProvider());
    }

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

    public static byte[] createSHA1Hash(String s) {
        Digest digest = new SHA1Digest();
        byte[] data = s.getBytes();
        byte[] hash = new byte[digest.getDigestSize()];

        assert hash.length == 20;

        digest.update(data, 0, data.length);
        digest.doFinal(hash, 0);

        return hash;
    }

    public static byte[] hash(Object[] data){
        try (
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
        ) {
            // Serialize each object and write it to the stream so than the digest function is able to use multiple objects as input
            for (Object obj : data) {
                objectOutputStream.writeObject(obj);
            }

            byte[] stream = byteArrayOutputStream.toByteArray();

            // Using SHA-256 as an example hash function
            Digest digest = new SHA256Digest();
            byte[] hash = new byte[digest.getDigestSize()];

            digest.update(stream, 0, stream.length);
            digest.doFinal(hash, 0);

            objectOutputStream.close();
            byteArrayOutputStream.close();

            return hash;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] sign(PrivateKey privateKey, Object[] data){
        // hash the data
        try {
            byte[] hash = hash(data);
            Signature signature = Signature.getInstance("SHA256withECDSA", "BC");
            signature.initSign(privateKey);
            signature.update(hash);
            return signature.sign();
        } catch (InvalidKeyException | SignatureException e) {
            return null;
        } catch (NoSuchAlgorithmException | NoSuchProviderException e) {
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


    public static Boolean verifySignature(byte[] signature, byte[] hash, byte[] publicKey){
        try {
            Security.addProvider(new BouncyCastleProvider());
            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            PublicKey pub = getPublicKeyFromBytes(publicKey);
            ecdsaVerify.initVerify(pub);
            ecdsaVerify.update(hash);
            return ecdsaVerify.verify(signature);
        } catch (NoSuchAlgorithmException e){
            throw new RuntimeException(e);
        } catch (InvalidKeyException | SignatureException e) {
            return false;
        }
    }

    public static String hexString(byte[] byteArray) {
        StringBuilder hex = new StringBuilder();

        // Iterating through each byte in the array
        for (byte i : byteArray) {
            hex.append(String.format("%02X", i));
        }

        return hex.toString();
    }

    public static byte[] getBytesFromHex(String s) {
        //convert hex string to byte[]
        byte[] byteArray = new byte[s.length() / 2];
        for (int i = 0; i < byteArray.length; i++) {
            byteArray[i] = (byte) Integer.parseInt(s.substring(2 * i, 2 * i + 2), 16);
        }
        return byteArray;
    }
}
