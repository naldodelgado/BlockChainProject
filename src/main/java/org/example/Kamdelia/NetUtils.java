package org.example.Kamdelia;

public class NetUtils {

    public static String IPtoString(byte[] ip) {
        return String.format("%d.%d.%d.%d", ip[0], ip[1], ip[2], ip[3]);
    }

    public static byte[] IPfromString(String value) {
        byte[] ip = new byte[4];

        String[] values = value.split("\\.");

        assert values.length == 4;

        for (int i = 0; i < ip.length; i++) {
            ip[i] = Byte.parseByte(values[i]);
        }

        return ip;
    }


}
