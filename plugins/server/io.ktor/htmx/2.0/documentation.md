Create a project using [htmx](https://htmx.org).

This plugin uses the `kotlinx-html` DSL for templating responses, and provides extra support for various HTMX features.

## Usage

Inserting HTMX tags into your HTML DSL:

```kotlin
button {
    attributes.hx {
        get = "/?page=$nextPage"
        target = Target { "#replaceMe" }
        swap = HxSwap.outerHTML
        trigger = "click[console.log('Hello!')||true]"
    }

    +"Load More Agents..."
}
```

Reading HTMX headers:

```kotlin
if (call.request.hx.isBoosted) {
    call.respondHtmlFragment { 
        div {
            id = "replaceMe"
            +"Loading..."
        }
    }
}
```

Routing for HTMX endpoints:

```kotlin
hx.get("/", target = Id("replaceMe")) {
    val page = requireNotNull(call.parameters["page"]).toInt()
    require(page > 0) { "Page must be greater than 0" }

    log.info("Current URL: ${call.request.hx.currentUrl.toString()}")
    call.respondHtmlFragment {
        AgentsRowsPage(AgentRepository.loadAgents(page), nextPage = page + 1)
    }
}
```

