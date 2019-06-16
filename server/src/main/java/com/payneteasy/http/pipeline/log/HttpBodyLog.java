package com.payneteasy.http.pipeline.log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

public class HttpBodyLog {

    private static final Logger LOG = LoggerFactory.getLogger(HttpBodyLog.class);

    private final AtomicLong index  = new AtomicLong();
    private final JsonParser parser = new JsonParser();
    private final Gson       gson   = new GsonBuilder().setPrettyPrinting().create();
    private final File       dir;

    public HttpBodyLog(File dir) {
        this.dir = dir;
    }

    public void log(String aPath, byte[] aRequestBody, byte[] aResponseBody) {
        try {
            String id = "http-" + System.currentTimeMillis() + "-" + index.incrementAndGet() + "-" + aPath;
            writeJson(aRequestBody, new File(dir, id + "-in.json"));
            writeRaw(aResponseBody, new File(dir, id + "-out-raw.json"));
            writeJson(aResponseBody, new File(dir, id + "-out.json"));
        } catch (IOException e) {
            LOG.error("Cannot write json", e);
        }
    }

    private void writeJson(byte[] aRequestBody, File aFile) throws IOException {
        try(OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(aFile), "utf-8")) {
            out.write(formatJson(aRequestBody));
        }
    }

    private void writeRaw(byte[] aBody, File aFile) throws IOException {
        try(FileOutputStream out = new FileOutputStream(aFile)) {
            out.write(aBody);
        }
    }

    protected String formatJson(byte[] aBody) {
        JsonElement jsonElement = parser.parse(new String(aBody, StandardCharsets.UTF_8));
        return gson.toJson(jsonElement);
    }
}
