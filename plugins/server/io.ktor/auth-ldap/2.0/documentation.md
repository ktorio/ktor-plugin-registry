
[LDAP](https://ktor.io/docs/ldap.html) is a protocol for working with various directory services that can store information about users. Ktor allows you to authenticate LDAP users using the Basic, Digest, or Form-based authentications schemes.

## Usage

To authenticate an LDAP user, you need to call the `ldapAuthenticate` function. Configuration with the Basic provider might look as follows:
```kotlin
install(Authentication) {
    basic("auth-ldap") {
        validate { credentials ->
            ldapAuthenticate(credentials, "ldap://0.0.0.0:389", "cn=%s,dc=ktor,dc=io")
        }
    }
}
```
