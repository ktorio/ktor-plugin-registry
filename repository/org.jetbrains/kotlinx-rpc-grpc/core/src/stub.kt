package kastle

class ClientGreeting {
    var name: String = ""

    companion object
}

operator fun ClientGreeting.Companion.invoke(body: ClientGreeting.() -> Unit): ClientGreeting = ClientGreeting().apply(body)

class ServerGreeting {
    var content: String = ""

    companion object
}

operator fun ServerGreeting.Companion.invoke(body: ServerGreeting.() -> Unit): ServerGreeting = ServerGreeting().apply(body)

interface SampleService {
    suspend fun greeting(name: ClientGreeting): ServerGreeting
}
