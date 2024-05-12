package org.example.Utils;

import org.junit.Test;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Arrays;
import java.util.Optional;

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

        Optional<PublicKey> publicKey1 = getPublicKeyFromBytes(publicKey);
        Optional<PrivateKey> privateKey1 = getPrivateKeyFromBytes(privateKey);

        assert publicKey1.isPresent();
        assert privateKey1.isPresent();

        assert Arrays.equals(publicKey, publicKey1.get().getEncoded());
        assert Arrays.equals(privateKey, privateKey1.get().getEncoded());
    }


}