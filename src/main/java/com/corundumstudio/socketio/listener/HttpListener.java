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
package com.corundumstudio.socketio.listener;

import com.corundumstudio.socketio.HttpParams;
import com.corundumstudio.socketio.HttpRequestBody;
import com.corundumstudio.socketio.HttpRequestSignature;
import com.corundumstudio.socketio.HttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

public interface HttpListener {
    HttpResponse onRequest(HttpRequestSignature signature, HttpParams params, HttpHeaders headers, HttpRequestBody body) throws Exception;
}