This plugin provides introduces [HTMX](https://htmx.org) to your web module, and a set of extensions
for templating HTMX from your server using the [Kotlin HTML DSL](https://github.com/Kotlin/kotlinx.html).

## Usage

Add [HTMX attributes](https://htmx.org/reference/#attributes) to your elements using the HTML DSL like this:

```kotlin
button {
    attributes["hx-get"]     = "/more-rows"
    attributes["hx-target"]  = "#replaceMe"
    attributes["hx-swap"]    = "outerHTML"
    attributes["hx-trigger"] = "click"
    attributes["hx-select"]  = "tr"
}
```

From your web module, you can handle [HTMX events](https://htmx.org/reference/#events) using Kotlin WASM source code:

```kotlin
document.body?.apply {
    var rowCount = 1
    // Update the total and scroll to the bottom of the page after adding content
    addEventListener("htmx:afterSwap") {
        document.getElementById("total-count")?.innerHTML = "Total: ${(++rowCount) * 10}"
        window.scrollTo(0.0, scrollHeight.toDouble())
    }
}
```