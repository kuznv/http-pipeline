package com.payneteasy.http.pipeline.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.util.*;

public class HttpHeaders {

    private static final Logger LOG = LoggerFactory.getLogger(HttpHeaders.class);

    private final Map<String, String> headers;

    private HttpHeaders(Map<String, String> aHeaders) {
        this.headers = toLowerCase(aHeaders);
    }

    private static Map<String, String> toLowerCase(Map<String, String> aHeaders) {
        Map<String, String> map = new HashMap<>();
        for (Map.Entry<String, String> entry : aHeaders.entrySet()) {
            if(entry.getKey() == null || entry.getValue() == null) {
                continue;
            }
            map.put(entry.getKey().toLowerCase(), entry.getValue());
        }
        return map;
    }

    public static HttpHeaders createFromRequest(HttpServletRequest aRequest) {
        Map<String, String> headers = new HashMap<>();
        Enumeration<String> en      = aRequest.getHeaderNames();
        while (en.hasMoreElements()) {
            String name = en.nextElement();
            headers.put(name, aRequest.getHeader(name));
        }
        return new HttpHeaders(headers);

    }

    public static HttpHeaders createEmpty() {
        return new HttpHeaders(Collections.emptyMap());
    }

    public static HttpHeaders createFromConnection(HttpURLConnection aConnection) {
        Map<String, String> map = new HashMap<>();
        Map<String, List<String>> headerFields = aConnection.getHeaderFields();
        for (Map.Entry<String, List<String>> entry : headerFields.entrySet()) {
            if(entry == null || entry.getKey() == null) {
                continue;
            }
            String key = entry.getKey();
            String value =  aConnection.getHeaderField(key);
            map.put(key, value);
        }
        return new HttpHeaders(map);
    }

    public void dump(PrintWriter aOut) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            aOut.printf("  %s = %s\n", entry.getKey(), entry.getValue());
        }
    }

    public HttpHeaders copyOnly(List<String> aList, String aRequired) {
        Map<String, String> map = new HashMap<>();

        map.put(aRequired.toLowerCase(), headers.get(aRequired.toLowerCase()));

        for (String header : aList) {
            String key   = header.toLowerCase();
            String value = headers.get(key);
            LOG.debug(" header copy: {} = {}", key, value);
            if(value != null) {
                map.put(key, value);
            }
        }
        return new HttpHeaders(map);
    }

    public void write(HttpServletResponse aResponse) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            aResponse.addHeader(entry.getKey(), entry.getValue());
        }
    }

    public void writeToConnection(HttpURLConnection connection) {
        for (Map.Entry<String, String> entry : headers.entrySet()) {
            LOG.debug(" header set: {} = {}", entry.getKey(), entry.getValue());
            if(entry.getValue() == null) {
                continue;
            }
            connection.setRequestProperty(entry.getKey(), entry.getValue());
        }
    }
}
