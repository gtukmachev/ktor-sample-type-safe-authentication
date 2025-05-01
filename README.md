# ktor-sample-type-safe-authentication

This projects illustrates the problem, described in StackOverflow question:

## See the code: 

See the kotlin/Application.kt file:
* :white_check_mark: the route `/private` works fine;
* :x: the route  `/private-2` has the error - **principal** is always null.
 

## Run test to see the problem:
```shell
./gradlew test --info
```

# Problem: Ktor: Principal resolution fails when encapsulating authenticate in a custom Route extension

## What I Want to Achieve

I’m building a type-safe wrapper around Ktor’s Authentication plugin to avoid mismatched `principal` types during refactoring. The goal is to enforce consistency between:

1. The authentication method (e.g., `authenticate("my-config")` )
2. The principal type (e.g., `call.principal<UserPrincipal>()` )

## Desired syntax:
```kotlin
routing {
    requiredAuth { // <-- Ensures principal type matches auth config
        get("/secure") {
            val user = call.authenticatedUser() // <-- Returns UserPrincipal (not nullable!)
            call.respond("Hello, ${user.name}")
        }
    }
}
```

## Current Progress (and Where It Breaks)
### Step 1: Basic Scope with Principal Access
This works when used directly:
```kotlin
class AuthTypeSafeScope {
    fun ApplicationCall.authenticatedUser() = principal<UserPrincipal>() 
        ?: error("Not authenticated")
}
```

### :white_check_mark: Step 2: Manual Usage (Works)
```kotlin
authenticate("user-header") {
    AuthTypeSafeScope().apply {
        get("/test") {
            val user = call.authenticatedUser() // Successfully resolves principal
            call.respond(user.name)
        }
    }
}
```

### :x: Step 3: Encapsulated in Extension (Fails)
```kotlin
inline fun Route.requiredAuth(crossinline block: AuthTypeSafeScope.() -> Unit) {
    authenticate("user-header") {
        AuthTypeSafeScope().apply(block) // <-- Principal resolves to NULL here
    }
}
```

## Symptoms:
* **Compiles** and **runs**, but `principal<UserPrincipal>()` always returns `null` inside `requiredAuth()`.
* **Works fine** if I inline the logic **(Step 2)**.

## Why :interrobang:
I suspect the `Route.()` vs. `AuthTypeSafeScope.()` scope handling breaks Ktor’s principal resolution, but:

1. Why does it work in **(Step)** 2 if the scopes are "wrong"?
2. How can I **properly encapsulate** this while preserving principal resolution?
