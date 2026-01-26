
Ktor supports secure sockets. To enable them you will need to include the `io.ktor:ktor-network-tls:$ktor_version` artifact, and call the `.tls()` to a connected socket.



## Usage

Connect to a secure socket:

```kotlin
runBlocking {
    val socket = aSocket(ActorSelectorManager(Dispatchers.IO)).tcp().connect(InetSocketAddress("google.com", 443)).tls()
    val w = socket.openWriteChannel(autoFlush = false)
    w.write("GET / HTTP/1.1\r\n")
    w.write("Host: google.com\r\n")
    w.write("\r\n")
    w.flush()
    val r = socket.openReadChannel()
    println(r.readUTF8Line())
}
```
You can adjust a few optional parameters for the TLS connection:

```kotlin
suspend fun Socket.tls(
        trustManager: X509TrustManager? = null,
        randomAlgorithm: String = "NativePRNGNonBlocking",
        serverName: String? = null,
        coroutineContext: CoroutineContext = Dispatchers.IO
): Socket
```

## Options

No options
