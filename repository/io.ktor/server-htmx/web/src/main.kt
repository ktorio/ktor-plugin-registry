package kastle

import kotlinx.browser.document
import kotlinx.browser.window

fun main() {
    // htmx reference is needed for import
    console.log("htmx loaded", htmx)

    // event handling for htmx
    var rowCount = 1
    document.addEventListener("htmx:beforeSwap", {
        document.getElementById("total-count")?.innerHTML = "Total: ${(++rowCount) * 10}"
    })
    // Scroll to the bottom of the page after adding content
    document.addEventListener("htmx:afterSwap", {
        val scrollHeight = document.body?.scrollHeight ?: 0
        window.scrollTo(0.0, scrollHeight.toDouble())
    })
}