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

        authenticate("user-header") {
            CustomAuthScope().apply {
                get("/private") {
                    val userName = call.authenticatedUser().name
                    call.respondText("Hello $userName!")
                }
            }
        }

        requiredAuth {
            get("/private-2") {
                val userName = call.authenticatedUser().name
                call.respondText("Hello to $userName from private-2!")
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

