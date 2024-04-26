/*
 * Copyright 2015 The gRPC Authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.example.Kamdelia;

import io.grpc.*;

import java.net.SocketAddress;
import java.util.logging.Logger;

/**
 * A interceptor to handle server header.
 */
public class Interceptor implements ServerInterceptor {

    private static final Logger logger = Logger.getLogger(Interceptor.class.getName());

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            final Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler
    ) {

        SocketAddress remoteAddress = serverCall.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        String clientIp;
        int clientPort;
        if (remoteAddress != null) {
            logger.info("rsjafngv" + remoteAddress);
            String[] ipAndPort = remoteAddress.toString().split(":");
            clientIp = ipAndPort[0].substring(1);
            logger.info("fndjdgb" + clientIp);
            clientPort = Integer.parseInt(ipAndPort[1]);
        } else {
            logger.info("rsjafngv");
            // Handle the case where remote address is not available
            clientIp = "Unknown";
            clientPort = 0;
        }

        Context ctx = Context.current().withValue(Constant.IP_HEADER_KEY, clientIp)
                .withValue(Constant.PORT_HEADER_KEY, clientPort);
        return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
    }
}