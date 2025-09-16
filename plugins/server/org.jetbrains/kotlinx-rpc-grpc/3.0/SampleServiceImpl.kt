class SampleServiceImpl : SampleService {
    override suspend fun greeting(name: ClientGreeting): ServerGreeting {
        return ServerGreeting { content = "Hello, ${name.name}!" }
    }
}
