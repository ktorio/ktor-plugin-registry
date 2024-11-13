/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerName.Companion.toCircuitBreakerName
import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.MockRequestHandleScope
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.respondError
import io.ktor.client.engine.mock.respondOk
import io.ktor.client.request.HttpResponseData
import io.ktor.http.HttpStatusCode
import io.ktor.http.HttpStatusCode.Companion.InternalServerError
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

fun HttpClientConfig<*>.configure() {
    install(CircuitBreaking) {
        global {
            failureThreshold = 10
            halfOpenFailureThreshold = 5
            resetInterval = 100.milliseconds
        }

        register("strict".toCircuitBreakerName()) {
            failureThreshold = 2
            halfOpenFailureThreshold = 1
            resetInterval = 1.seconds
        }
    }
}
