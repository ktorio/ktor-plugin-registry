
[CSS DSL](https://ktor.io/docs/css-dsl.html) extends HTML DSL and allows you to author stylesheets in Kotlin by using the `kotlin-css` wrapper.

## Usage

Serving CSS for a specific route might look as follows:
```kotlin
get("/styles.css") {
    call.respondCss {
        body {
            backgroundColor = Color.darkBlue
            margin(0.px)
        }
        rule("h1.page-title") {
            color = Color.white
        }
    }
}
```

You can learn more from [CSS DSL](https://ktor.io/docs/css-dsl.html).
