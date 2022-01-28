package com.payneteasy.http.pipeline.upstream;

import com.payneteasy.http.pipeline.client.HttpResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class UpstreamExecutor {
    private static final Logger LOG = LoggerFactory.getLogger(UpstreamExecutor.class);

    private final int maxByUser;
    private final int maxTotal;
    private final AtomicInteger currentlyRunning = new AtomicInteger();

    private final Map<String, UserTaskQueue> tasksByUser = new HashMap<>();
    private final ArrayDeque<String> userQueue = new ArrayDeque<>();
    private final ThreadPoolExecutor executor;

    public UpstreamExecutor(int maxByUser, int maxTotal) {
        executor = new ThreadPoolExecutor(maxTotal, maxTotal, 0L, TimeUnit.SECONDS, new ArrayBlockingQueue<>(maxTotal));

        this.maxByUser = maxByUser;
        this.maxTotal = maxTotal;
    }

    public Future<HttpResponse> submit(UpstreamTask task) {
        UpstreamTaskFuture future = new UpstreamTaskFuture(task);
        String user = task.getUser();

        synchronized (this) {
            UserTaskQueue userTasks = tasksByUser.computeIfAbsent(user, u -> new UserTaskQueue());
            userTasks.tasks.addLast(future);
            userQueue.addLast(user);
            tryScheduleNext();
        }

        return future;
    }

    public int getActiveTasksCount() {
        return executor.getActiveCount();
    }

    public int getActiveTasksCountByUser(String user) {
        UserTaskQueue userTaskQueue;

        synchronized (this) {
            userTaskQueue = tasksByUser.get(user);
        }

        if (userTaskQueue == null) {
            return 0;
        }

        return userTaskQueue.currentlyRunningByUser.get();
    }

    public synchronized int getQueueSize() {
        return tasksByUser.values().stream().mapToInt(e -> e.currentlyRunningByUser.get()).sum();
    }

    public void stop() {
        LOG.debug("Stopping executors ...");
        List<Runnable> runnables = executor.shutdownNow();
        for (Runnable runnable : runnables) {
            if (runnable instanceof UpstreamTaskFuture) {
                UpstreamTaskFuture task = (UpstreamTaskFuture) runnable;
                LOG.debug("Queued: {}", task.getTask().getHttpRequest().getUrl());
            }
        }

        for (UserTaskQueue userTaskQueue : tasksByUser.values()) {
            for (UpstreamTaskFuture task : userTaskQueue.tasks) {
                LOG.debug("Queued: {}", task.getTask().getHttpRequest().getUrl());
            }
        }

        try {
            boolean terminated = executor.awaitTermination(1, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            LOG.debug("Skip interruption");
        }
    }

    public ThreadPoolExecutor getExecutor() {
        return executor;
    }

    private synchronized void tryScheduleNext() {
        if (currentlyRunning.get() >= maxTotal) {
            return;
        }

        Iterator<String> usersQueueIterator = userQueue.iterator();
        while (usersQueueIterator.hasNext()) {
            String login = usersQueueIterator.next();
            UserTaskQueue userTaskQueue = tasksByUser.get(login);
            if (userTaskQueue.currentlyRunningByUser.get() >= maxByUser) {
                continue;
            }

            UpstreamTaskFuture task = userTaskQueue.tasks.pollFirst();
            if (task == null) {
                continue;
            }

            usersQueueIterator.remove();

            userTaskQueue.currentlyRunningByUser.incrementAndGet();
            currentlyRunning.incrementAndGet();
            try {
                executor.execute(() -> {
                    try {
                        task.run();
                    } finally {
                        userTaskQueue.currentlyRunningByUser.decrementAndGet();
                        currentlyRunning.decrementAndGet();
                        tryScheduleNext();
                    }
                });
            } catch (Exception e) {
                userTaskQueue.currentlyRunningByUser.decrementAndGet();
                currentlyRunning.decrementAndGet();
                throw e;
            }
            return;
        }
    }

    private static class UserTaskQueue {
        private final ArrayDeque<UpstreamTaskFuture> tasks = new ArrayDeque<>();
        private final AtomicInteger currentlyRunningByUser = new AtomicInteger();
    }

    private static class UpstreamTaskFuture extends FutureTask<HttpResponse> {
        private final UpstreamTask task;

        public UpstreamTaskFuture(UpstreamTask task) {
            super(task);
            this.task = task;
        }

        public UpstreamTask getTask() {
            return task;
        }
    }
}
