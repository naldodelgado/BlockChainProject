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


public class Interceptor implements ServerInterceptor {

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
            ServerCall<ReqT, RespT> serverCall,
            final Metadata metadata,
            ServerCallHandler<ReqT, RespT> serverCallHandler
    ) {
        SocketAddress remoteAddress = serverCall.getAttributes().get(Grpc.TRANSPORT_ATTR_REMOTE_ADDR);
        String clientIp;

        if (remoteAddress != null) {
            String[] ipAndPort = remoteAddress.toString().split(":");
            clientIp = ipAndPort[0].substring(1);
        } else {
            // Handle the case where remote address is not available
            clientIp = "Unknown";
        }

        Context ctx = Context.current().withValue(Constant.IP_HEADER_KEY, clientIp);
        return Contexts.interceptCall(ctx, serverCall, metadata, serverCallHandler);
    }
}