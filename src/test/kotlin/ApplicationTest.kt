package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test fun tsstRoutes() = testApplication {
        application {
            module()
        }

        client.get("/").apply {
            println(
                """
                    ////////////////////////////////////////////////////////////////////////////////////
                    get / << ${bodyAsText()} 
                """.trimIndent()
            )
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/private"){
            headers.append("user", "Test User")
        }.apply {
            println(
                """
                    ////////////////////////////////////////////////////////////////////////////////////
                    get /private{user=Test User} << ${bodyAsText()} 
                """.trimIndent()
            )
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/private-2"){
            headers.append("user", "Test User")
        }.apply {
            println(
                """
                    ////////////////////////////////////////////////////////////////////////////////////
                    get /private-2{user=Test User} << ${bodyAsText()} 
                """.trimIndent()
            )
            assertEquals(HttpStatusCode.OK, status)
        }
    }

}
