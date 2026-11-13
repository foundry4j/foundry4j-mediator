package io.github.foundry4j.mediator;

import org.junit.jupiter.api.Test;

import static io.github.foundry4j.mediator.TestMessages.Archive;
import static io.github.foundry4j.mediator.TestMessages.BaseRequest;
import static io.github.foundry4j.mediator.TestMessages.ChildOfParentQuery;
import static io.github.foundry4j.mediator.TestMessages.ChildRequest;
import static io.github.foundry4j.mediator.TestMessages.CountQuery;
import static io.github.foundry4j.mediator.TestMessages.HybridRequest;
import static io.github.foundry4j.mediator.TestMessages.PagedUsers;
import static io.github.foundry4j.mediator.TestMessages.Ping;
import static io.github.foundry4j.mediator.TestMessages.Query;
import static io.github.foundry4j.mediator.TestMessages.Unhandled;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/// Tests for request dispatch and handler resolution.
class SendTest {

    @Test
    void dispatchesToExactHandler() {
        Mediator mediator = Mediators.builder()
                .requestHandler(Ping.class, Ping::value)
                .build();

        assertEquals("hello", mediator.send(new Ping("hello")));
    }

    @Test
    void dispatchesToSuperclassHandler() {
        Mediator mediator = Mediators.builder()
                .requestHandler(BaseRequest.class, _ -> "base")
                .build();

        assertEquals("base", mediator.send(new ChildRequest()));
    }

    @Test
    void exactHandlerWinsOverSuperclassHandler() {
        Mediator mediator = Mediators.builder()
                .requestHandler(BaseRequest.class, _ -> "base")
                .requestHandler(ChildRequest.class, _ -> "child")
                .build();

        assertEquals("child", mediator.send(new ChildRequest()));
    }

    @Test
    void dispatchesToInterfaceHandler() {
        Mediator mediator = Mediators.builder()
                .requestHandler(Query.class, _ -> "query")
                .build();

        assertEquals("query", mediator.send(new CountQuery()));
    }

    @Test
    void superclassHandlerWinsOverInterfaceHandler() {
        Mediator mediator = Mediators.builder()
                .requestHandler(Query.class, _ -> "query")
                .requestHandler(BaseRequest.class, _ -> "base")
                .build();

        assertEquals("base", mediator.send(new HybridRequest()));
    }

    @Test
    void resolvesHandlerThroughInterfaceHierarchy() {
        Mediator mediator = Mediators.builder()
                .requestHandler(Query.class, _ -> "query")
                .build();

        assertEquals("query", mediator.send(new PagedUsers()));
    }

    @Test
    void resolvesInterfaceImplementedBySuperclass() {
        Mediator mediator = Mediators.builder()
                .requestHandler(Query.class, _ -> "query")
                .build();

        assertEquals("query", mediator.send(new ChildOfParentQuery()));
    }

    @Test
    void missingHandlerThrows() {
        Mediator mediator = Mediators.create();

        HandlerNotFoundException error = assertThrows(HandlerNotFoundException.class,
                () -> mediator.send(new Unhandled()));

        assertTrue(error.getMessage().contains(Unhandled.class.getName()));
    }

    @Test
    void nullRequestThrows() {
        Mediator mediator = Mediators.create();

        assertThrows(NullPointerException.class, () -> mediator.send(null));
    }

    @Test
    void handlerExceptionPropagatesUnchanged() {
        IllegalStateException boom = new IllegalStateException("boom");
        Mediator mediator = Mediators.builder()
                .requestHandler(Ping.class, _ -> {
                    throw boom;
                })
                .build();

        assertSame(boom, assertThrows(IllegalStateException.class,
                () -> mediator.send(new Ping("x"))));
    }

    @Test
    void handlerMayReturnNull() {
        Mediator mediator = Mediators.builder()
                .requestHandler(Ping.class, _ -> null)
                .build();

        assertNull(mediator.send(new Ping("x")));
    }

    @Test
    void voidRequestUsesNullResponse() {
        Mediator mediator = Mediators.builder()
                .requestHandler(Archive.class, _ -> null)
                .build();

        assertNull(mediator.send(new Archive(7L)));
    }
}
