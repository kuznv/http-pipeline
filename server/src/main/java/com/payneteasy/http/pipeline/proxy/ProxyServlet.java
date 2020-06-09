package com.payneteasy.http.pipeline.proxy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.util.Date;
import java.util.List;

public class ProxyServlet extends HttpServlet {

    private static final Logger LOG = LoggerFactory.getLogger(ProxyServlet.class);

    private final String             upstreamBaseUrl;
    private final List<String>       headers;
    private final UpstreamHttpClient client;
    private final File               logDir;

    public ProxyServlet(File aLogDir, String upstreamBaseUrl, List<String> aHeaders) {
        this.upstreamBaseUrl = upstreamBaseUrl;
        this.headers = aHeaders;
        logDir = aLogDir;
        client = new UpstreamHttpClient();
    }

    @Override
    protected void service(HttpServletRequest aRequest, HttpServletResponse aResponse) throws ServletException, IOException {
        String requestId = System.currentTimeMillis() + "";
        try {
            ProxyRequest proxyRequest    = ProxyRequest.createFromHttpServletRequest(aRequest);
            ProxyRequest upstreamRequest = createUpstreamRequest(proxyRequest);

            saveRequest(requestId, proxyRequest, upstreamRequest);

            ProxyResponse upstreamResponse = client.executeRequest(upstreamBaseUrl, upstreamRequest);
            saveResponse(requestId, upstreamResponse);
            writeResponse(upstreamResponse, aResponse);
        } catch (Exception e) {
            LOG.error("Cannot process query {}", requestId, e);
            aResponse.setStatus(503);
            aResponse.getWriter().println("{\"error-unique-id\" : " + requestId + "}");
        }
    }

    private void writeResponse(ProxyResponse aUpstreamResponse, HttpServletResponse aResponse) throws IOException {
        aResponse.setStatus(aUpstreamResponse.getStatus());
        aUpstreamResponse.getHeaders().write(aResponse);
        if(aUpstreamResponse.getBody() != null) {
            aResponse.getOutputStream().write(aUpstreamResponse.getBody());
        }
    }

    private void saveResponse(String aRequestId, ProxyResponse aResponse) throws FileNotFoundException {
        File requestFile = createLogFile(aRequestId);
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(requestFile, true)))) {
            out.println();
            out.println("== Response");
            out.println("Date: " + new Date());
            out.println();
            aResponse.dump(out);
        }

    }

    private void saveRequest(String aRequestId, ProxyRequest aProxyRequest, ProxyRequest aUpstreamRequest) throws FileNotFoundException {
        File requestFile = createLogFile(aRequestId);
        try (PrintWriter out = new PrintWriter(new OutputStreamWriter(new FileOutputStream(requestFile)))) {
            out.println("Date: " + new Date());
            out.println();
            out.println("== Incoming Request");
            aProxyRequest.dump(out);

            out.println();
            out.println("== Upstream Request");
            aUpstreamRequest.dump(out);
        }
    }

    private File createLogFile(String aRequestId) {
        return new File(logDir, aRequestId + ".txt");
    }

    private ProxyRequest createUpstreamRequest(ProxyRequest aSource) {
        return new ProxyRequest(
                aSource.getMethod()
                , aSource.getFullPath()
                , aSource.getHeaders().copyOnly(headers, "Content-Length")
                , aSource.getBody()
        );
    }


}
