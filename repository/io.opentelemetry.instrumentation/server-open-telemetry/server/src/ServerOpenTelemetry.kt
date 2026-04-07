package kastle

import io.ktor.http.*
import io.ktor.server.request.*
import io.ktor.server.application.*
import io.opentelemetry.api.trace.SpanKind
import io.opentelemetry.instrumentation.ktor.v3_0.KtorServerTelemetry

fun Application.configureOpenTelemetry() {
    install(KtorServerTelemetry) {
        setOpenTelemetry(getOpenTelemetry(serviceName = "ktor-sample"))
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
                attributes.put("start-time", System.currentTimeMillis())
            }
            onEnd {
                attributes.put("end-time", System.currentTimeMillis())
            }
        }
    }
}
