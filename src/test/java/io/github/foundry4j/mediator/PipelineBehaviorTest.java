package io.github.foundry4j.mediator;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static io.github.foundry4j.mediator.TestMessages.BaseRequest;
import static io.github.foundry4j.mediator.TestMessages.ChildRequest;
import static io.github.foundry4j.mediator.TestMessages.CountQuery;
import static io.github.foundry4j.mediator.TestMessages.Ping;
import static io.github.foundry4j.mediator.TestMessages.Query;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/// Tests for pipeline behavior composition and ordering.
class PipelineBehaviorTest {

    @Test
    void behaviorsWrapTheHandlerInRegistrationOrder() {
        List<String> log = new ArrayList<>();

        Mediator mediator = Mediators.builder()
                .behavior(Ping.class, (request, next) -> {
                    log.add("first-before");
                    String response = next.handle(request);
                    log.add("first-after");
                    return response;
                })
                .behavior(Ping.class, (request, next) -> {
                    log.add("second-before");
                    String response = next.handle(request);
                    log.add("second-after");
                    return response;
                })
                .requestHandler(Ping.class, request -> {
                    log.add("handler");
                    return request.value();
                })
                .build();

        assertEquals("x", mediator.send(new Ping("x")));
        assertEquals(List.of("first-before", "second-before", "handler",
                "second-after", "first-after"), log);
    }

    @Test
    void behaviorCanShortCircuit() {
        AtomicBoolean invoked = new AtomicBoolean();

        Mediator mediator = Mediators.builder()
                .behavior(Ping.class, (_, _) -> "blocked")
                .requestHandler(Ping.class, _ -> {
                    invoked.set(true);
                    return "handled";
                })
                .build();

        assertEquals("blocked", mediator.send(new Ping("x")));
        assertFalse(invoked.get());
    }

    @Test
    void behaviorRegisteredForInterfaceApplies() {
        Mediator mediator = Mediators.builder()
                .behavior(Query.class, (request, next) -> next.handle(request).toUpperCase())
                .requestHandler(CountQuery.class, _ -> "ok")
                .build();

        assertEquals("OK", mediator.send(new CountQuery()));
    }

    @Test
    void behaviorRegisteredForSuperclassApplies() {
        Mediator mediator = Mediators.builder()
                .behavior(BaseRequest.class, (request, next) -> "[" + next.handle(request) + "]")
                .requestHandler(ChildRequest.class, _ -> "child")
                .build();

        assertEquals("[child]", mediator.send(new ChildRequest()));
    }

    @Test
    void unrelatedBehaviorIsNotApplied() {
        Mediator mediator = Mediators.builder()
                .behavior(CountQuery.class, (_, _) -> "blocked")
                .requestHandler(Ping.class, Ping::value)
                .build();

        assertEquals("value", mediator.send(new Ping("value")));
    }

    @Test
    void behaviorExceptionPropagatesUnchanged() {
        IllegalStateException boom = new IllegalStateException("boom");

        Mediator mediator = Mediators.builder()
                .behavior(Ping.class, (_, _) -> {
                    throw boom;
                })
                .requestHandler(Ping.class, Ping::value)
                .build();

        assertSame(boom, assertThrows(IllegalStateException.class,
                () -> mediator.send(new Ping("x"))));
    }
}
