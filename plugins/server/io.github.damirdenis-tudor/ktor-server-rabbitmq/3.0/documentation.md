
#### This plugin provides access to major core functionalities of the `com.rabbitmq:amqp-client` library.

### Features

- Integrated with coroutines and has a separate dispatcher.
- Seamlessly integrates with the Kotlin DSL, making it readable, maintainable, and easy to use.
- Includes a built-in connection/channel management system.
- Provides a built-in mechanism for validating property combinations.
- Gives the possibility to interact directly with the java library.


## Usage

### Installation
```kotlin
install(RabbitMQ) {
    uri = "amqp://<user>:<password>@<address>:<port>"
    defaultConnectionName = "<default_connection>"
    connectionAttempts = 20
    attemptDelay = 10
    dispatcherThreadPollSize = 2

    tlsEnabled = true
    tlsKeystorePath = "<path>"
    tlsKeystorePassword = "<password>"
    tlsTruststorePath = "<path>"
    tlsTruststorePassword = "<password>"
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

### Consumer Example With Dispatchers.IO
```kotlin
rabbitmq {
    basicConsume {
        autoAck = true
        queue = "demo-queue"
        dispatcher = Dispatchers.IO
        deliverCallback<String> { tag, message ->
            logger.info("Received message: $message")
        }
    }
}
```

### Library Calls Example
```kotlin
rabbitmq {
    libConnection("lib_connection") {
        with(createChannel()) {
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
}
```