package com.payneteasy.http.pipeline.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class ChangeWriteHttpBodyServlet extends HttpServlet {

    private final AtomicBoolean writeLog;

    public ChangeWriteHttpBodyServlet(AtomicBoolean writeLog) {
        this.writeLog = writeLog;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.getWriter().println(writeLog);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        writeLog.set(Boolean.parseBoolean(req.getParameter("enabled")));
        resp.getWriter().println(writeLog);
    }

}
