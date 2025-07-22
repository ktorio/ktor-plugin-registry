import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry
import java.time.Instant

fun Application.configureOpenTelemetry() {
    val openTelemetry = getOpenTelemetry(serviceName = "opentelemetry-ktor-sample-server")

    install(KtorServerTelemetry) {
        setOpenTelemetry(openTelemetry)

        capturedRequestHeaders(HttpHeaders.UserAgent)

        spanKindExtractor {
            if (httpMethod == HttpMethod.Post) {
                SpanKind.PRODUCER
            } else {
                SpanKind.CLIENT
            }
        }

        attributesExtractor {
            onStart {
                attributes.put("start-time", Instant.now().toEpochMilli())
            }
            onEnd {
                attributes.put("end-time", Instant.now().toEpochMilli())
            }
        }
    }
}
