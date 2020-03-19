/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.namespace;


import com.corundumstudio.socketio.*;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.listener.HttpListener;
import com.corundumstudio.socketio.listener.HttpListeners;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.internal.PlatformDependent;

import java.util.concurrent.ConcurrentMap;


public class HttpNamespace implements HttpListeners {

    private final ConcurrentMap<HttpRequestSignature, HttpListener> httpListeners = PlatformDependent.newConcurrentHashMap();
    private final ExceptionListener exceptionListener;

    public HttpNamespace(Configuration configuration) {
        this.exceptionListener = configuration.getExceptionListener();
    }

    public void addHttpListener(HttpMethod method, String path, HttpListener listener) {
        HttpRequestSignature signature = new HttpRequestSignature(method, path);
        httpListeners.put(signature, listener);
    }

    public boolean hasListeners() {
        return !httpListeners.isEmpty();
    }

    public HttpResponse onRequest(HttpRequestSignature httpRequestSignature, HttpParams params, HttpHeaders headers, HttpRequestBody body) {
        HttpListener httpListener = httpListeners.get(httpRequestSignature);
        if (httpListener == null) return null;

        try {
            return httpListener.onRequest(httpRequestSignature, params, headers, body);
        } catch (Exception e) {
            exceptionListener.onHttpException(e, httpRequestSignature);
            return HttpResponse.INTERNAL_SERVER_ERROR();
        }
    }

}
