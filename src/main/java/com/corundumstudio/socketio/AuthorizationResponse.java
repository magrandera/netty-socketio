package com.corundumstudio.socketio;

import io.netty.handler.codec.http.HttpResponseStatus;

public abstract class AuthorizationResponse {
    private final HttpResponseStatus httpResponseStatus;

    protected AuthorizationResponse(HttpResponseStatus httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }
}
