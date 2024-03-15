
The [HSTS](https://ktor.io/docs/hsts.html) plugin adds the required _HTTP Strict Transport Security_ headers to a request. When the browser receives HSTS policy headers, it will no longer attempt to connect to the server with insecure connections for the given period of time.

## Usage

To install `HSTS`, pass it to the `install` function:
```kotlin
install(HSTS)
```
## Options

* `maxAge` (default is one year): duration to tell the client to keep the host in a list of known HSTS hosts.
* `includeSubDomains` (default is `true`): adds `includeSubDomains` directive, which applies this policy to this domain and any subdomains.
* `preload` (default is `false`): consents that the policy allows including the domain into the web browser's preloading list.
* `customDirectives` (default is empty): any custom directives supported by a specific user-agent.
