/**
 * slot://io.ktor/server-core/http
 */
package kastle

import io.ktor.server.application.*
import com.ucasoft.ktor.simpleCache.SimpleCache
import com.ucasoft.ktor.simpleMemoryCache.*
import kotlin.time.Duration.Companion.seconds

fun Application.configureSimpleMemoryCache() {
    install(SimpleCache) {
        memoryCache {
            invalidateAt = 10.seconds
        }
    }
}
