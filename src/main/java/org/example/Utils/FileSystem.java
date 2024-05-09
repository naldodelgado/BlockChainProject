package org.example.Utils;

import java.nio.file.FileSystems;


public class FileSystem {
    public final static String fileSeparator = FileSystems.getDefault().getSeparator();
    public final static String blockchainPath = System.getProperty("user.dir") + fileSeparator + "blockchain" + fileSeparator;
    public final static String auctionPath = "blockchain" + fileSeparator + "transactions" + fileSeparator + "auctions" + fileSeparator;
    public final static String bidPath = "blockchain" + fileSeparator + "transactions" + fileSeparator + "bids" + fileSeparator;
}
