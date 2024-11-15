import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreakerName.Companion.toCircuitBreakerName
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.CircuitBreaking
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.global
import io.github.flaxoos.ktor.client.plugins.circuitbreaker.register
import io.ktor.client.*
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
