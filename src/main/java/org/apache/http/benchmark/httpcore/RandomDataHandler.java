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
package org.apache.http.benchmark.httpcore;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

import org.apache.http.HttpEntity;
import org.apache.http.HttpEntityEnclosingRequest;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.MethodNotSupportedException;
import org.apache.http.entity.AbstractHttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;
import org.apache.http.util.EntityUtils;

class RandomDataHandler implements HttpRequestHandler  {

    public RandomDataHandler() {
        super();
    }

    @Override
    public void handle(
            final HttpRequest request,
            final HttpResponse response,
            final HttpContext context) throws HttpException, IOException {
        final String method = request.getRequestLine().getMethod().toUpperCase(Locale.ENGLISH);
        if (!method.equals("GET") && !method.equals("HEAD") && !method.equals("POST")) {
            throw new MethodNotSupportedException(method + " method not supported");
        }
        if (request instanceof HttpEntityEnclosingRequest) {
            final HttpEntity entity = ((HttpEntityEnclosingRequest) request).getEntity();
            EntityUtils.consume(entity);
        }
        final String target = request.getRequestLine().getUri();

        int count = 100;

        final int idx = target.indexOf('?');
        if (idx != -1) {
            String s = target.substring(idx + 1);
            if (s.startsWith("c=")) {
                s = s.substring(2);
                try {
                    count = Integer.parseInt(s);
                } catch (final NumberFormatException ex) {
                    response.setStatusCode(HttpStatus.SC_BAD_REQUEST);
                    response.setEntity(new StringEntity("Invalid query format: " + s, ContentType.TEXT_PLAIN));
                    return;
                }
            }
        }
        response.setStatusCode(HttpStatus.SC_OK);
        final RandomEntity body = new RandomEntity(count);
        response.setEntity(body);
    }

    static class RandomEntity extends AbstractHttpEntity {

        private final int count;
        private final byte[] buf;

        public RandomEntity(final int count) {
            super();
            this.count = count;
            this.buf = new byte[count];
            final int r = Math.abs(this.buf.hashCode());
            for (int i = 0; i < count; i++) {
                this.buf[i] = (byte) ((r + i) % 96 + 32);
            }
            setContentType("text/plain");
        }

        @Override
        public InputStream getContent() throws IOException, IllegalStateException {
            throw new IllegalStateException("Method not supported");
        }

        @Override
        public long getContentLength() {
            return this.count;
        }

        @Override
        public boolean isRepeatable() {
            return true;
        }

        @Override
        public boolean isStreaming() {
            return false;
        }

        @Override
        public void writeTo(final OutputStream outstream) throws IOException {
            outstream.write(this.buf);
        }

    }

}
