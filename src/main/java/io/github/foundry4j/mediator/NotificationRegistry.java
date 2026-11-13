package io.github.foundry4j.mediator;

import java.util.ArrayList;
import java.util.List;

/// Immutable notification-handler registry with a per-class dispatch cache.
///
/// Registrations are copied at construction time. The first publish of a
/// concrete notification class resolves all matching handlers into an array;
/// later publishes only iterate that array.
final class NotificationRegistry {

    private final List<HandlerEntry> handlers;

    private final ClassValue<NotificationDispatcher> dispatchers = new ClassValue<>() {
        @Override
        protected NotificationDispatcher computeValue(Class<?> notificationType) {
            return createDispatcher(notificationType);
        }
    };

    NotificationRegistry(List<HandlerEntry> handlers) {
        this.handlers = List.copyOf(handlers);
    }

    /// Publishes a notification to every matching handler in registration
    /// order.
    ///
    /// @param notification the notification instance, never `null`
    void publish(Object notification) {
        dispatchers.get(notification.getClass()).publish(notification);
    }

    private NotificationDispatcher createDispatcher(Class<?> notificationType) {
        List<UntypedNotificationHandler> matched = new ArrayList<>();
        for (HandlerEntry entry : handlers) {
            if (entry.type().isAssignableFrom(notificationType)) {
                matched.add(entry.handler());
            }
        }
        if (matched.isEmpty()) {
            return NotificationDispatcher.EMPTY;
        }
        return new NotificationDispatcher(matched.toArray(UntypedNotificationHandler[]::new));
    }

    /// A type-erased notification handler.
    @FunctionalInterface
    interface UntypedNotificationHandler {

        /// @param notification the notification instance
        void handle(Object notification);
    }

    /// A notification handler together with the type it was registered for.
    ///
    /// @param type    the registered notification type
    /// @param handler the handler
    record HandlerEntry(Class<?> type, UntypedNotificationHandler handler) {
    }

    /// The resolved handler array for one concrete notification class.
    private record NotificationDispatcher(UntypedNotificationHandler[] handlers) {

        private static final NotificationDispatcher EMPTY =
                new NotificationDispatcher(new UntypedNotificationHandler[0]);

        private void publish(Object notification) {
            for (UntypedNotificationHandler handler : handlers) {
                handler.handle(notification);
            }
        }
    }
}
