package org.example.Auction;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.bouncycastle.util.encoders.Hex;

public class Utils {

     public static final Charset charset = StandardCharsets.US_ASCII;
     public static final String hashAlgorithm = "SHA-1";
     public static final byte[] HEX_ARRAY = "0123456789ABCDEF".getBytes(charset);

     private static final Logger logger = Logger.getLogger(Utils.class.getName());

     public static String getHash(String input){
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance(hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error: Could not find hash algorithm " + Utils.hashAlgorithm);
            e.printStackTrace();
            return null;
        }

        byte[] hash = digest.digest(input.getBytes(charset));
        return bytesToHexString(hash);
        //return new String(Hex.encode(hash));
    }

    public static byte[] getHash(byte[] input){
        MessageDigest digest = null;

        try {
            digest = MessageDigest.getInstance(hashAlgorithm);
        } catch (NoSuchAlgorithmException e) {
            logger.log(Level.SEVERE, "Error: Could not find hash algorithm " + Utils.hashAlgorithm);
            e.printStackTrace();
            return null;
        }

        return digest.digest(input);
    }


    public static byte[] hexStringToBytes(String string) {
        if(string == null)
            return null;

        int len = string.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(string.charAt(i), 16) << 4)
                    + Character.digit(string.charAt(i+1), 16));
        }

        return data;
    }

    public static String bytesToHexString(byte[] bytes) {
        if(bytes == null)
        return null;

        int len = bytes.length;
        byte[] hexChars = new byte[len * 2];

        for (int j = 0; j < len; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars, charset);
    }

}
