package kastle

import io.ktor.client.HttpClient

val httpClient = HttpClient {
    _slots("clientConfig")
}
