package org.example.Utils;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

public class LogFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        return true;
    }
}
