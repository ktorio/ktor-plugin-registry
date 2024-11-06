# Ktor Server Kafka

![Build Status](https://img.shields.io/github/actions/workflow/status/flaxoos/extra-ktor-plugins/build-and-publish-main.yml?event=push&logo=githubactions&style=for-the-badge)
[![Maven-central](https://img.shields.io/maven-central/v/io.github.flaxoos/ktor-server-task-scheduling?style=for-the-badge&logo=apachemaven)](https://search.maven.org/search?q=io.github.flaxoos%20AND%20ktor)
![Language: Kotlin](https://img.shields.io/github/languages/top/flaxoos/flax-ktor-plugins?color=blue&logo=kotlin&style=for-the-badge)
[![Koverage: 94.42%](https://img.shields.io/badge/94.42-green?logo=kotlin&label=koverage&style=for-the-badge)](file:/Users/ido/IdeaProjects/flax-ktor-plugins/build/reports/kover/html/index.html)
[![Docs](https://custom-icon-badges.demolab.com/badge/Pages-blue.svg?label=Docs&logo=github&logoColor=white?icon=githubpages&style=for-the-badge)](https://github.com/Flaxoos/extra-ktor-plugins/actions/workflows/pages/pages-build-deployment)
[![Awesome Kotlin Badge](https://custom-icon-badges.demolab.com/badge/awesome-kotlin-orange.svg?labelColor=blue&style=for-the-badge)](https://github.com/KotlinBy/awesome-kotlin)

Integrate Apache Kafka into your Ktor applications with this plugin. 
It provides flexible configuration options, admin client functionality, easy producer and consumer setup, and built-in Avro4k support. 
Simplify event-driven communication and real-time data processing within your Ktor projects.

## Usage

The plugin provides a DSL that enables comprehensive Kafka configuration, adhering to the classes and properties defined in [org.apache.kafka.common.config](https://kafka.apache.org/21/javadoc/index.html?org/apache/kafka/common/config/package-summary.html), the DSL offers a fluent, programmatic way to set up your Kafka settings right within your Ktor application.

- Setting up clients:
```kotlin
install(Kafka) {
     schemaRegistryUrl = listOf("my.schemaRegistryUrl")
     common { // <-- Define common configs
         // properties
     } 
     admin { // <-- Creates an admin client
         // properties
     }
     producer { // <-- Creates a producer
         // properties
     }
     consumer { // <-- Creates a consumer
         clientId = "my-client-id-override" //<-- Override common properties
     }
}
```

- Handling records
```kotlin
// in the plugin install block
    val myTopic = TopicName.named("my-topic")
    consumerConfig {
        consumerRecordHandler(myTopic) { record ->
            myService.save(record)
        }
    }
```

- Registering Schemas:

```kotlin
// in the plugin install block
    registerSchemas {
        using { // <-- optionally provide a client, by default CIO is used
            HttpClient()
        }
        MyRecord::class at myTopic // <-- Will register schema upon startup
    }
```

- Producing records
```kotlin
fun Application.sendKafkaEvent(event: MyEvent) {
   kafkaProducer?.send(ProducerRecord("events", event.time.toString(), event.toRecord()))
        ?.get(100, TimeUnit.MILLISECONDS)
}
```

- Using the admin
```kotlin
val Application.kafkaMetrics
    get() = kafkaAdminClient?.metrics()
```

### File Configuration:

You can also easily install the Kafka plugin using an application configuration file, instead of configuring in code:

```kotlin
install(FileConfig.Kafka) {
    // consumerConfig and registerSchemas are still done here 
}
```
You can also specify a different path if needed:
```kotlin
install(FileConfig.Kafka("ktor.my.kafka")) {
    // consumerConfig and registerSchemas are still done here
}
```
Example file configuration:

```hocon
ktor {
  kafka {
    schema.registry.url = ["SCHEMA_REGISTRY_URL"]
    common {
      "bootstrap.servers" = ["BOOTSTRAP_SERVERS"]
      # Additional attributes
    }
    admin {
      ##Additional attributes
    }
    consumer {
      "group.id" = "my-group-id"
      # Additional attributes
    }
    producer {
      "client.id" = "my-client-id"
      # Additional attributes
    }
    topics = [
      {
        name = my-topic
        partitions = 1
        configs {
          "message.timestamp.type" = CreateTime
          # Additional attributes
        }
      }
    ]
  }
}
```

### Access Kafka Clients:

The Kafka clients, if installed, are always available as properties of the application:

```kotlin
val adminClient = application.kafkaAdminClient
val producer = application.kafkaProducer
val consumer = application.kafkaConsumer
```