package org.example.Utils;

import java.nio.file.FileSystems;

public class FileSystem {
    public static String fileSeparator = FileSystems.getDefault().getSeparator();
    public static String blockchainPath = System.getProperty("user.dir") + fileSeparator + "blockchain" + fileSeparator;
    public static String auctionPath = "blockchain"+ fileSeparator + "transactions"+ fileSeparator + "auctions" + fileSeparator;
    public static String bidPath = "blockchain"+ fileSeparator + "transactions"+ fileSeparator + "bids" + fileSeparator;
}
