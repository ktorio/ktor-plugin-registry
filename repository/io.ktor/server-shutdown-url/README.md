
The [Shutdown URL](https://ktor.io/docs/shutdown-url.html) plugin provides the ability to shut down a server when accessing a specified URL.

## Usage

To specify a shutdown URL in a configuration file, use the `ktor.deployment.shutdown.url` property:

```
ktor {
    deployment {
        shutdown.url = "/my/shutdown/path"
    }
}
```

Learn more from [Shutdown URL](https://ktor.io/docs/shutdown-url.html).
