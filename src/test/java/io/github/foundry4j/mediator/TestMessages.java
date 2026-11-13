package io.github.foundry4j.mediator;

/// Shared message types used by the test suite.
final class TestMessages {

    private TestMessages() {
    }

    record Ping(String value) implements Request<String> {
    }

    record Echo(int value) implements Request<Integer> {
    }

    record Unhandled() implements Request<String> {
    }

    record Archive(long id) implements Request<Void> {
    }

    static class BaseRequest implements Request<String> {
    }

    static final class ChildRequest extends BaseRequest {
    }

    interface Query extends Request<String> {
    }

    static final class HybridRequest extends BaseRequest implements Query {
    }

    record CountQuery() implements Query {
    }

    interface PagedQuery extends Query {
    }

    record PagedUsers() implements PagedQuery {
    }

    static class ParentQuery implements Query {
    }

    static final class ChildOfParentQuery extends ParentQuery {
    }

    record Event(String name) implements Notification {
    }

    interface DomainEvent extends Notification {
    }

    record OrderPlaced(long id) implements DomainEvent {
    }

    record UserCreated(long id) implements Notification {
    }
}
