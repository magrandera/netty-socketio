package com.corundumstudio.socketio;

import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;

public class AuthorizedResponse extends AuthorizationResponse {

    private final Map<String, Object> clientData = new HashMap<String, Object>();

    public AuthorizedResponse() {
        super(HttpResponseStatus.OK);
    }

    public static AuthorizedResponse OK() {
        return new AuthorizedResponse();
    }

    public AuthorizedResponse setClientData(String key, Object value) {
        clientData.put(key, value);
        return this;
    }

    public AuthorizedResponse setClientData(Map<String, Object> map) {
        clientData.putAll(map);
        return this;
    }


    public Map<String, Object> getClientData() {
        return clientData;
    }

}