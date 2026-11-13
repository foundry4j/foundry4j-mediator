package io.github.foundry4j.mediator;

/// Handles one request type and returns its response.
///
/// Exactly one handler may be registered per request type; duplicate
/// registrations are rejected when the mediator is built.
///
/// @param <Q> the request type handled by this instance
/// @param <R> the response type produced by this instance
@FunctionalInterface
public interface RequestHandler<Q extends Request<R>, R> {

    /// Handles a request.
    ///
    /// @param request the request to handle, never `null`
    /// @return the response, possibly `null` when the request declares
    ///         [Void] as its response type
    R handle(Q request);
}
