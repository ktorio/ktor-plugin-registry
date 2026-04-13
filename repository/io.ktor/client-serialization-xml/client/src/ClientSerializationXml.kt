package kastle

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.serialization.kotlinx.xml.*

fun HttpClientConfig<*>.configureClientSerializationXml() {
    install(ContentNegotiation) {
        xml()
    }
}
