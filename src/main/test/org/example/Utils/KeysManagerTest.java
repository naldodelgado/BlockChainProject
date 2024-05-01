package org.example.Utils;

import org.junit.Test;

import java.util.Arrays;

import static org.example.Utils.KeysManager.*;
import static org.example.Utils.KeysManager.hexString;

public class KeysManagerTest {
     @Test
    public void TestHexToString(){
        var hash = hash(new Object[]{1});
        assert Arrays.equals(getBytesFromHex(hexString(hash)), hash);
    }
}