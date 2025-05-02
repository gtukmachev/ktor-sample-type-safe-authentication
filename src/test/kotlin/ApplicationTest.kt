package com.example

import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {

    val logger: Logger = LoggerFactory.getLogger(ApplicationTest::class.java)

    @Test fun testRoutes() = testApplication {
        application {
            module()
        }

        testRoute("/", user = null, expectedStatus = HttpStatusCode.OK)

        testRoute("/public", user = null,       expectedStatus = HttpStatusCode.OK)
        testRoute("/public", user = "TestUser", expectedStatus = HttpStatusCode.OK)

        testRoute("/private", user = null,       expectedStatus = HttpStatusCode.InternalServerError)
        testRoute("/private", user = "TestUser", expectedStatus = HttpStatusCode.OK)
    }

    private suspend fun ApplicationTestBuilder.testRoute(path: String, user: String?, expectedStatus: HttpStatusCode) {
        client.get(path){
            if (user != null) headers.append("user", user)
        }.apply {
            logger.info(
                """
                    |////////////////////////////////////////////////////////////////////////////////////
                    |get /path ${ if (user != null) "{user=$user}" else ""} << ${bodyAsText()} 
                    |
                """.trimMargin()
            )
            assertEquals(expectedStatus, status)
        }
    }

}
