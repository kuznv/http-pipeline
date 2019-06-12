package com.payneteasy.http.pipeline.util;

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
        byte[] buffer = new byte[8 * 1024];
        int    count  = aInputStream.read(buffer);
        LOG.debug("Read {}", count);
        
        byte[] ret = new byte[count];
        System.arraycopy(buffer, 0, ret, 0, count);
        return ret;
    }


}
