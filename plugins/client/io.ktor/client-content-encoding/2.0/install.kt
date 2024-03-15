import io.ktor.client.*
import io.ktor.client.plugins.compression.*

public fun HttpClientConfig<*>.configure() {
    install(ContentEncoding) {
        deflate(1.0F)
        gzip(0.9F)
    }
}
