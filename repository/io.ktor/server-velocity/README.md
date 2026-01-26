
Ktor allows you to use Velocity templates as views within your application by installing the [Velocity](https://ktor.io/docs/velocity.html) plugin.

## Usage

Inside the `install` block, you can configure the `VelocityEngine`. For example, if you want to use templates from the classpath, use a resource loader for `classpath`:
```kotlin
install(Velocity) {
    setProperty(RuntimeConstants.RESOURCE_LOADER, "classpath")
    setProperty("classpath.resource.loader.class", ClasspathResourceLoader::class.java.name)
}
```

To learn more, see [Velocity](https://ktor.io/docs/velocity.html).
