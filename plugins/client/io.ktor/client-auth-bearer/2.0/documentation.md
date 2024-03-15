
Bearer authentication involves security tokens called bearer tokens. As an example, these tokens can be used as a part of OAuth flow to authorize users of your application by using external providers, such as Google, Facebook, Twitter, and so on. You can learn how the OAuth flow might look from the OAuth authorization flow section for a Ktor server.



## Usage



1. Call the `bearer` function inside the `install` block.
   ```kotlin
   import io.ktor.client.*
   import io.ktor.client.engine.cio.*
   import io.ktor.client.plugins.auth.*
   //...
   val client = HttpClient(CIO) {
       install(Auth) {
          bearer {
             // Configure bearer authentication
          }
       }
   }
   ```
   
2. Configure how to obtain the initial access and refresh tokens using the `loadTokens` callback. This callback is intended to load cached tokens from a local storage and return them as the `BearerTokens` instance.

   ```kotlin
   install(Auth) {
       bearer {
           loadTokens {
               // Load tokens from a local storage and return them as the 'BearerTokens' instance
               BearerTokens("abc123", "xyz111")
           }
       }
   }
   ```
   
   The `abc123` access token is sent with each [request](request.md) in the `Authorization` header using the `Bearer` scheme:
   ```HTTP
   GET http://localhost:8080/
   Authorization: Bearer abc123
   ```
   
3. Specify how to obtain a new token if the old one is invalid using `refreshTokens`.

   ```kotlin
   install(Auth) {
       bearer {
           // Load tokens ...
           refreshTokens { // this: RefreshTokensParams
               // Refresh tokens and return them as the 'BearerTokens' instance
               BearerTokens("def456", "xyz111")
           }
       }
   }
   ```
   
   This callback works as follows:
   
   a. The client makes a request to a protected resource using an invalid access token and gets a `401` (Unauthorized) response.
     > If [several providers](auth.md#realm) are installed, a response should have the `WWW-Authenticate` header.
   
   b. The client calls `refreshTokens` automatically to obtain new tokens.

   c. The client makes one more request to a protected resource automatically using a new token this time.

5. Optionally, specify a condition for sending credentials without waiting for the `401` (Unauthorized) response. For example, you can check whether a request is made to a specified host.

   ```kotlin
   install(Auth) {
       bearer {
           // Load and refresh tokens ...
           sendWithoutRequest { request ->
               request.url.host == "www.googleapis.com"
           }
       }
   }
   ```

