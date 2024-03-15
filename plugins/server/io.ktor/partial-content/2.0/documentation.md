
The [PartialContent](https://ktor.io/docs/partial-content.html) plugin adds support for handling requests with the `Range` header. It intercepts the generated response adding the `Accept-Ranges` and the `Content-Range` header and slicing the served content when required. `PartialContent` is well-suited for streaming content or resuming partial downloads with download managers or in unreliable networks.

## Usage

The configuration below allows you to specify the maximum number of ranges that will be accepted from an HTTP request. If an HTTP request specifies more ranges, they will be merged into a single range.
```kotlin
install(PartialContent) {
    maxRangeCount = 10
}
```
