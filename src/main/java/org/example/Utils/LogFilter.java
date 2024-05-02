package org.example.Utils;

import java.util.logging.Filter;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class LogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        Logger.getLogger(LogFilter.class.getName()).info(record.getSourceClassName());
        if (record.getSourceClassName().equals("org.example.Blockchain.Kamdelia.RouteTable") && record.getSourceMethodName().equals("add"))
            return false;
        return !record.getSourceClassName().equals("org.example.Blockchain.Kamdelia.RouteTable") || !record.getSourceMethodName().split("\\$")[2].equals("start");
    }
}
