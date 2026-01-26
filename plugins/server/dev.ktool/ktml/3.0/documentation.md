A blazingly fast, type-safe HTML template engine for Kotlin JVM and Multiplatform that transforms .ktml templates into optimized Kotlin functions.

## Usage

### 1. Create a Custom Tag Component

Custom tags let you build reusable components with type-safe parameters that can be included in other templates:

```html
<!-- card.ktml -->
<card header="${Content? = null}" content="$Content">
    <div class="card">
        <div if="${header != null}" class="card-header">
            $header
        </div>
        <div class="card-body">
            $content
        </div>
    </div>
</card>
```

### 2. Create a Full Page Template

Pages use `<html>` as the root and can be rendered from a controller. A template can import types from your code and use
values from a context model. To pull a value from the context model into the template you prefix the attribute name on
the root template tag with a `@`.

```html
<!-- dashboard.ktml -->
<!DOCTYPE html>

import com.myapp.User

<html lang="en" @user="$User">
<head>
    <title>Dashboard</title>
</head>
<body>
<card>
    <header><h2>Welcome, ${user.name}!</h2></header>
    <p if="${user.type == UserType.ADMIN}">You have admin privileges</p>
</card>
</body>
</html>
```

### 3. Use in Your Application

With integrations for Spring MVC, Ktor, and Javalin, KTML works like other template engines. The Gradle plugin will
automatically generate and compile the code from your templates.

Here's an example using Ktor:

```kotlin
fun main() {
    embeddedServer(CIO, port = 8080, host = "0.0.0.0") {
        install(KtmlPlugin)
        configureRouting()
    }.start(wait = true)
}

fun Application.configureRouting() {
    routing {
        get("/") {
            call.respondKtml(path = "dashboard", model = mapOf("user" to User()))
        }
    }
}
```

Check out other [example applications here](https://github.com/ktool-dev/ktml/tree/main/applications)!