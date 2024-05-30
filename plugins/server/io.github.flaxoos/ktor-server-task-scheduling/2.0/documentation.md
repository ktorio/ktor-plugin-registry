Manage scheduled tasks across instances of your distributed ktor server, using various strategies and a kotlin flavoured cron tab
Use Redis, JDBC or MongoDb for managing locks, or implement your own strategy 


## Usage

The plugin provides a DSL for defining task managers and tasks

- Add your chosen lock manager:
```kotlin
    implementation("io.github.flaxoos:ktor-server-task-scheduling-${redis/jdbc/mongodb}:$version")
```
- Setting up task manager:

```kotlin
    install(TaskScheduling) {
    redis { //<-- given no name, this will be the default manager
        connectionPoolInitialSize = 1
        host = "host"
        port = 6379
        username = "my_username"
        password = "my_password"
        connectionAcquisitionTimeoutMs = 1_000
        lockExpirationMs = 60_000
    }
    jdbc("my jdbc manager") {
        database = org.jetbrains.exposed.sql.Database.connect(
            url = "jdbc:postgresql://host:port",
            driver = "org.postgresql.Driver",
            username = "my_username",
            password = "my_password"
        ).also {
            transaction { SchemaUtils.create(DefaultTaskLockTable) }
        }
    }
    mongoDb("my mongodb manager") {
        databaseName = "test"
        client = MongoClient.create("mongodb://host:port")
    }
}
```

- Setting up tasks:
```kotlin
task { // if no taskManagerName is provided, the task would be assigned to the default manager
    name = "My task"
    task = { taskExecutionTime ->
        log.info("My task is running: $taskExecutionTime")
    }
    kronSchedule = {
        hours {
            from 0 every 12
        }
        minutes {
            from 15 every 30
        }
    }
    concurrency = 2
}

task(taskManagerName = "my jdbc manager") {
    name = "My Jdbc task"
    // rest of task config
}
```