/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

import io.ktor.server.routing.*
import kotlinx.rpc.krpc.serialization.json.*
import kotlinx.rpc.krpc.ktor.server.rpc

fun Routing.configureRouting() {
    rpc("/api") {
        rpcConfig {
            serialization {
                json()
            }
        }

        registerService<SampleService> { SampleServiceImpl() }
    }
}
