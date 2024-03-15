
The [static](https://ktor.io/docs/serving-static-content.html) plugin provides the ability to serve files, such as stylesheets, scripts, images, and so on.

## Usage

In order to serve the contents from a folder, we need to specify the folder name using the `files` function. The path is always relative to the application path:
```kotlin
routing {
    static("assets") {
        files("css")
    }
}
```
`files("css")` would then allow for any file located in the folder `css` to be served as static content under the given URL pattern, which in this case is `assets`. This means that a request to `/assets/stylesheet.css` would serve the file `/css/stylesheet.css`.

To learn how to serve individual files, embedded application resources, etc., see [Serving static content](https://ktor.io/docs/serving-static-content.html).
