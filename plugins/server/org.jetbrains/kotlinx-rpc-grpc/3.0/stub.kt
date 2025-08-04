// DO NOT DELETE, not included in generation, but is used for code highlighting in registry

class ClientGreeting {
    var name: String = ""

    companion object {
        operator fun invoke(body: ClientGreeting.() -> Unit): ClientGreeting = ClientGreeting().apply(body)
    }
}

class ServerGreeting {
    var content: String = ""

    companion object {
        operator fun invoke(body: ServerGreeting.() -> Unit): ServerGreeting = ServerGreeting().apply(body)
    }
}

interface SampleService {
    suspend fun greeting(name: ClientGreeting): ServerGreeting
}
