package io.github.foundry4j.mediator;

import org.junit.jupiter.api.Test;

import static io.github.foundry4j.mediator.TestMessages.Echo;
import static io.github.foundry4j.mediator.TestMessages.Event;
import static io.github.foundry4j.mediator.TestMessages.Ping;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for the mediator factory, its builder and its validation rules.
class MediatorsTest {

    @Test
    void createReturnsUsableEmptyMediator() {
        Mediator mediator = Mediators.create();

        assertNotNull(mediator);
        assertThrows(HandlerNotFoundException.class, () -> mediator.send(new Ping("x")));
        mediator.publish(new Event("x"));
    }

    @Test
    void duplicateRequestHandlerIsRejected() {
        Mediators.Builder builder = Mediators.builder()
                .requestHandler(Ping.class, Ping::value);

        MediatorConfigurationException error = assertThrows(MediatorConfigurationException.class,
                () -> builder.requestHandler(Ping.class, _ -> "other"));

        assertTrue(error.getMessage().contains(Ping.class.getName()));
    }

    @Test
    void duplicateNotificationHandlersAreAllowed() {
        Mediator mediator = Mediators.builder()
                .notificationHandler(Event.class, _ -> {
                })
                .notificationHandler(Event.class, _ -> {
                })
                .build();

        mediator.publish(new Event("x"));
    }

    @Test
    void multipleBehaviorsAreAllowed() {
        Mediator mediator = Mediators.builder()
                .behavior(Ping.class, (request, next) -> next.handle(request))
                .behavior(Ping.class, (request, next) -> next.handle(request))
                .requestHandler(Ping.class, Ping::value)
                .build();

        assertEquals("x", mediator.send(new Ping("x")));
    }

    @Test
    void builderSnapshotsRegistrations() {
        Mediators.Builder builder = Mediators.builder()
                .requestHandler(Ping.class, Ping::value);

        Mediator first = builder.build();
        builder.requestHandler(Echo.class, Echo::value);
        Mediator second = builder.build();

        assertEquals("x", first.send(new Ping("x")));
        assertThrows(HandlerNotFoundException.class, () -> first.send(new Echo(1)));
        assertEquals(1, second.send(new Echo(1)));
    }

    @Test
    void rejectsNullRequestType() {
        Mediators.Builder builder = Mediators.builder();

        assertThrows(NullPointerException.class,
                () -> builder.requestHandler(null, Ping::value));
    }

    @Test
    void rejectsNullRequestHandler() {
        Mediators.Builder builder = Mediators.builder();

        assertThrows(NullPointerException.class,
                () -> builder.requestHandler(Ping.class, null));
    }

    @Test
    void rejectsNullNotificationType() {
        Mediators.Builder builder = Mediators.builder();

        assertThrows(NullPointerException.class,
                () -> builder.notificationHandler((Class<Event>) null, _ -> {
                }));
    }

    @Test
    void rejectsNullNotificationHandler() {
        Mediators.Builder builder = Mediators.builder();

        assertThrows(NullPointerException.class,
                () -> builder.notificationHandler(Event.class, null));
    }

    @Test
    void rejectsNullBehaviorType() {
        Mediators.Builder builder = Mediators.builder();

        assertThrows(NullPointerException.class,
                () -> builder.behavior((Class<Ping>) null, (_, next) -> next.handle(null)));
    }

    @Test
    void rejectsNullBehavior() {
        Mediators.Builder builder = Mediators.builder();

        assertThrows(NullPointerException.class,
                () -> builder.behavior(Ping.class, null));
    }

    @Test
    void baseExceptionCarriesMessageAndCause() {
        IllegalStateException cause = new IllegalStateException("boom");
        MediatorException error = new MediatorException("wrapped", cause);

        assertEquals("wrapped", error.getMessage());
        assertSame(cause, error.getCause());
        assertEquals("plain", new MediatorException("plain").getMessage());
    }
}
