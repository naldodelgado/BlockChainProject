package org.example.Utils;

import java.nio.file.FileSystems;

public class FileSystem {
    public final String fileSeparator = FileSystems.getDefault().getSeparator();
    public final String blockchainPath = System.getProperty("user.dir") + fileSeparator + "blockchain" + fileSeparator;
    public final String auctionPath = "blockchain"+ fileSeparator + "transactions"+ fileSeparator + "auctions" + fileSeparator;
    public final String bidPath = "blockchain"+ fileSeparator + "transactions"+ fileSeparator + "bids" + fileSeparator;
}
