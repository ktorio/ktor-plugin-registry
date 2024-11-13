/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.client.plugins.websocket.*
import io.ktor.server.testing.*
import kotlinx.rpc.krpc.ktor.client.RPC
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.ktor.client.rpcConfig
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationRpcTest {
    @Test
    fun testRpc() = testApplication {
        application {
            configureFrameworks()
        }

        val ktorClient = createClient {
            install(WebSockets)
            install(RPC)
        }

        val rpcClient = ktorClient.rpc("/api") {
            rpcConfig {
                serialization {
                    json()
                }
            }
        }

        val service = rpcClient.withService<SampleService>()

        val response = service.hello(Data("client"))

        assertEquals("Server: client", response)
    }
}