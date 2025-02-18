/*
 * Copyright (c) 2008-2025, Hazelcast, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.hazelcast.internal.networking.nio;

import com.hazelcast.internal.nio.IOUtil;
import org.junit.Assume;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.ServerSocketChannel;

public class NetworkTestUtil {

    private NetworkTestUtil() {
    }

    public static void assumeKeepAlivePerSocketOptionsNotSupported() throws Throwable {
        Assume.assumeFalse(socketSupportsKeepAliveOptions());
    }

    public static void assumeKeepAlivePerSocketOptionsSupported() throws Throwable {
        Assume.assumeTrue(socketSupportsKeepAliveOptions());
    }

    public static boolean socketSupportsKeepAliveOptions() throws Throwable {
        try (ServerSocketChannel serverSocketChannel = buildServerSocket()) {
            return IOUtil.supportsKeepAliveOptions(serverSocketChannel);
        }
    }

    private static ServerSocketChannel buildServerSocket() throws IOException {
        InetSocketAddress socketAddress = new InetSocketAddress("127.0.0.1", 0);
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        ServerSocket server = serverSocketChannel.socket();
        server.bind(socketAddress);
        return serverSocketChannel;
    }
}
