
Ktor provides the `Resources` plugin that allows you to implement type-safe requests. To accomplish this, you need to create a class that describes resources available on a server and then annotate this class using the `@Resource` keyword. Such classes should also have the `@Serializable` annotation provided by the kotlinx.serialization library.

## Usage

To install `Resources`, pass it to the `install` function inside a [client configuration block](create-client.md#configure-client):
```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.resources.*
//...
val client = HttpClient(CIO) {
    install(Resources)
}
```
### Create resource classes

Each resource class should have the following annotations:

* The `@Serializable` annotation, which is provided by the kotlinx.serialization library.
* The `@Resource` annotation.

Below we'll take a look at several examples of resource classes - defining a single path segment, query and path parameters, and so on.

###### Resource URL

The example below shows how to define the `Articles` class that specifies a resource responding on the `/articles` path.

```kotlin
import io.ktor.resources.*

@Serializable
@Resource("/articles")
class Articles()
```

###### Resources with a query parameter

The `Articles` class below has the sort string property that acts as a query parameter and allows you to define a resource responding on the following path with the sort query parameter: `/articles?sort=new`.

```kotlin
@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
```
Note that properties can be primitives or types annotated with the `@Serializable` annotation.


