
#### This plugin provides access to major core functionalities of the `com.rabbitmq:amqp-client` library.

### Features

- Includes a built-in connection/channel management system.
- Gives the possibility to interact directly with the java library.
- Seamlessly integrates with the Kotlin DSL, making it readable, maintainable, and easy to use.
- Integrated with coroutines, featuring a separate dispatcher and the ability to define a custom coroutine scope.


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
            properties = basicProperties {
                correlationId = "jetbrains"
                type = "plugin"
                headers = mapOf("ktor" to "rabbitmq")
            }
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

### Custom Coroutine Scope Example
```kotlin
val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
    println("ExceptionHandler got $throwable")
}

val rabbitMQScope = CoroutineScope(SupervisorJob() + exceptionHandler)

// ...

install(RabbitMQ) {
    connectionAttempts = 3
    attemptDelay = 10
    uri = rabbitMQContainer.amqpUrl
    scope = rabbitMQScope
}

// ...

rabbitmq {
    connection(id = "consume") {
        basicConsume {
            autoAck = true
            queue = "demo-queue"
            dispacher = Dispacher.IO
            coroutinePollSize = 1_000
            deliverCallback<String> { message ->
                throw Exception("business logic exception")
            }
        }
    }
}
```

### Serialization Fallback Example

```kotlin 
@Serializable
data class Message(
    var content: String
)

fun Application.module() {
    install(RabbitMQ) {
        uri = "amqp://guest:guest@localhost:5672"
        dispatcherThreadPollSize = 3
    }

    rabbitmq {
        queueBind {
            queue = "test-queue"
            exchange = "test-exchange"
            queueDeclare {
                queue = "test-queue"
                arguments = mapOf(
                    "x-dead-letter-exchange" to "dlx",
                    "x-dead-letter-routing-key" to "dlq-dlx"
                )
            }
            exchangeDeclare {
                exchange = "test-exchange"
                type = "fanout"
            }
        }
    }

    rabbitmq {
        repeat(10) {
            basicPublish {
                exchange = "test-exchange"
                message {
                    Message(content = "Hello world!")
                }
            }
        }
        repeat(10) {
            basicPublish {
                exchange = "test-exchange"
                message { "Hello world!" }
            }
        }
    }

    rabbitmq {
        basicConsume {
            queue = "test-queue"
            autoAck = false
            deliverCallback<Message> { message ->
                println("Received as Message: ${message.body}")
            }
            deliverFailureCallback { message ->
                println("Could not serialize, received as ByteArray: ${message.body}")
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