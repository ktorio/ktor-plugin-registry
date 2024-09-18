Authentication provider for Firebase Auth module.


## Usage

You need to setup [Firebase](https://firebase.google.com/) project
with [Authentication module](https://firebase.google.com/products/auth) enabled. See [sample project](https://github.com/krizzu/firebase-auth-provider/blob/main/sample/README.md) to learn more.

```kotlin
install(Authentication) {
    firebase("my-auth") {
        adminFile = File("path/to/admin/file.json")
        realm = "Sample Server"

        /**
         * A decoded and verified Firebase token.
         * Can be used to get the uid and other user attributes available in the token.
         */
        validate { token ->
            UserIdPrincipal(token.uid)
        }
    }
}
```

## API

| **Param** | **Required** | **Description**                                                                                                                                                                                                                                            |
|-----------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| adminFile | Required     | [File](https://docs.oracle.com/javase/8/docs/api/java/io/File.html) instance, pointing to your Service account for your Firebase project. See [sample project](https://github.com/krizzu/firebase-auth-provider/blob/main/sample/README.md) to learn more. |
| validate  | Required     | Lambda receiving decoded and verified [FirebaseToken](https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/auth/FirebaseToken), expected to return Principal, if user is authorized or null otherwise.                      |
| realm     | Optional     | String describing the protected area or the scope of protection. This could be a message like "Access to the staging site" or similar, so that the user knows to which space they are trying to get access to. Defaults to "Ktor Server"                   |
