
In addition to HTTP handling for the server and the client, Ktor supports client and server, TCP and UDP raw sockets. It exposes a suspending API that uses NIO under the hoods.



## Usage

In order to create either server or client sockets, you have to use the `aSocket` builder, with a mandatory `ActorSelectorManager`: `aSocket(selector)`. For example: `aSocket(ActorSelectorManager(Dispatchers.IO))`.

Then use:
* `val socketBuilder = aSocket(selector).tcp()` for a builder using TCP sockets
* `val socketBuilder = aSocket(selector).udp()` for a builder using UDP sockets

This returns a `SocketBuilder` that can be used to:
* `val serverSocket = aSocket(selector).tcp().bind(address)` to listen to an address (for servers)
* `val clientSocket = aSocket(selector).tcp().connect(address)` to connect to an address (for clients)

If you need to control the dispatcher used by the sockets, you can instantiate a selector, that uses, for example, a cached thread pool:
```kotlin
val exec = Executors.newCachedThreadPool()
val selector = ActorSelectorManager(exec.asCoroutineDispatcher())
val tcpSocketBuilder = aSocket(selector).tcp()
```
Once you have a `socket` open by either binding or connecting the builder, you can read from or write to the socket, by opening read/write channels:

```kotlin
val input : ByteReadChannel  = socket.openReadChannel()
val output: ByteWriteChannel = socket.openWriteChannel(autoFlush = true)
```
When creating a server socket, you have to bind to a specific `SocketAddress` to get a `ServerSocket`:

```kotlin
val server = aSocket(selector).tcp().bind(InetSocketAddress("127.0.0.1", 2323))
```
The server socket has an `accept` method that returns, one at a time, a connected socket for each incoming connection pending in the backlog:

```kotlin
val socket = server.accept()
```
When creating a socket client, you have to connect to a specific SocketAddress to get a Socket:

```kotlin
val socket = aSocket(selector).tcp().connect(InetSocketAddress("127.0.0.1", 2323))
```

## Options

No options
