package io.github.foundry4j.mediator;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import static io.github.foundry4j.mediator.TestMessages.Event;
import static io.github.foundry4j.mediator.TestMessages.Ping;
import static org.junit.jupiter.api.Assertions.assertEquals;

/// Tests that the immutable mediator and its dispatch caches behave correctly
/// under concurrent access.
class ConcurrencyTest {

    @Test
    void concurrentSendsReturnTheirOwnResponse() throws Exception {
        Mediator mediator = Mediators.builder()
                .requestHandler(Ping.class, Ping::value)
                .build();

        int tasks = 1_000;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<String>> futures = IntStream.range(0, tasks)
                    .mapToObj(i -> executor.submit(() -> mediator.send(new Ping("v" + i))))
                    .toList();

            for (int i = 0; i < tasks; i++) {
                assertEquals("v" + i, futures.get(i).get());
            }
        }
    }

    @Test
    void concurrentPublishesInvokeEveryHandler() throws Exception {
        AtomicInteger counter = new AtomicInteger();

        Mediator mediator = Mediators.builder()
                .notificationHandler(Event.class, _ -> counter.incrementAndGet())
                .notificationHandler(Event.class, _ -> counter.incrementAndGet())
                .build();

        int tasks = 500;

        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var futures = IntStream.range(0, tasks)
                    .mapToObj(_ -> executor.submit(() -> mediator.publish(new Event("x"))))
                    .toList();

            for (Future<?> future : futures) {
                future.get();
            }
        }

        assertEquals(tasks * 2, counter.get());
    }
}
