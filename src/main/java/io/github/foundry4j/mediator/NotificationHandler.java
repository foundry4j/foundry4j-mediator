package io.github.foundry4j.mediator;

/// Handles notifications of one type.
///
/// Any number of handlers may be registered for the same notification type.
/// All matching handlers are invoked in registration order by
/// [Mediator#publish(Notification)].
///
/// @param <N> the notification type handled by this instance
@FunctionalInterface
public interface NotificationHandler<N extends Notification> {

    /// Handles a notification.
    ///
    /// @param notification the notification to handle, never `null`
    void handle(N notification);
}
