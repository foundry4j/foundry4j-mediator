package io.github.foundry4j.mediator;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/// Immutable request-handler registry with a per-class dispatch cache.
///
/// Registrations are copied at construction time. The first dispatch of a
/// concrete request class resolves the handler and composes the pipeline
/// behaviors; the result is cached in a [ClassValue], so later dispatches never
/// walk the type hierarchy and never allocate.
final class RequestRegistry {

    private final Map<Class<?>, UntypedRequestHandler> handlers;
    private final List<BehaviorEntry> behaviors;

    private final ClassValue<RequestDispatcher> dispatchers = new ClassValue<>() {
        @Override
        protected RequestDispatcher computeValue(Class<?> requestType) {
            return createDispatcher(requestType);
        }
    };

    RequestRegistry(Map<Class<?>, UntypedRequestHandler> handlers,
                    List<BehaviorEntry> behaviors) {
        this.handlers = Map.copyOf(handlers);
        this.behaviors = List.copyOf(behaviors);
    }

    /// Dispatches a request through its cached handler pipeline.
    ///
    /// @param request the request instance, never `null`
    /// @return the response produced by the request handler
    /// @throws HandlerNotFoundException if no handler matches the runtime type
    Object dispatch(Object request) {
        return dispatchers.get(request.getClass()).dispatch(request);
    }

    private RequestDispatcher createDispatcher(Class<?> requestType) {
        Class<?> handlerType = resolveHandlerType(requestType);
        if (handlerType == null) {
            throw new HandlerNotFoundException(requestType);
        }

        UntypedRequestHandler pipeline = handlers.get(handlerType);

        // Wrap from the inside out so that the first registered behavior is
        // the outermost stage and therefore runs first.
        for (int i = behaviors.size() - 1; i >= 0; i--) {
            BehaviorEntry entry = behaviors.get(i);
            if (entry.type().isAssignableFrom(requestType)) {
                UntypedRequestHandler next = pipeline;
                UntypedBehavior behavior = entry.behavior();
                pipeline = request -> behavior.handle(request, next);
            }
        }

        return new RequestDispatcher(pipeline);
    }

    /// Resolves the registered handler type: exact class first, then the
    /// nearest superclass, then the nearest implemented interface.
    private Class<?> resolveHandlerType(Class<?> requestType) {
        Deque<Class<?>> interfaces = new ArrayDeque<>();
        Class<?> current = requestType;

        while (current != null && current != Object.class) {
            if (handlers.containsKey(current)) {
                return current;
            }
            interfaces.addAll(Arrays.asList(current.getInterfaces()));
            current = current.getSuperclass();
        }

        while (!interfaces.isEmpty()) {
            Class<?> candidate = interfaces.poll();
            if (handlers.containsKey(candidate)) {
                return candidate;
            }
            interfaces.addAll(Arrays.asList(candidate.getInterfaces()));
        }

        return null;
    }

    /// A type-erased request handler.
    @FunctionalInterface
    interface UntypedRequestHandler {

        /// @param request the request instance
        /// @return the response
        Object handle(Object request);
    }

    /// A type-erased pipeline behavior.
    @FunctionalInterface
    interface UntypedBehavior {

        /// @param request the request instance
        /// @param next    the remaining pipeline
        /// @return the response
        Object handle(Object request, UntypedRequestHandler next);
    }

    /// A pipeline behavior together with the type it was registered for.
    ///
    /// @param type     the registered request type
    /// @param behavior the middleware
    record BehaviorEntry(Class<?> type, UntypedBehavior behavior) {
    }

    /// A resolved handler pipeline for one concrete request class.
    private record RequestDispatcher(UntypedRequestHandler pipeline) {

        private Object dispatch(Object request) {
            return pipeline.handle(request);
        }
    }
}
