package io.github.foundry4j.mediator;

import java.io.Serial;

/// Thrown when a [Mediators.Builder] is configured inconsistently, for example
/// when two handlers are registered for the same request type.
public final class MediatorConfigurationException extends MediatorException {

    /// Serialization identifier.
    @Serial
    private static final long serialVersionUID = 1L;

    /// Creates the exception with a detail message.
    ///
    /// @param message human-readable description of the misconfiguration
    public MediatorConfigurationException(String message) {
        super(message);
    }
}
