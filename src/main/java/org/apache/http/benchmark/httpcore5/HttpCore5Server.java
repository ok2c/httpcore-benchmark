/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */
package org.apache.http.benchmark.httpcore5;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.Future;

import org.apache.hc.core5.http.config.H1Config;
import org.apache.hc.core5.http.impl.bootstrap.AsyncServerBootstrap;
import org.apache.hc.core5.http.impl.bootstrap.HttpAsyncServer;
import org.apache.hc.core5.reactor.IOReactorConfig;
import org.apache.hc.core5.reactor.ListenerEndpoint;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.VersionInfo;
import org.apache.http.benchmark.BenchConsts;
import org.apache.http.benchmark.HttpServer;

public class HttpCore5Server implements HttpServer {

    private final int port;
    private final HttpAsyncServer httpAsyncServer;

    public HttpCore5Server(final int port) throws IOException {
        if (port <= 0) {
            throw new IllegalArgumentException("Server port may not be negative or null");
        }
        this.port = port;

        this.httpAsyncServer = AsyncServerBootstrap.bootstrap()
                .setH1Config(H1Config.custom()
                        .setBufferSize(BenchConsts.BUF_SIZE)
                        .setChunkSizeHint(BenchConsts.BUF_SIZE)
                        .build())
                .setIOReactorConfig(IOReactorConfig.custom()
                        .setSoReuseAddress(true)
                        .setTcpNoDelay(BenchConsts.TCP_NO_DELAY)
                        .build())
                .register("/rnd", new RandomDataHandler())
                .create();
    }

    @Override
    public String getName() {
        return "HttpCore 5 (async)";
    }

    @Override
    public String getVersion() {
        final VersionInfo vinfo = VersionInfo.loadVersionInfo("org.apache.hc.core5",
                Thread.currentThread().getContextClassLoader());
        return vinfo.getRelease();
    }

    @Override
    public int getPort() {
        return this.port;
    }

    @Override
    public void start() throws Exception {
        this.httpAsyncServer.start();
        final Future<ListenerEndpoint> future = this.httpAsyncServer.listen(new InetSocketAddress(this.port));
        future.get();
    }

    @Override
    public void shutdown() {
        this.httpAsyncServer.initiateShutdown();
        try {
            this.httpAsyncServer.awaitShutdown(TimeValue.ofSeconds(1L));
        } catch (final InterruptedException ignore) {
            Thread.currentThread().interrupt();
        }
    }

    public void awaitShutdown() throws InterruptedException {
        this.httpAsyncServer.awaitShutdown(TimeValue.ofDays(Long.MAX_VALUE));
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 1) {
            System.out.println("Usage: <port>");
            System.exit(1);
        }
        final int port = Integer.parseInt(args[0]);
        final HttpCore5Server server = new HttpCore5Server(port);
        System.out.println("Listening on port: " + port);
        server.start();
        Runtime.getRuntime().addShutdownHook(new Thread() {

            @Override
            public void run() {
                server.shutdown();
            }

        });
        server.awaitShutdown();
    }

}
