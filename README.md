# Foundry4j Mediator

<img src="docs/logo.svg" alt="Foundry4j Mediator logo" align="right" width="128" height="128" />

A zero-dependency, high-performance in-process mediator for Java 27, with
typed request/response dispatch, notification fan-out and composable
pipeline behaviors.

![Java 27+](https://img.shields.io/badge/Java-27%2B-ED8B00?logo=openjdk&logoColor=white)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.foundry4j/foundry4j-mediator?logo=apachemaven&color=blue)](https://central.sonatype.com/artifact/io.github.foundry4j/foundry4j-mediator)
[![Coverage](https://sonarcloud.io/api/project_badges/measure?project=foundry4j_foundry4j-mediator&metric=coverage)](https://sonarcloud.io/summary/new_code?id=foundry4j_foundry4j-mediator)
[![CI](https://github.com/foundry4j/foundry4j-mediator/actions/workflows/ci.yml/badge.svg)](https://github.com/foundry4j/foundry4j-mediator/actions/workflows/ci.yml)

## Features

- **Zero runtime dependencies** — everything is built on the JDK, with a JPMS
  module descriptor (`io.github.foundry4j.mediator`).
- **Constant-time dispatch** — the handler for a request class is resolved once
  and cached in a `ClassValue`; steady-state sends never walk the type
  hierarchy.
- **Typed request/response** — `Request<R>` and `RequestHandler<Q, R>` keep the
  response type checked at compile time.
- **Supertype fallback** — a handler registered for a superclass or interface
  serves every subtype, so request families can share one handler.
- **Notification fan-out** — every matching handler is invoked in registration
  order; zero handlers is a no-op.
- **Composable pipeline behaviors** — validation, logging, metrics and
  transactions as middleware, with short-circuit support.
- **No reflection, no scanning** — handlers are registered explicitly, so there
  is no classpath scanning, no proxy generation and no startup cost.
- **Fail-fast semantics** — a missing handler raises
  `HandlerNotFoundException`; handler exceptions propagate unchanged.

## Requirements

- JDK 27 or later

## Installation

Gradle (Kotlin DSL):

```kotlin
dependencies {
    implementation("io.github.foundry4j:foundry4j-mediator:1.0.0")
}
```

The library has no transitive dependencies.

## Quick start

```java
import io.github.foundry4j.mediator.Mediator;
import io.github.foundry4j.mediator.Mediators;
import io.github.foundry4j.mediator.Notification;
import io.github.foundry4j.mediator.Request;

record GetUser(long id) implements Request<User> {
}

record UserCreated(long id) implements Notification {
}

void main() {
    Mediator mediator = Mediators.builder()
            .requestHandler(GetUser.class, request -> repository.find(request.id()))
            .notificationHandler(UserCreated.class, event -> mailer.sendWelcome(event.id()))
            .build();

    User user = mediator.send(new GetUser(42));
    mediator.publish(new UserCreated(user.id()));
}
```

The mediator is created once and is immutable afterwards: it can be shared
freely between threads, while handlers must be thread-safe themselves.

## Requests

A request is dispatched to exactly one handler. The handler type is resolved
from the runtime class of the request; the first match wins:

| Step | Lookup |
|------|--------|
| 1 | the exact runtime class |
| 2 | the nearest superclass |
| 3 | implemented interfaces, breadth-first through their hierarchy |

This makes it possible to register one handler for a whole request family:

```java
interface Query extends Request<Long> {
}

record CountUsers() implements Query {
}

Mediator mediator = Mediators.builder()
        .requestHandler(Query.class, _ -> 42L)
        .build();

Long count = mediator.send(new CountUsers());
```

A request that only performs side effects can declare `Void` as its response
type; its handler returns `null`:

```java
record ArchiveOrder(long id) implements Request<Void> {
}

Mediator mediator = Mediators.builder()
        .requestHandler(ArchiveOrder.class, request -> {
            repository.archive(request.id());
            return null;
        })
        .build();
```

Request handlers are registered per type. Registering a second handler for the
same class fails fast with `MediatorConfigurationException` at registration
time, rather than silently choosing one at runtime.

## Notifications

A notification is delivered to every handler whose registered type is the
notification class or one of its supertypes. Handlers run synchronously on the
calling thread in registration order:

```java
Mediator mediator = Mediators.builder()
        .notificationHandler(UserCreated.class, event -> mailer.sendWelcome(event.id()))
        .notificationHandler(Notification.class, event -> auditLog.record(event))
        .build();

mediator.publish(new UserCreated(42));
```

Delivery is fail-fast: if a handler throws, the exception propagates and the
remaining handlers are skipped. A notification with no handlers is a no-op.

## Pipeline behaviors

Behaviors wrap request handling with cross-cutting logic. A behavior receives
the request and the rest of the pipeline; call `next.handle(request)` to
continue, or return without calling it to short-circuit:

```java
Mediator mediator = Mediators.builder()
        .behavior(Query.class, (request, next) -> {
            long start = System.nanoTime();
            try {
                return next.handle(request);
            } finally {
                metrics.record(System.nanoTime() - start);
            }
        })
        .requestHandler(CountUsers.class, _ -> 42L)
        .build();
```

A behavior applies to the registered type and every subtype of it, including
implementations of an interface. Behaviors run in registration order: the first
registered behavior is the outermost stage, so it runs first before the handler
and last when the response unwinds.

## Registration rules

| Builder method | Duplicates | Ordering |
|----------------|------------|----------|
| `requestHandler(Class, RequestHandler)` | rejected with `MediatorConfigurationException` | not applicable |
| `notificationHandler(Class, NotificationHandler)` | allowed | registration order |
| `behavior(Class, PipelineBehavior)` | allowed | first registered is outermost |

`build()` snapshots the current registrations, so a builder can be reused to
create further mediators without affecting the ones already built.

## Performance

```text
  mediator.send(request)
        │
        ▼
  ClassValue cache ──────────────► RequestDispatcher
  (one entry per request class)          │
                                         ▼
                          behavior N ... behavior 1
                                         │
                                         ▼
                                  RequestHandler
```

- Dispatch resolution happens once per concrete request class and is cached in
  a `ClassValue`; steady-state sends are one cache lookup plus virtual calls.
- Pipeline behaviors are composed at cache time, so sending a request does not
  allocate pipeline stages.
- Notification dispatchers precompute the matching handler array per
  notification class; publishing is a single array loop.
- All registries are immutable after `build()`, which gives safe publication
  and lock-free reads on every dispatch.
- No reflection, no bytecode generation, no classpath scanning.

## Semantics

- **Missing handler** — `send` throws `HandlerNotFoundException` naming the
  request class when neither it nor any supertype has a handler.
- **Handler failures** — exceptions from request handlers, notification
  handlers and behaviors propagate unchanged; they are never wrapped.
- **Notification failures** — delivery is fail-fast, so an exception stops the
  remaining handlers of that notification.
- **Null arguments** — `send(null)` and `publish(null)` throw
  `NullPointerException`; registration rejects null types and handlers.
- **Thread safety** — a mediator is immutable and safe to share; handlers may
  be invoked concurrently and must be thread-safe.
