package org.example.Kamdelia;

import java.util.Arrays;
import java.util.logging.Logger;

public class NetUtils {

    public static String IPtoString(byte[] ip) {
        return String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);
    }

    public static byte[] IPfromString(String value) {
        byte[] ip = new byte[4];

        Logger.getLogger(NetUtils.class.getName()).info("fjdbgfn" + value);

        String[] values = value.split("\\.");

        Logger.getLogger(NetUtils.class.getName()).info("rjgndb" + Arrays.toString(values));

        assert values.length == 4;

        for (int i = 0; i < ip.length; i++) {
            ip[i] = (byte) Integer.parseInt(values[i]);
        }

        return ip;
    }


}
