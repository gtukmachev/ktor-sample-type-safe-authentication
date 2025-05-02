# ktor-sample-type-safe-authentication

This projects illustrates the problem, described in StackOverflow question: https://stackoverflow.com/questions/79601768/ktor-principal-resolution-fails-when-encapsulating-authenticate-in-a-custom-rou

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

----
I've tried to build just a simple custom Kotlin-builder DSL, where I manipulated by scopes like here ^^^, and I didn't experience any errors:
- all defined functions (methods) which are in the scope of any apper level - accessible and works good.

---

# Solution: Fixing Principal Resolution in Ktor Route Extensions
The key issue is that get("/secure") inside requiredAuth uses the wrong this scope. It's actually calling:
```kotlin
routing {
    requiredAuth {
        this@routing.get("/secure") { ... } // Uses parent route instead of auth scope
    }
}
```
This makes the route register outside the authenticate block, breaking the authentication context.

## The Solution
`AuthTypeSafeScope` must implement Ktor’s `Route` interface to ensure routes are registered in the correct scope.

After identifying the issue (incorrect this scope handling), I implemented this solution—though it’s slightly hacky since it involves embedding a new Route class, which isn’t very idiomatic in current Ktor versions:
```kotlin
class CustomAuthRoute(parent: RoutingNode?, selector: RouteSelector, developmentMode: Boolean = false, environment: ApplicationEnvironment) : RoutingNode(parent, selector, developmentMode, environment) {
    fun ApplicationCall.authenticatedUser() = principal<UserIdPrincipal>() ?: throw Exception("User is not authenticated")
}

@KtorDsl
fun Route.requiredAuth(build: CustomAuthRoute.() -> Unit) =
    authenticate("user-header") {
        // hack #1: the code if a child creating is copyPasted from the `RoutingNode.createChild()` method.
        val childrenNodesMutableList = (this as RoutingNode).children as MutableList<RoutingNode>
        val newSelector: RouteSelector = CustomAuthRouteSelector()

        val existingRoute = childrenNodesMutableList.firstOrNull { it.selector == newSelector }
        val customAuthRoute = if (existingRoute == null) {
            // not sure if the `if` required in our case..
            // it looks like the existingRoute is always null
            // (newSelector is just a newly created object, and the `==` is just the references comparing in this case)
            val newRoute = CustomAuthRoute(this, selector, developmentMode, environment)
            childrenNodesMutableList.add(newRoute)
            newRoute
        } else {
            existingRoute
        }

        @Suppress("UNCHECKED_CAST")
        customAuthRoute.apply(build as (Route.() -> Unit) ) // <- the cast is hack #2 :-)
    }

class CustomAuthRouteSelector : RouteSelector() {
    override suspend fun evaluate(context: RoutingResolveContext, segmentIndex: Int) = RouteSelectorEvaluation.Transparent
    override fun toString() = "CustomAuthRouteSelector()"
}
```

And now the routers work as expected:
```kotlin
    routing {
        requiredAuth { // <- builds principal of a certain type
            get("/private") {
                val user = call.authenticatedUser()  // <- returns the principal of the certain type
                call.respondText("Hello ${user.name}!")
            }
        }
    }
```
