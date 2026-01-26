
The [DoubleReceive](https://ktor.io/docs/double-receive.html) plugin allows you to invoke `ApplicationCall.receive` several times with no `RequestAlreadyConsumedException`. It usually makes sense when a plugin consumes a request body so a handler cannot receive it again.

## Usage

The code snippet below shows how to install `DoubleReceive`:
```kotlin
install(DoubleReceive)
```
After that, you can receive a call multiple times, and every invocation may return the same instance:
```kotlin
val first = call.receiveText()
val theSame = call.receiveText()
```
You can learn more from [DoubleReceive](https://ktor.io/docs/double-receive.html).
## Options

* `receiveEntireContent` - when enabled, for every request the whole content will be received and stored as a byte array. This is useful when completely different types need to be received. You also can receive streams and channels. Note that enabling this causes the whole receive pipeline to be executed for every further receive pipeline.
