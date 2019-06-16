package com.payneteasy.http.pipeline.util;

import com.google.common.io.ByteStreams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class InputStreams {

    private static final Logger LOG = LoggerFactory.getLogger(InputStreams.class);

    public static byte[] readFully(InputStream aInputStream, int length) throws IOException {
        if(length <= 0) {
            return new byte[] {};
        }

        byte[] buffer = new byte[length];
        int    count  = 0;

        do {
            int read = aInputStream.read(buffer, count, length - count);
            if (read < 0) {
                throw new IllegalStateException("Read only " + count + " but wanted " + length + " (-1)");
            }
            count += read;
        } while (count < length);

        return buffer;
    }


    public static byte[] readAll(InputStream aInputStream) throws IOException {
        return ByteStreams.toByteArray(aInputStream);
    }


}
