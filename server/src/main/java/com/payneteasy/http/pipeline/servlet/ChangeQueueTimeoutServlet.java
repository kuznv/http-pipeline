package com.payneteasy.http.pipeline.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

public class ChangeQueueTimeoutServlet extends HttpServlet {

    private final AtomicInteger waitTimeout;

    public ChangeQueueTimeoutServlet(AtomicInteger waitTimeout) {
        this.waitTimeout = waitTimeout;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().println(waitTimeout);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        waitTimeout.set(Integer.parseInt(req.getParameter("timeout")));
        resp.getWriter().println(waitTimeout);
    }

}
