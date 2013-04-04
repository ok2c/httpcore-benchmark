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
package org.apache.http.benchmark;

import java.net.URL;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.PosixParser;
import org.apache.http.benchmark.httpcore.HttpCoreNIOServer;
import org.apache.http.benchmark.httpcore.HttpCoreServer;
import org.apache.http.benchmark.jetty.JettyNIOServer;
import org.apache.http.benchmark.jetty.JettyServer;
import org.apache.http.benchmark.netty.NettyNIOServer;

public class Benchmark {

    private static final int PORT = 8989;

    public static void main(final String[] args) throws Exception {

        final Config config = new Config();
        if (args.length > 0) {
            final Options options = CommandLineUtils.getOptions();
            final CommandLineParser parser = new PosixParser();
            final CommandLine cmd = parser.parse(options, args);
            if (cmd.hasOption('h')) {
                final HelpFormatter formatter = new HelpFormatter();
                formatter.printHelp("Benchmark [options]", options);
                System.exit(1);
            }
            CommandLineUtils.parseCommandLine(cmd, config);
        } else {
            config.setKeepAlive(true);
            config.setRequests(100000);
            config.setThreads(50);
        }

        final URL target = new URL("http", "localhost", PORT, "/rnd?c=2048");
        config.setUrl(target);

        final Benchmark benchmark = new Benchmark();
        benchmark.run(new JettyServer(PORT), config);
        benchmark.run(new HttpCoreServer(PORT), config);
        benchmark.run(new JettyNIOServer(PORT), config);
        benchmark.run(new HttpCoreNIOServer(PORT), config);
        benchmark.run(new NettyNIOServer(PORT), config);
    }

    public Benchmark() {
        super();
    }

    public void run(final HttpServer server, final Config config) throws Exception {
        server.start();
        try {
            System.out.println("---------------------------------------------------------------");
            System.out.println(server.getName() + "; version: " + server.getVersion());
            System.out.println("---------------------------------------------------------------");

            final Config warmupConfig = config.copy();
            int n = warmupConfig.getRequests() / 100;
            if (n > 100) {
                n = 100;
            }
            warmupConfig.setRequests(n);
            final HttpBenchmark warmUp = new HttpBenchmark(warmupConfig);
            warmUp.doExecute();

            final HttpBenchmark benchmark = new HttpBenchmark(config);
            benchmark.execute();
            System.out.println("---------------------------------------------------------------");
            Thread.sleep(3000);
        } finally {
            server.shutdown();
        }
    }

}
