package com.corundumstudio.socketio;

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;

public class HttpResponse {

    private final HttpResponseStatus httpResponseStatus;
    private final HttpHeaders httpHeaders = new DefaultHttpHeaders();
    private String body;
    private String contentType = "text/plain";
    private Charset charset = CharsetUtil.UTF_8;

    public HttpResponse(HttpResponseStatus httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
    }

    public static HttpResponse OK() {
        return new HttpResponse(HttpResponseStatus.OK);
    }

    public static HttpResponse TEMPORARY_REDIRECT(String locationUrl) {
        HttpResponse authorizationResponse = new HttpResponse(HttpResponseStatus.TEMPORARY_REDIRECT);
        authorizationResponse.getHeaders().add(HttpHeaderNames.LOCATION, locationUrl);
        return authorizationResponse;
    }

    public static HttpResponse UNAUTHORIZED() {
        return new HttpResponse(HttpResponseStatus.UNAUTHORIZED);
    }

    public static HttpResponse INTERNAL_SERVER_ERROR() {
        return new HttpResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public HttpResponse setHeader(AsciiString name, String value) {
        httpHeaders.add(name, value);
        return this;
    }

    public HttpResponse setHeaders(HttpHeaders headers) {
        httpHeaders.setAll(headers);
        return this;
    }

    public HttpResponse setHeaders(Map<AsciiString, List<String>> headers) {
        for (Map.Entry<AsciiString, List<String>> header : headers.entrySet()) {
            AsciiString name = header.getKey();
            List<String> values = header.getValue();
            for (String value : values) {
                httpHeaders.set(name, value);
            }
        }
        return this;
    }

    public HttpHeaders getHeaders() {
        return httpHeaders;
    }

    public HttpResponse setBody(String body) {
        this.body = body;
        return this;
    }

    public HttpResponse setBody(String body, String contentType) {
        this.body = body;
        this.contentType = contentType;
        return this;
    }

    public HttpResponse setBody(String body, String contentType, Charset charset) {
        this.body = body;
        this.contentType = contentType;
        this.charset = charset;
        return this;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public Charset getCharset() {
        return charset;
    }
}
