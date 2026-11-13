package io.github.foundry4j.mediator;

import java.io.Serial;

/// Base type for errors raised by the mediator itself.
///
/// Exceptions thrown by request or notification handlers are **not** wrapped:
/// they propagate unchanged so that callers keep their original exception
/// types.
public class MediatorException extends RuntimeException {

    /// Serialization identifier.
    @Serial
    private static final long serialVersionUID = 1L;

    /// Creates the exception with a detail message.
    ///
    /// @param message human-readable description of the failure
    public MediatorException(String message) {
        super(message);
    }

    /// Creates the exception with a detail message and a cause.
    ///
    /// @param message human-readable description of the failure
    /// @param cause   the underlying cause
    public MediatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
