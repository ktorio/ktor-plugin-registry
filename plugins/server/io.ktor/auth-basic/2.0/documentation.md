
Ktor supports two authentication schemes with the user and raw password as credentials:
* The [Basic](https://ktor.io/docs/basic.html) scheme is a part of HTTP framework used for access control and authentication. In this scheme, user credentials are transmitted as username/password pairs encoded using Base64.
* [Form-based](https://ktor.io/docs/form.html) authentication uses a web form to collect credential information and authenticate a user.

## Usage

A configured Basic provider might look as follows:
```kotlin
install(Authentication) {
    basic("auth-basic") {
        realm = "Access to the '/' path"
        validate { credentials ->
            if (credentials.name == "jetbrains" && credentials.password == "foobar") {
                UserIdPrincipal(credentials.name)
            } else {
                null
            }
        }
    }
}
```

Form-based authentication also requires parameter names used to fetch a username and password:
```kotlin
install(Authentication) {
    form("auth-form") {
        userParamName = "username"
        passwordParamName = "password"
        validate { credentials ->
            if (credentials.name == "jetbrains" && credentials.password == "foobar") {
                UserIdPrincipal(credentials.name)
            } else {
                null
            }
        }
    }
}
```
