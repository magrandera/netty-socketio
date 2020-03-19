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
package com.corundumstudio.socketio;

import io.netty.handler.codec.http.HttpMethod;

public class HttpRequestSignature {
    private final HttpMethod httpMethod;
    private final String path;

    public HttpRequestSignature(HttpMethod httpMethod, String path) {
        this.httpMethod = httpMethod;
        this.path = path;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpRequestSignature)) return false;

        HttpRequestSignature that = (HttpRequestSignature) o;

        if (!httpMethod.equals(that.httpMethod)) return false;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = httpMethod.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
