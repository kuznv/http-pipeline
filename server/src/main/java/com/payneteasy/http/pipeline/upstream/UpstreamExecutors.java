package com.payneteasy.http.pipeline.upstream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public class UpstreamExecutors {

    private static final Logger LOG = LoggerFactory.getLogger(UpstreamExecutors.class);

    private volatile int                activeExecutors;
    private final    UpstreamExecutor[] executors;

    public UpstreamExecutors(int aCount, int aQueueSize, int aActiveExecutors) {
        if(aActiveExecutors > aCount) {
            throw new IllegalArgumentException("ActiveExecutors should be less or equal to count");
        }
        activeExecutors = aActiveExecutors;
        executors = createExecutors(aCount, aQueueSize);
    }

    private static UpstreamExecutor[] createExecutors(int aCount, int aQueueSize) {
        UpstreamExecutor[] services = new UpstreamExecutor[aCount];
        for(int i=0; i<aCount; i++) {
            services[i] = new UpstreamExecutor(aQueueSize);
        }
        return services;
    }

    public ExecutorService findExecutor(String aKey) {
        if(aKey.startsWith("/api/v2/app/log")) {
            LOG.debug("Returned dedicated (0) executor for log");
            return executors[0];
        } else {
            final int hash = aKey.hashCode();
            final int mod  = activeExecutors - 1;
            final int index = Math.abs(hash % mod) + 1;

            LOG.debug("Found executor at index {} (hash={}, active={}, mod={})", index, hash, activeExecutors, mod);

            return executors[index];
        }
    }

    public List<BlockingQueue> getQueues() {
        List<BlockingQueue> queues = new ArrayList<>();
        for (UpstreamExecutor executor : executors) {
            queues.add(executor.getQueue());
        }
        return queues;
    }

    public void setActiveExecutors(int aCount) {
        if(aCount > executors.length) {
            throw new IllegalArgumentException("Trying to set more than " + executors.length + " active executors ");
        }
        if(aCount < 1) {
            throw new IllegalArgumentException("Active executors should be more than 0");
        }
        activeExecutors = aCount;
    }

    public List<UpstreamExecutor> geExecutors() {
        return new ArrayList<>(Arrays.asList(executors));
    }

    public void stop() {
        LOG.debug("Stopping executors ...");
        for (UpstreamExecutor executor : executors) {
            List<Runnable> runnables = executor.shutdownNow();
            for (Runnable runnable : runnables) {
                if(runnable instanceof UpstreamTask) {
                    UpstreamTask task = (UpstreamTask) runnable;
                    LOG.debug("Queued: {}", task.getHttpRequest().getUrl());
                }
            }
        }

        for (UpstreamExecutor executor : executors) {
            try {
                boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                LOG.debug("Skip interruption");
            }
        }
    }

    public int getActiveExecutorsCount() {
        return activeExecutors;
    }

    public int getMaxExecutorsCount() {
        return executors.length;
    }
}
