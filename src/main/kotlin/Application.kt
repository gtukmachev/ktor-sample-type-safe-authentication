package com.example

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.utils.io.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

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


fun Application.module() {

    install(Authentication) {
        provider("user-header") {
            authenticate { context: AuthenticationContext ->
                val userName = context.call.request.headers["user"] ?: return@authenticate
                context.principal(UserIdPrincipal(userName))
            }
        }
    }

    routing {
        get("/") {
            call.respondText("Hello World!")
        }

        requiredAuth { // <- builds principal of a certain type
            get("/private") {
                val user = call.authenticatedUser()  // <- returns the principal of the certain type
                call.respondText("Hello ${user.name}!")
            }
        }

        authenticate("user-header") {
            get("/public") {
                val userName = call.principal<UserIdPrincipal>()?.name ?: "unauthorized"
                call.respondText("Hello $userName !")
            }
        }

    }
}

