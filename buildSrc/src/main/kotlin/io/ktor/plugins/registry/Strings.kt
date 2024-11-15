/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry

fun String.stripSpecialChars() =
    Regex("[^a-zA-Z0-9\\-,.]").replace(this, "").trim(',')