import io.github.damir.denis.tudor.ktor.server.rabbitmq.RabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.*
import io.github.damir.denis.tudor.ktor.server.rabbitmq.rabbitMQ
import io.ktor.server.application.*
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import kotlinx.coroutines.Dispatchers

fun Application.install() {
    install(RabbitMQ) {
        uri = "amqp://<user>:<password>@<address>:<port>"
        defaultConnectionName = "default-connection"
        dispatcherThreadPollSize = 2
        tlsEnabled = false
    }

    rabbitmq {
        queueBind {
            queue = "dlq"
            exchange = "dlx"
            routingKey = "dlq-dlx"
            exchangeDeclare {
                exchange = "dlx"
                type = "direct"
            }
            queueDeclare {
                queue = "dlq"
                durable = true
            }
        }
    }

    rabbitmq {
        queueBind {
            queue = "test-queue"
            exchange = "test-exchange"
            routingKey = "test-routing-key"
            exchangeDeclare {
                exchange = "test-exchange"
                type = "direct"
            }
            queueDeclare {
                queue = "test-queue"
                arguments = mapOf(
                    "x-dead-letter-exchange" to "dlx",
                    "x-dead-letter-routing-key" to "dlq-dlx"
                )
            }
        }.onSuccess{ response->
            log.info("Successfully bind queue: $response")
        }.onFailure{ error ->
            log.error("Failed to bind queue: $error")
        }
    }

    routing {
        get("/rabbitmq") {
            rabbitmq {
                basicPublish {
                    exchange = "test-exchange"
                    routingKey = "test-routing-key"
                    message { "Hello Ktor!" }
                }
            }
            call.respondText("Hello RabbitMQ!")
        }


        rabbitmq {
            basicConsume {
                autoAck = true
                queue = "test-queue"
                dispatcher = Dispatchers.rabbitMQ
                deliverCallback<String> { tag, message ->
                    log.debug("Received message: $message")
                }
            }
        }
    }
}
