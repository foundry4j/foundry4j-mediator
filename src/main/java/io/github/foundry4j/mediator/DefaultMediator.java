package io.github.foundry4j.mediator;

import java.util.Objects;

/// Default immutable [Mediator] implementation.
///
/// Dispatch delegates to two immutable registries, each of which caches the
/// resolution result per concrete message class.
final class DefaultMediator implements Mediator {

    private final RequestRegistry requests;
    private final NotificationRegistry notifications;

    DefaultMediator(RequestRegistry requests, NotificationRegistry notifications) {
        this.requests = requests;
        this.notifications = notifications;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <R> R send(Request<R> request) {
        Objects.requireNonNull(request, "request must not be null");
        return (R) requests.dispatch(request);
    }

    @Override
    public void publish(Notification notification) {
        Objects.requireNonNull(notification, "notification must not be null");
        notifications.publish(notification);
    }
}
