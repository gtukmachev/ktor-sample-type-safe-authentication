package com.example

import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.response.*
import io.ktor.server.routing.*

fun Application.configureRouting() {

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
            get("/private") {
                val userName = call.principal<UserIdPrincipal>()?.name ?: "unauthorized"
                call.respondText("Hello $userName !")
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


