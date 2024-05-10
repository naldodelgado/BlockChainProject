package org.example.Utils;

import org.junit.Test;

import java.util.Arrays;

import static org.example.Utils.KeysManager.*;

public class KeysManagerTest {
     @Test
    public void TestHexToString(){
        var hash = hash(new Object[]{1});
        assert Arrays.equals(getBytesFromHex(hexString(hash)), hash);
    }

    @Test
    public void TestHash(){
         byte[] hash1 = KeysManager.hash(new Object[]{1});
         byte[] hash2 = KeysManager.hash(new Object[]{2});
         assert !Arrays.equals(hash1,hash2);
    }

    @Test
    public void TestHash2(){
        byte[] hash1 = KeysManager.hash(new Object[]{1});
        byte[] hash2 = KeysManager.hash(new Object[]{1});
        assert Arrays.equals(hash1,hash2);
    }

    @Test
    public void TestHash3() {
        byte[] hash1 = KeysManager.hash(new Object[]{new byte[]{1, 2}});
        byte[] hash2 = KeysManager.hash(new Object[]{new byte[]{1, 2}});
        assert Arrays.equals(hash1, hash2);
    }

    @Test
    public void TestPublicKey() {
        var key = KeysManager.generateKeys();
        byte[] publicKey = key.getPublic().getEncoded();
        byte[] privateKey = key.getPrivate().getEncoded();

        KeysManager.getPublicKeyFromBytes(publicKey);
        KeysManager.getPrivateKeyFromBytes(privateKey);
    }


}