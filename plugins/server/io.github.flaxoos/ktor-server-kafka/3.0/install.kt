import io.github.flaxoos.ktor.server.plugins.kafka.Kafka
import io.github.flaxoos.ktor.server.plugins.kafka.MessageTimestampType
import io.github.flaxoos.ktor.server.plugins.kafka.TopicName
import io.github.flaxoos.ktor.server.plugins.kafka.admin
import io.github.flaxoos.ktor.server.plugins.kafka.common
import io.github.flaxoos.ktor.server.plugins.kafka.consumer
import io.github.flaxoos.ktor.server.plugins.kafka.consumerConfig
import io.github.flaxoos.ktor.server.plugins.kafka.consumerRecordHandler
import io.github.flaxoos.ktor.server.plugins.kafka.producer
import io.github.flaxoos.ktor.server.plugins.kafka.registerSchemas
import io.github.flaxoos.ktor.server.plugins.kafka.topic
import io.ktor.client.HttpClient
import io.ktor.server.application.Application
import io.ktor.server.application.install

// The contents of the `install` function will be used for the project template
public fun Application.install() {
    install(Kafka) {
        schemaRegistryUrl = "my.schemaRegistryUrl"
        val myTopic = TopicName.named("my-topic")
        topic(myTopic) {
            partitions = 1
            replicas = 1
            configs {
                messageTimestampType = MessageTimestampType.CreateTime
            }
        }
        common { // <-- Define common properties
            bootstrapServers = listOf("my-kafka")
            retries = 1
            clientId = "my-client-id"
        }
        admin { } // <-- Creates an admin client
        producer { // <-- Creates a producer
            clientId = "my-client-id"
        }
        consumer { // <-- Creates a consumer
            groupId = "my-group-id"
            clientId = "my-client-id-override" //<-- Override common properties
        }
        consumerConfig {
            consumerRecordHandler(myTopic) { record ->
                // Do something with record
            }
        }
        registerSchemas {
            using { // <-- optionally provide a client, by default CIO is used
                HttpClient()
            }
            // MyRecord::class at myTopic // <-- Will register schema upon startup
        }
    }
}
