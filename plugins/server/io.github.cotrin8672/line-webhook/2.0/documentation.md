The LineWebhook plugin is used to verify signatures of incoming webhooks when utilizing the LINE Messaging API.
This ensures the webhooks are from the official LINE source, providing security for data transfer.

## About LINE
LINE is a widely used messaging app, primarily in Asia.
In addition to its messaging platform, LINE also provides a robust Messaging API. This enables developers to create sophisticated chat bots which can interact with users, providing a broad array of services and functionalities.

## How it Works
The verification process relies on a channel secret obtained from
[LINE's Developer Console](https://developers.line.biz/console).
This secret is used to compute a hash with the incoming message and is then compared against the "X-Line-Signature"
in the header of the incoming request.

If the signatures match, the request is deemed trustworthy and is proceeded with. Otherwise, the request is denied and a "Forbidden" response is returned.

## Gradle Configuration
```kotlin
implementation("io.github.cotrin8672:ktor-line-webhook-plugin:1.5.0")
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

## Additional Resources
For more information on the LINE Messaging API and signature verification, refer to the
[Official documentation](https://developers.line.biz/ja/docs/messaging-api/) and
[Signature documentation](https://developers.line.biz/ja/docs/messaging-api/receiving-messages/#verify-signature).
