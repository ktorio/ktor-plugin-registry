/*
 * Copyright 2014-2026 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.registry

import io.kotest.core.spec.Spec
import io.kotest.core.test.TestCase
import io.kotest.engine.coroutines.CoroutineDispatcherFactory
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.util.concurrent.Executors

// Each test does a lot of blocking, so we want a large thread pool
private const val THREAD_POOL_SIZE = 12

object ThreadPoolDispatcherFactory : CoroutineDispatcherFactory {
    val dispatcher = Executors.newFixedThreadPool(THREAD_POOL_SIZE).asCoroutineDispatcher()

    override suspend fun <T> withDispatcher(spec: Spec, f: suspend () -> T): T {
        return withContext(dispatcher) {
            f()
        }
    }

    override suspend fun <T> withDispatcher(testCase: TestCase, f: suspend () -> T): T {
        return withContext(dispatcher) {
            f()
        }
    }

    override fun close() {
        dispatcher.close()
    }
}
