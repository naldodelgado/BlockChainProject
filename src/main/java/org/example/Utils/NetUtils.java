package org.example.Utils;

public class NetUtils {

    public static String IPtoString(byte[] ip) {
        return String.format("%d.%d.%d.%d", (int) ip[0] & 0xFF, (int) ip[1] & 0xFF, (int) ip[2] & 0xFF, (int) ip[3] & 0xFF);
    }

    public static byte[] IPfromString(String value) {
        byte[] ip = new byte[4];

        String[] values = value.split("\\.");

        assert values.length == 4;

        for (int i = 0; i < ip.length; i++) {
            ip[i] = (byte) Integer.parseInt(values[i]);
        }

        return ip;
    }

}