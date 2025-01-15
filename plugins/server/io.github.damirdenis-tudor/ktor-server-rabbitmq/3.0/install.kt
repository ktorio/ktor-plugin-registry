import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicConsume
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.basicPublish
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.exchangeDeclare
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.queueBind
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.queueDeclare
import io.github.damir.denis.tudor.ktor.server.rabbitmq.plugin.RabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.plugin.rabbitmq
import io.ktor.server.application.*
import io.ktor.server.request.receiveText
import io.ktor.server.response.respondText
import io.ktor.server.routing.application
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.install() {
    install(RabbitMQ)

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
        }
    }

    routing {
        get("/rabbitmq") {
            rabbitmq {
                basicPublish {
                    exchange = "demo-exchange"
                    routingKey = "demo-routing-key"
                    message { "Hello Ktor!" }
                }
            }
            call.respondText("Hello RabbitMQ!")
        }


        rabbitmq {
            basicConsume {
                autoAck = true
                queue = "demo-queue"
                deliverCallback<String> { tag, message ->
                    log.debug("Received message: $message")
                }
            }
        }
    }
}
