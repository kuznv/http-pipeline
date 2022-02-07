package com.payneteasy.http.pipeline.client;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public class HttpResponse {

    private final int status;
    private final byte[] responseBody;
    private final Map<String, List<String>> headerFields;
    private final String errorMessage;

    public HttpResponse(int status, byte[] responseBody, Map<String, List<String>> headerFields, String aErrorMessage) {
        this.status = status;
        this.responseBody = responseBody;
        this.headerFields = headerFields;
        errorMessage = aErrorMessage;
    }

    public static HttpResponse error(int aStatusCode, String aMessage, Map<String, List<String>> headerFields) {
        return new HttpResponse(aStatusCode, null, headerFields, aMessage);
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

    public Map<String, List<String>> getHeaderFields() {
        return headerFields;
    }

    public void dump(PrintWriter out) {
        out.println("Status: " + status);
        out.println("Error: " + errorMessage);
        out.write(new String(responseBody, StandardCharsets.UTF_8));
    }
}
