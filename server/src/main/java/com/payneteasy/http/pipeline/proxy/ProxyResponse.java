package com.payneteasy.http.pipeline.proxy;

import lombok.Data;

import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Data
public class ProxyResponse {

    private final int         status;
    private final HttpHeaders headers;
    private final byte[]      body;

    public void dump(PrintWriter out) {
        out.println("Status: " + status);
        out.println("Headers: ");
        headers.dump(out);
        out.write(new String(body, StandardCharsets.UTF_8));
    }
}
