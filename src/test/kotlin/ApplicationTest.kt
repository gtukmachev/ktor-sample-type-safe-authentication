package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    @Test
    fun testRoot() = testApplication {
        application { module() }


        client.get("/").apply {
            println("get / << "  + bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
        }


        client.get("/private").apply {
            println("get /private << "  + bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/private"){
            headers.append("user", "Test User")
        }.apply {
            println("get /private{user=Test User} << "  + bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/public").apply {
            println("get /public << "  + bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
        }

        client.get("/public"){
            headers.append("user", "Test User")
        }.apply {
            println("get /public{user=Test User} << "  + bodyAsText())
            assertEquals(HttpStatusCode.OK, status)
        }

    }



}
