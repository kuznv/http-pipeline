package com.payneteasy.http.pipeline.upstream;

import com.payneteasy.http.pipeline.client.HttpResponse;
import com.payneteasy.http.pipeline.upstream.UpstreamExecutorTest.User.Task;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class UpstreamExecutorTest {
    private static final Logger LOG = LoggerFactory.getLogger(UpstreamExecutorTest.class);

    @Test
    public void testMaxByUser() throws Exception {
        UpstreamExecutor executor = new UpstreamExecutor(2, 3);

        User user1 = new User("user1", executor);

        Task task1 = user1.run();
        Task task2 = user1.run();
        Task task3 = user1.run();

        task3.latch.countDown();

        assertFalse(task3.isFinished(1, TimeUnit.SECONDS));

        task1.latch.countDown();
        task2.latch.countDown();

        assertTrue(task1.isFinished(1, TimeUnit.SECONDS));
        assertTrue(task2.isFinished(1, TimeUnit.SECONDS));
        assertTrue(task3.isFinished(1, TimeUnit.SECONDS));
    }

    @Test
    public void testMaxTotal() throws Exception {
        UpstreamExecutor executor = new UpstreamExecutor(2, 3);

        User user1 = new User("user1", executor);
        User user2 = new User("user2", executor);
        User user3 = new User("user3", executor);
        User user4 = new User("user4", executor);

        Task task1 = user1.run();
        Task task2 = user2.run();
        Task task3 = user3.run();
        Task task4 = user4.run();

        task4.latch.countDown();

        assertFalse(task4.isFinished(1, TimeUnit.SECONDS));

        task1.latch.countDown();
        task2.latch.countDown();
        task3.latch.countDown();

        assertTrue(task1.isFinished(1, TimeUnit.SECONDS));
        assertTrue(task2.isFinished(1, TimeUnit.SECONDS));
        assertTrue(task3.isFinished(1, TimeUnit.SECONDS));
        assertTrue(task4.isFinished(1, TimeUnit.SECONDS));
    }

    @Test
    public void testUserQueue() throws Exception {
        UpstreamExecutor executor = new UpstreamExecutor(1, 3);

        User user1 = new User("user1", executor);
        User user2 = new User("user2", executor);

        Task task1 = user1.run();
        Task task2 = user1.run();
        Task task3 = user1.run();
        Task task4 = user2.run();

        task4.latch.countDown();

        assertTrue(task4.isFinished(1, TimeUnit.SECONDS));
    }

    @Test
    public void testTaskQueue() throws Exception {
        UpstreamExecutor executor = new UpstreamExecutor(1, 3);

        List<Task> tasks = IntStream.rangeClosed(1, 10)
                .mapToObj(i -> new User("user" + i, executor).run())
                .collect(Collectors.toList());

        for (Task task : tasks) {
            task.latch.countDown();
        }

        for (Task t : tasks) {
            assertTrue(t.isFinished(1, TimeUnit.SECONDS));
        }
    }

    static class User {
        private final String login;
        private final UpstreamExecutor executor;
        private final AtomicInteger taskCount = new AtomicInteger(1);

        User(String login, UpstreamExecutor executor) {
            this.login = login;
            this.executor = executor;
        }

        Task run() {
            int taskId = taskCount.getAndIncrement();

            CountDownLatch latch = new CountDownLatch(1);

            Future<?> future = executor.submit(new UpstreamTask(String.valueOf(taskId), null, null, login) {
                @Override
                public HttpResponse call() throws InterruptedException {
                    LOG.debug("START " + login + " task" + taskId);
                    latch.await();
                    LOG.debug("END " + login + " task" + taskId);
                    return null;
                }
            });

            return new Task(future, latch);
        }

        static class Task {
            private final Future<?> future;
            private final CountDownLatch latch;

            public Task(Future<?> future, CountDownLatch latch) {
                this.future = future;
                this.latch = latch;
            }

            public boolean isFinished(int timeout, TimeUnit unit) throws ExecutionException, InterruptedException {
                try {
                    future.get(timeout, unit);
                    return true;
                } catch (TimeoutException e) {
                    return false;
                }
            }
        }
    }
}
