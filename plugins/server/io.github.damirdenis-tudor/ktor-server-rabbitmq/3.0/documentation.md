
#### This plugin provides access to major core functionalities of the `com.rabbitmq:amqp-client` library.

### Features

- Integrated with coroutines and has a separate dispatcher.
- Includes a built-in connection/channel management system.
- Gives the possibility to interact directly with the java library.
- Seamlessly integrates with the Kotlin DSL, making it readable, maintainable, and easy to use.


## Usage

### Installation
```kotlin
install(RabbitMQ) {
    uri = "amqp://<user>:<password>@<address>:<port>"
    defaultConnectionName = "<default_connection>"
    connectionAttempts = 20
    attemptDelay = 10
    dispatcherThreadPollSize = 4
    tlsEnabled = false
}
```

### Queue binding example
```kotlin
rabbitmq {
    queueBind {
        queue = "demo-queue"
        exchange = "demo-exchange"
        routingKey = "demo-routing-key"
        queueDeclare {
            queue = "demo-queue"
            durable = true
        }
        exchangeDeclare {
            exchange = "demo-exchange"
            type = "direct"
        }
    }
}
```

### Producer example
```kotlin
rabbitmq {
    repeat(10) {
        basicPublish {
            exchange = "demo-exchange"
            routingKey = "demo-routing-key"
            message { "Hello World!" }
        }
    }
}
```

### Consumer Example
```kotlin
rabbitmq {
    basicConsume {
        autoAck = true
        queue = "demo-queue"
        deliverCallback<String> { tag, message ->
            logger.info("Received message: $message")
        }
    }
}
```

### Consumer Example with coroutinePollSize
```kotlin
rabbitmq {
    connection(id = "consume") {
        basicConsume {
            autoAck = true
            queue = "demo-queue"
            dispacher = Dispacher.IO
            coroutinePollSize = 1_000
            deliverCallback<String> { tag, message ->
                logger.info("Received message: $message")
                delay(30)
            }
        }
    }
}
```

### Library Calls Example
```kotlin
rabbitmq {
    libChannel(id = 2) {
        basicPublish("demo-queue", "demo-routing-key", null, "Hello!".toByteArray())

        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope?,
                properties: AMQP.BasicProperties?,
                body: ByteArray?
            ) {

            }
        }

        basicConsume("demo-queue", true, consumer)
    }
}
```

```kotlin
rabbitmq {
    libConnection(id = "lib-connection") {
        val channel = createChannel()

        channel.basicPublish("demo-queue", "demo-routing-key", null, "Hello!".toByteArray())

        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope?,
                properties: AMQP.BasicProperties?,
                body: ByteArray?
            ) {

            }
        }

        channel.basicConsume("demo-queue", true, consumer)
    }
}
```