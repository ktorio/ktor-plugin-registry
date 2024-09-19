import kotlinx.browser.window

fun main() {
    // Use Kotlin's `js` function to require the library and assign it to `window`
    window.asDynamic().htmx = js("require('htmx.org')")
}