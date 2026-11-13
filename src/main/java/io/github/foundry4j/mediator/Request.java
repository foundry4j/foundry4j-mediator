package io.github.foundry4j.mediator;

/// A message that produces exactly one response of type `R`.
///
/// Requests are immutable value objects dispatched by the [Mediator] to the
/// single [RequestHandler] registered for their runtime type. When no handler
/// is registered for the exact class, the nearest superclass or implemented
/// interface is used instead, so handlers can be shared by a request family.
///
/// ```java
/// record GetUser(long id) implements Request<User> {}
/// ```
///
/// A request that only needs side effects can declare [Void] as its response
/// type and its handler can return `null`.
///
/// @param <R> the response type produced by the matching handler
public interface Request<R> {
}
