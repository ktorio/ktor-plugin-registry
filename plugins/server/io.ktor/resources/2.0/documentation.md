
Ktor provides a mechanism to create routes in a typed way for both constructing URLs and reading the parameters. This functionality is provided by the [Resources](https://ktor.io/docs/type-safe-routing.html) plugin.

## Usage

You can install the `Resources` plugin in the following way: 
```kotlin
install(Resources)
```
For each typed route you want to handle, you need to create a class containing the parameters that you want to handle. To accomplish this, you need to annotate this class using the `@Resource` keyword. Such classes should also have the `@Serializable` annotation provided by the kotlinx.serialization library. For example:
```kotlin
@Serializable
@Resource("/articles")
class Articles(val sort: String? = "new")
```
To learn more about how to define route classes and route handlers, see the [Type-safe routing](https://ktor.io/docs/type-safe-routing.html) section.
