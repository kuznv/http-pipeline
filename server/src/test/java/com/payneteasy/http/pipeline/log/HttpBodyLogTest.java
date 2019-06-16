package com.payneteasy.http.pipeline.log;

import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class HttpBodyLogTest {

    @Test
    public void formatJson() {
        HttpBodyLog log = new HttpBodyLog(new File("."));
        assertEquals("{}", log.formatJson("{\"printer\":null,\"sync\":null}".getBytes()));
        assertEquals("{}", log.formatJson("{\"shift\":null,\"cash_action\":null,\"check\":null,\"check_item\":null,\"payment\":null,\"refund\":null,\"refund_item\":null,\"sign\":null,\"calc_check_total\":null,\"shift_recalc\":null}".getBytes()));
    }
}