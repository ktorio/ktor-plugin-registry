
The Ktor DI plugin allows for the declaration and resolution of services through a shared application state.

## Usage

### Declaration

Dependencies can be provided either via modules or configuration.

#### Modules

The most basic way to supply implementations is through the dependencies DSL within your modules:

```kotlin
fun Application.database() {
    dependencies {
        provide<Database> { PostgresDatabase(property<PostgresConfig>()) }
    }
}
```

#### Reflection

You can provide implementations by referencing classes or constructors:

```kotlin
fun Application.database() {
    dependencies {
        provide<Database> { PostgresDatabase(property<PostgresConfig>()) }
    }
}
```

#### Properties

Adding classpath items to the `ktor.application.dependencies` list property will automatically instantiate and provide them to the map of dependencies. 

You can supply class names or function references this way.  Parameters will automatically be resolved through the dependency resolution context.

```
# application.yaml
ktor:
  application:
    dependencies:
      - com.example.db.PostgresDatabase
      - com.example.DatabaseKt#configureDb
```

### Resolution

Dependencies can be resolved inside modules or through their parameters.

#### Modules

Using the same _dependencies_ DSL in our modules:

```kotlin
suspend fun Application.endpoints() {
    val database: Database = dependencies.resolve()
    
    routing {
        get("/users") {
            call.respond(database.query<User>().toList())
        }
    }
}
```

#### Parameters

You can also specify the correct parameters when loading modules from your configuration file:

```kotlin
fun Application.endpoints(database: Database) {
    // etc.
}
```