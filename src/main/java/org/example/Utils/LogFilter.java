package org.example.Utils;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class LogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        if (record.getSourceClassName().equals("org.example.Blockchain.Kademlia.RouteTable")) {
            if (record.getSourceMethodName().equals("add")) {
                return false;
            } else if (record.getSourceMethodName().equals("lambda$start$0")) {
                return false;
            } else if (record.getSourceMethodName().equals("start")) {
                return false;
            } else if (record.getSourceMethodName().equals("findNode")) {
                return false;
            }
        }
        if (record.getSourceClassName().equals("org.example.Blockchain.Kademlia.KademliaAPI")) {
            return !record.getSourceMethodName().equals("findNode");
        }

        return true;
    }
}
