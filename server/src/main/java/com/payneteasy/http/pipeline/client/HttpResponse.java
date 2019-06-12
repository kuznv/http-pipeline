package com.payneteasy.http.pipeline.client;

public class HttpResponse {

    private final int status;
    private final byte[] responseBody;
    private final String errorMessage;

    public HttpResponse(int status, byte[] responseBody, String aErrorMessage) {
        this.status = status;
        this.responseBody = responseBody;
        errorMessage = aErrorMessage;
    }

    public static HttpResponse error(int aStatusCode, String aMessage) {
        return new HttpResponse(aStatusCode, null, aMessage);
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getStatus() {
        return status;
    }

    public byte[] getResponseBody() {
        return responseBody;
    }
}
