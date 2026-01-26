
The [Digest](https://ktor.io/docs/digest.html) authentication scheme is a part of HTTP framework used for access control and authentication. In this scheme, a hash function is applied to a username and password before sending them over the network.

## Usage

For the Digest scheme, you need to provide a user table that contains usernames and corresponding `HA1` hashes:

```kotlin
val userTable: Map<String, ByteArray> = mapOf(
    "jetbrains" to getMd5Digest("jetbrains:        $myRealm        :foobar"),
    "admin" to getMd5Digest("admin:        $myRealm        :password")
)
```

Then, you can configure the `digestProvider` function that fetches the `HA1` part of digest for a specified username:
```kotlin
install(Authentication) {
    digest("auth-digest") {
        realm = "Access to the '/' path"
        digestProvider { userName, realm ->
            userTable[userName]
        }
    }
}
```
