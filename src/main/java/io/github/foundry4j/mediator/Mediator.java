package io.github.foundry4j.mediator;

/// Central dispatcher for requests and notifications.
///
/// A mediator is created through [Mediators#builder()] and is immutable once
/// built: the handler set cannot change, so dispatch is lock-free and safe for
/// concurrent use from any number of threads.
///
/// ```java
/// Mediator mediator = Mediators.builder()
///         .requestHandler(GetUser.class, request -> repository.find(request.id()))
///         .notificationHandler(UserCreated.class, event -> mailer.sendWelcome(event.id()))
///         .build();
///
/// User user = mediator.send(new GetUser(42));
/// mediator.publish(new UserCreated(user.id()));
/// ```
///
/// ## Dispatch semantics
///
/// A request is dispatched to exactly one [RequestHandler], resolved from the
/// runtime class of the request: the exact class wins, then the nearest
/// superclass, then the nearest implemented interface. The resolution result is
/// cached per request class, so steady-state sends do not walk the hierarchy.
///
/// A notification is dispatched to every matching [NotificationHandler] in
/// registration order. Handlers registered for a supertype or interface also
/// receive subtype notifications.
///
/// ## Thread safety
///
/// Implementations are immutable and thread-safe. Handlers may run
/// concurrently and must therefore be thread-safe themselves. Exceptions
/// thrown by handlers propagate unchanged to the caller.
public interface Mediator {

    /// Sends a request to its handler and returns the response.
    ///
    /// The handler is resolved from `request.getClass()`: an exact match is
    /// preferred, then the nearest superclass, then the nearest implemented
    /// interface.
    ///
    /// @param request the request to dispatch, must not be `null`
    /// @param <R>     the response type
    /// @return the handler's response, possibly `null` when the request
    ///         declares [Void] as its response type
    /// @throws NullPointerException     if `request` is `null`
    /// @throws HandlerNotFoundException if no handler matches the request type
    ///                                  or any of its supertypes
    <R> R send(Request<R> request);

    /// Publishes a notification to every matching handler.
    ///
    /// Handlers run synchronously on the calling thread in registration order.
    /// If one throws, the exception propagates immediately and the remaining
    /// handlers are skipped. When no handler matches, this method does nothing.
    ///
    /// @param notification the notification to publish, must not be `null`
    /// @throws NullPointerException if `notification` is `null`
    void publish(Notification notification);
}
