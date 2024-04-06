# Line Webhook Signature Validation Plugin
The LineWebhook plugin is used to verify the signature of incoming webhooks. This ensures that the webhooks are from a trusted source.

[![Maven Central Version](https://img.shields.io/maven-central/v/io.github.cotrin8672/ktor-line-webhook-plugin)
](https://central.sonatype.com/artifact/io.github.cotrin8672/ktor-line-webhook-plugin)
## Gradle Configuration
```gradle
implementation("io.github.cotrin8672:ktor-line-webhook-plugin:1.2.0")
```

## Usage
Install this plugin only on endpoints that receive webhooks.
```kotlin
route("/callback") {
    install(DoubleReceive)
    install(LineSignatureVerification) {
        channelSecret = System.getenv("CHANNEL_SECRET")
    }
    post {
        call.respond(HttpStatusCode.OK)
    }
}
```
