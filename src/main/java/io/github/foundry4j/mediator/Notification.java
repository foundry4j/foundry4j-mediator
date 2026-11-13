package io.github.foundry4j.mediator;

/// A message broadcast to every [NotificationHandler] that matches its runtime
/// type.
///
/// Unlike [Request], a notification has no response and may be handled by any
/// number of handlers, including zero. A handler registered for a supertype or
/// for an implemented interface also receives notifications of that subtype.
///
/// ```java
/// record UserCreated(long id) implements Notification {}
/// ```
public interface Notification {
}
