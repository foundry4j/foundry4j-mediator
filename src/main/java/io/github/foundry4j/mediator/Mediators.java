package io.github.foundry4j.mediator;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/// Entry point for creating [Mediator] instances.
///
/// ```java
/// // An empty mediator: every send fails, every publish is a no-op.
/// Mediator empty = Mediators.create();
///
/// // A configured mediator
/// Mediator mediator = Mediators.builder()
///         .requestHandler(GetUser.class, request -> repository.find(request.id()))
///         .notificationHandler(UserCreated.class, event -> mailer.sendWelcome(event.id()))
///         .build();
/// ```
public final class Mediators {

    private Mediators() {
    }

    /// Creates a mediator without any handlers.
    ///
    /// @return an empty, immutable mediator
    public static Mediator create() {
        return builder().build();
    }

    /// @return a new builder
    public static Builder builder() {
        return new Builder();
    }

    /// Builder for [Mediator] instances.
    ///
    /// Registration order matters for notification handlers and pipeline
    /// behaviors. The builder is not thread-safe and should be confined to one
    /// thread. [#build()] snapshots the current registrations, so a builder
    /// may be reused to create further mediators.
    public static final class Builder {

        private final Map<Class<?>, RequestRegistry.UntypedRequestHandler> requestHandlers = new LinkedHashMap<>();
        private final List<NotificationRegistry.HandlerEntry> notificationHandlers = new ArrayList<>();
        private final List<RequestRegistry.BehaviorEntry> behaviors = new ArrayList<>();

        private Builder() {
        }

        /// Registers the handler for a request type.
        ///
        /// Exactly one handler may be registered per request type; a second
        /// registration for the same class is rejected by throwing
        /// [MediatorConfigurationException].
        ///
        /// @param requestType the request class handled by `handler`
        /// @param handler     the handler invoked by [Mediator#send(Request)]
        /// @param <R>         the response type
        /// @param <Q>         the request type
        /// @return this builder
        /// @throws NullPointerException           if any argument is `null`
        /// @throws MediatorConfigurationException if a handler is already
        ///                                        registered for `requestType`
        public <R, Q extends Request<R>> Builder requestHandler(
                Class<Q> requestType, RequestHandler<Q, R> handler) {
            Objects.requireNonNull(requestType, "requestType must not be null");
            Objects.requireNonNull(handler, "handler must not be null");

            @SuppressWarnings("unchecked")
            RequestRegistry.UntypedRequestHandler untyped = request -> handler.handle((Q) request);

            if (requestHandlers.putIfAbsent(requestType, untyped) != null) {
                throw new MediatorConfigurationException(
                        "Duplicate request handler for " + requestType.getName());
            }
            return this;
        }

        /// Registers a notification handler.
        ///
        /// Any number of handlers may be registered for the same notification
        /// type; they are invoked in registration order by
        /// [Mediator#publish(Notification)].
        ///
        /// @param notificationType the notification type handled by `handler`
        /// @param handler          the handler invoked by
        ///                         [Mediator#publish(Notification)]
        /// @param <N>              the notification type
        /// @return this builder
        /// @throws NullPointerException if any argument is `null`
        public <N extends Notification> Builder notificationHandler(
                Class<N> notificationType, NotificationHandler<N> handler) {
            Objects.requireNonNull(notificationType, "notificationType must not be null");
            Objects.requireNonNull(handler, "handler must not be null");

            @SuppressWarnings("unchecked")
            NotificationRegistry.UntypedNotificationHandler untyped =
                    notification -> handler.handle((N) notification);

            notificationHandlers.add(new NotificationRegistry.HandlerEntry(notificationType, untyped));
            return this;
        }

        /// Registers a pipeline behavior for a request type.
        ///
        /// The behavior applies to the registered type and every subtype of
        /// it, including requests that implement it as an interface. Several
        /// behaviors may be registered; the first registered one becomes the
        /// outermost stage of the pipeline.
        ///
        /// @param requestType the request type the behavior applies to
        /// @param behavior    the middleware
        /// @param <R>         the response type
        /// @param <Q>         the request type
        /// @return this builder
        /// @throws NullPointerException if any argument is `null`
        public <R, Q extends Request<R>> Builder behavior(
                Class<Q> requestType, PipelineBehavior<Q, R> behavior) {
            Objects.requireNonNull(requestType, "requestType must not be null");
            Objects.requireNonNull(behavior, "behavior must not be null");

            @SuppressWarnings("unchecked")
            RequestRegistry.UntypedBehavior untyped = (request, next) -> behavior.handle(
                    (Q) request,
                    typedRequest -> (R) next.handle(typedRequest));

            behaviors.add(new RequestRegistry.BehaviorEntry(requestType, untyped));
            return this;
        }

        /// Builds an immutable mediator from the current registrations.
        ///
        /// Later changes to this builder do not affect the returned mediator.
        ///
        /// @return a new immutable mediator
        public Mediator build() {
            return new DefaultMediator(
                    new RequestRegistry(requestHandlers, behaviors),
                    new NotificationRegistry(notificationHandlers));
        }
    }
}
