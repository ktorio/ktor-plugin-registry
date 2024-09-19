When developing websites for Ktor, you can enable browser refreshing whenever project files change.  This allows for rapid feedback when making changes to your codebase.

The plugin works by injecting a small script in outbound HTML documents that will wait for refresh messages from the server.

# Usage

When the plugin is installed and `developmentMode` is enabled, a GET endpoint will be created that keeps connections open until the server is refreshed.

To configure your project for auto-reload, follow the directions found here: [https://ktor.io/docs/server-auto-reload.html](https://ktor.io/docs/server-auto-reload.html)