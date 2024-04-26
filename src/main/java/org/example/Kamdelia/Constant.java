package org.example.Kamdelia;

import io.grpc.Context;

public class Constant {
    static final Context.Key<String> IP_HEADER_KEY = Context.key("ip");
    static final Context.Key<Integer> PORT_HEADER_KEY = Context.key("port");

}
