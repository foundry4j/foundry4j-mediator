package io.github.foundry4j.mediator;

import java.io.Serial;

/// Thrown when [Mediator#send(Request)] cannot find a handler for the runtime
/// class of a request.
///
/// The request class and every one of its supertypes are checked; this error
/// means none of them was registered.
public final class HandlerNotFoundException extends MediatorException {

    /// Serialization identifier.
    @Serial
    private static final long serialVersionUID = 1L;

    /// Creates the exception for a request type that has no handler.
    ///
    /// @param requestType the runtime class of the undispatchable request
    public HandlerNotFoundException(Class<?> requestType) {
        super("No request handler registered for " + requestType.getName()
                + " or any of its supertypes");
    }
}
