package com.payneteasy.http.pipeline.upstream;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class UpstreamExecutor extends ThreadPoolExecutor {

    public UpstreamExecutor(int aQueueSize) {
        super(1, 1, 0, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(aQueueSize));
    }

}
