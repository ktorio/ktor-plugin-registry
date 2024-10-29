This plugin provides mitigations for cross-site request forgery (CSRF).

There are several ways to prevent CSRF attacks, each with different pros / cons depending on how
your website is structured.  The [OWASP cheatsheet](https://cheatsheetseries.owasp.org/cheatsheets/Cross_Site_Scripting_Prevention_Cheat_Sheet.html)
enumerates the mitigations provided here.

## Usage

```kotlin
install(CSRF) {
    // tests Origin is an expected value
    allowOrigin("http://localhost:8080")
    
    // tests Origin matches Host header
    originMatchesHost()
    
    // custom header checks
    checkHeader("X-CSRF-Token")
}
```