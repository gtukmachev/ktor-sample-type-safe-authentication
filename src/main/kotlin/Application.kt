package com.example

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun main(args: Array<String>) {
    io.ktor.server.netty.EngineMain.main(args)
}

class CustomAuthScope {
    fun ApplicationCall.authenticatedUser() = principal<UserIdPrincipal>() ?: throw Exception("User is not authenticated")
}

inline fun Route.requiredAuth(crossinline build: CustomAuthScope.() -> Unit) =
    authenticate("user-header") {
        CustomAuthScope().apply {
            build()
        }
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

        // works fine.
        authenticate("user-header") {
            CustomAuthScope().apply {
                get("/private") {
                    val user = call.authenticatedUser()
                    call.respondText("Hello ${user.name}!")
                }
            }
        }

        // doesn't work :-( - the `principal<UserIdPrincipal>()` always returns `null`
        requiredAuth { // <- builds principal of a certain type
            get("/private-2") {
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

