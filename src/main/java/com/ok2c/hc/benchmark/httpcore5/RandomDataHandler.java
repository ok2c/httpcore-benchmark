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
package com.ok2c.hc.benchmark.httpcore5;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.EntityDetails;
import org.apache.hc.core5.http.HttpException;
import org.apache.hc.core5.http.HttpRequest;
import org.apache.hc.core5.http.MethodNotSupportedException;
import org.apache.hc.core5.http.ProtocolException;
import org.apache.hc.core5.http.impl.BasicEntityDetails;
import org.apache.hc.core5.http.message.BasicHttpResponse;
import org.apache.hc.core5.http.nio.AsyncRequestConsumer;
import org.apache.hc.core5.http.nio.AsyncResponseProducer;
import org.apache.hc.core5.http.nio.AsyncServerRequestHandler;
import org.apache.hc.core5.http.nio.DataStreamChannel;
import org.apache.hc.core5.http.nio.ResponseChannel;
import org.apache.hc.core5.http.nio.entity.NoopEntityConsumer;
import org.apache.hc.core5.http.nio.support.AbstractAsyncRequesterConsumer;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.http.HttpStatus;

class RandomDataHandler implements AsyncServerRequestHandler<Integer> {

    @Override
    public AsyncRequestConsumer<Integer> prepare(
            final HttpRequest request,
            final EntityDetails entityDetails,
            final HttpContext context) throws HttpException {
        final String method = request.getMethod();
        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }
        final String path = request.getPath();
        final AtomicInteger count = new AtomicInteger(100);

        final int idx = path.indexOf('?');
        if (idx != -1) {
            String s = path.substring(idx + 1);
            if (s.startsWith("c=")) {
                s = s.substring(2);
                try {
                    count.set(Integer.parseInt(s));
                } catch (final NumberFormatException ex) {
                    throw new ProtocolException("Invalid query format");
                }
            }
        }

        return new AbstractAsyncRequesterConsumer<Integer, Void>(new NoopEntityConsumer()) {

            @Override
            protected Integer buildResult(final HttpRequest request, final Void entity, final ContentType contentType) {
                return count.get();
            }

        };
    }

    @Override
    public void handle(
            final Integer count,
            final ResponseTrigger responseTrigger,
            final HttpContext context) throws HttpException, IOException {

        final byte[] b = new byte[count];
        final int r = Math.abs(b.hashCode());
        for (int i = 0; i < count; i++) {
            b[i] = (byte) ((r + i) % 96 + 32);
        }
        final ByteBuffer buf = ByteBuffer.wrap(b);

        responseTrigger.submitResponse(new AsyncResponseProducer() {

            @Override
            public void sendResponse(
                    final ResponseChannel channel, final HttpContext context) throws HttpException, IOException {
                channel.sendResponse(
                        new BasicHttpResponse(HttpStatus.SC_OK),
                        new BasicEntityDetails(count, ContentType.TEXT_PLAIN),
                        context);
            }

            @Override
            public int available() {
                return buf.remaining();
            }

            @Override
            public void produce(final DataStreamChannel channel) throws IOException {
                channel.write(buf);
                if (!buf.hasRemaining()) {
                    channel.endStream(null);
                }
            }

            @Override
            public void failed(final Exception ignore) {
            }

            @Override
            public void releaseResources() {
            }

        }, context);
    }

}
