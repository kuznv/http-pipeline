package com.payneteasy.http.pipeline.proxy;

import com.google.common.base.Strings;
import com.payneteasy.http.pipeline.util.InputStreams;
import lombok.Data;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

@Data
public class ProxyRequest {

    private final String      method;
    private final String      fullPath;
    private final HttpHeaders headers;
    private final byte[]      body;

    public static ProxyRequest createFromHttpServletRequest(HttpServletRequest aRequest) throws IOException {
        return new ProxyRequest(
                aRequest.getMethod()
                , createFullPath(aRequest)
                , HttpHeaders.createFromRequest(aRequest)
                , extractBody(aRequest)
        );
    }

    private static byte[] extractBody(HttpServletRequest aRequest) throws IOException {
        return InputStreams.readFully(aRequest.getInputStream(), aRequest.getContentLength());
    }

    private static String createFullPath(HttpServletRequest aRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append(aRequest.getRequestURI());
        if (!Strings.isNullOrEmpty(aRequest.getQueryString())) {
            sb.append('?');
            sb.append(aRequest.getQueryString());
        }
        return sb.toString();
    }

    public void dump(PrintWriter aOut) {
        aOut.println("  Method   :  " + method);
        aOut.println("  FullPath : " + fullPath);

        aOut.println("  Headers  :");
        headers.dump(aOut);

        aOut.println("  Body:");
        aOut.write(new String(body, StandardCharsets.UTF_8));

    }
}
