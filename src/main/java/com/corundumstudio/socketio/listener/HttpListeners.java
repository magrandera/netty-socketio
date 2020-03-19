package com.corundumstudio.socketio.listener;

import io.netty.handler.codec.http.HttpMethod;

public interface HttpListeners {
    void addHttpListener(HttpMethod method, String path, HttpListener listener);
}
