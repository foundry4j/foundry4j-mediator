package io.github.foundry4j.mediator;

/// Middleware wrapped around request handling, such as validation, logging,
/// metrics or transaction management.
///
/// A behavior receives the request and the rest of the pipeline. It must call
/// [#handle(Request, RequestHandler)] on `next` to continue; returning without
/// calling `next` short-circuits the pipeline and the request handler is not
/// invoked.
///
/// ```java
/// Mediator mediator = Mediators.builder()
///         .behavior(Query.class, (request, next) -> {
///             long start = System.nanoTime();
///             try {
///                 return next.handle(request);
///             } finally {
///                 record(System.nanoTime() - start);
///             }
///         })
///         .requestHandler(CountUsers.class, _ -> 42L)
///         .build();
/// ```
///
/// Behaviors run in registration order: the first registered behavior is the
/// outermost one and therefore runs first before the request handler and last
/// when the response unwinds.
///
/// @param <Q> the request type this behavior applies to
/// @param <R> the response type produced by the pipeline
@FunctionalInterface
public interface PipelineBehavior<Q extends Request<R>, R> {

    /// Handles a request, optionally delegating to the remaining pipeline.
    ///
    /// @param request the request being dispatched, never `null`
    /// @param next    the remaining pipeline, never `null`
    /// @return the response produced by the remaining pipeline or by this
    ///         behavior when it short-circuits
    R handle(Q request, RequestHandler<Q, R> next);
}
