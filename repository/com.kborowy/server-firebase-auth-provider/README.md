Ktor authentication provider for Firebase Auth module.


## Usage

A [Firebase](https://firebase.google.com/) project
with [Authentication module](https://firebase.google.com/products/auth) is required.
See [sample project](./sample/README.md) to learn more.

```kotlin
install(Authentication) {
    firebase("my-auth") {
        realm = "My Server"

        setup {
            // required, see configuration below 
        }

        validate { token ->
            UserIdPrincipal(token.uid)
        }
    }
}
```

## Configuration

The authentication provider requires initialization of a FirebaseApp instance. You can either:

1. Provide an existing FirebaseApp instance, or

2. Initialize a new instance using a service account file

### Existing FirebaseApp instance

Provide a pre-configured FirebaseApp instance:

```kotlin
firebase("auth-firebase") {
    setup {
        firebaseApp = FirebaseApp.getInstance()
    }
}
```

### Initialize FirebaseApp with admin file

Provide a service account JSON file OR an `InputStream` containing the service account credentials.
Optionally specify a `firebaseAppName` to identify your Firebase instance.

```kotlin
firebase(name = "auth-firebase", firebaseAppName = "my-fb-app") {
    setup {
        // Choose one initialization method:

        adminFile = File("path/to/admin/file.json")
        // OR
        adminFileStream = myFile.inputStream()
    }
}

```

### Authentication Parameters

| **Param** | **Required** | **Description**                                                                                                                                                                                                                          |
|-----------|--------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| validate  | Required     | Lambda receiving decoded and verified [FirebaseToken](https://firebase.google.com/docs/reference/admin/java/reference/com/google/firebase/auth/FirebaseToken), expected to return Principal, if user is authorized or null otherwise.    |
| realm     | Optional     | String describing the protected area or the scope of protection. This could be a message like "Access to the staging site" or similar, so that the user knows to which space they are trying to get access to. Defaults to "Ktor Server" |
