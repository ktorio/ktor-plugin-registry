import com.rabbitmq.client.BuiltinExchangeType
import io.github.damir.denis.tudor.ktor.server.rabbitmq.KabbitMQ
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.extensions.basicAck
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.extensions.exchangeDeclare
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.extensions.queueBind
import io.github.damir.denis.tudor.ktor.server.rabbitmq.dsl.extensions.queueDeclare
import io.ktor.server.application.*

fun Application.install() {
    install(KabbitMQ) {
        uri = "amqp://guest:guest@localhost:5672"
    }

    queueBind {
        queue = "dlq"
        exchange = "dlx"
        routingKey = "dlq-dlx"
        queueDeclare {
            queue = "dlq"
            durable = true
        }
        exchangeDeclare {
            exchange = "dlx"
            type = BuiltinExchangeType.DIRECT
        }
    }

    queueBind {
        queue = "test-queue"
        exchange = "test-exchange"
        routingKey = "test-routing-key"
        queueDeclare {
            queue = "test-queue"
            arguments = mapOf(
                "x-dead-letter-exchange" to "dlx",
                "x-dead-letter-routing-key" to "dlq-dlx"
            )
        }
        exchangeDeclare {
            exchange = "test-exchange"
            type = BuiltinExchangeType.FANOUT
        }
    }
}
