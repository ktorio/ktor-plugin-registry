
#### This plugin provides access to all the core functionalities of the `com.rabbitmq:amqp-client` library.

### Features

- Seamlessly integrates with `Kotlin DSL`, making it readable, maintainable, and easy to use.
- Includes a built-in connection/channel management system.
- Provides a built-in mechanism for validating property combinations.


## Usage

### Instalation
```kotlin
install(KabbitMQ) {
    uri = "amqp://<user>:<password>@<address>:<port>"
    defaultConnectionName = "<default_connection>"
    connectionAttempts = 20
    attemptDelay = 10

    tlsEnabled = true
    tlsKeystorePath = "<path>"
    tlsKeystorePassword = "<password>"
    tlsTruststorePath = "<path>"
    tlsTruststorePassword = "<password>"
}
```

### Queue binding example
```kotlin
queueBind {
    queue = "test-queue"
    exchange = "test-exchange"
    routingKey = "test-routing-key"
    queueDeclare {
        queue = "test-queue"
        durable = true
    }
    exchangeDeclare {
        exchange = "test-exchange"
        type = BuiltinExchangeType.DIRECT
    }
}
```

### Producer example
```kotlin
channel(id = 2, autoClose = true) {
    repeat(10) {
        basicPublish {
            exchange = "test-exchange"
            routingKey = "test-routing-key"
            message {
                Message(content = "Hello world!")
            }
        }
    }
}
```

### Consumer Example
```kotlin
basicConsume {
    queue = "test-queue"
    autoAck = false
    deliverCallback<Message> { tag, message ->
        basicAck {
            deliveryTag = tag
        }
    }
}
```

### Library Calls Example
```kotlin
channel("direct-calls"){
    basicPublish("test", "test-routing-key", null, "Hello!".toByteArray())
    
    val consumer = object : DefaultConsumer(channel) {
        override fun handleDelivery(
            consumerTag: String?,
            envelope: Envelope?,
            properties: AMQP.BasicProperties?,
            body: ByteArray?
        ) {
            
        }
    }
    basicConsume(queueName, true, consumer)
}
```

### For additional details check [repo](https://github.com/DamirDenis-Tudor/ktor-server-rabbitmq).