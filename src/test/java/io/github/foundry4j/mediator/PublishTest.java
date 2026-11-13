package io.github.foundry4j.mediator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static io.github.foundry4j.mediator.TestMessages.DomainEvent;
import static io.github.foundry4j.mediator.TestMessages.Event;
import static io.github.foundry4j.mediator.TestMessages.OrderPlaced;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for notification fan-out.
class PublishTest {

    @Test
    void invokesEveryHandlerInRegistrationOrder() {
        List<String> log = new ArrayList<>();

        Mediator mediator = Mediators.builder()
                .notificationHandler(Event.class, event -> log.add("first:" + event.name()))
                .notificationHandler(Event.class, event -> log.add("second:" + event.name()))
                .build();

        mediator.publish(new Event("x"));

        assertEquals(List.of("first:x", "second:x"), log);
    }

    @Test
    void supertypeHandlerAlsoReceivesSubtypeNotifications() {
        List<String> log = new ArrayList<>();

        Mediator mediator = Mediators.builder()
                .notificationHandler(Notification.class, _ -> log.add("any"))
                .notificationHandler(Event.class, _ -> log.add("event"))
                .build();

        mediator.publish(new Event("x"));

        assertEquals(List.of("any", "event"), log);
    }

    @Test
    void interfaceHandlerReceivesImplementations() {
        List<String> log = new ArrayList<>();

        Mediator mediator = Mediators.builder()
                .notificationHandler(DomainEvent.class, _ -> log.add("domain"))
                .build();

        mediator.publish(new OrderPlaced(42L));

        assertEquals(List.of("domain"), log);
    }

    @Test
    void missingHandlersAreANoOp() {
        Mediator mediator = Mediators.create();

        mediator.publish(new Event("x"));
    }

    @Test
    void nullNotificationThrows() {
        Mediator mediator = Mediators.create();

        assertThrows(NullPointerException.class, () -> mediator.publish(null));
    }

    @Test
    void failingHandlerStopsRemainingHandlers() {
        List<String> log = new ArrayList<>();

        Mediator mediator = Mediators.builder()
                .notificationHandler(Event.class, _ -> {
                    throw new IllegalStateException("boom");
                })
                .notificationHandler(Event.class, event -> log.add(event.name()))
                .build();

        assertThrows(IllegalStateException.class, () -> mediator.publish(new Event("x")));
        assertTrue(log.isEmpty());
    }
}
