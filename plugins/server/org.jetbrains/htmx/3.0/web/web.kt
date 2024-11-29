import kotlinx.browser.document
import kotlinx.browser.window

fun main() {
    document.body?.apply {
        var rowCount = 1
        // Update the total count of items
        addEventListener("htmx:beforeSwap") {
            document.getElementById("total-count")?.innerHTML = "Total: ${(++rowCount) * 10}"
        }
        // Scroll to the bottom of the page after adding content
        addEventListener("htmx:afterSwap") {
            window.scrollTo(0.0, scrollHeight.toDouble())
        }
    }
}