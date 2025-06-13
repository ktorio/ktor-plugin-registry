/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.example

import io.ktor.server.html.respondHtml
import io.ktor.server.routing.*
import kotlinx.html.*
import kotlin.random.Random

fun Routing.htmxEndpoints() {
    val random = Random(System.currentTimeMillis())

    get("/") {
        call.respondHtml {
            leaderboardPage(random)
        }
    }

    get("/more-rows") {
        call.respondHtml {
            body {
                table {
                    tbody {
                        randomRows(random)
                    }
                }
            }
        }
    }
}