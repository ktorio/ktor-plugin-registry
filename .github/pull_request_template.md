Thanks for contributing to Ktor's plugin registry!

Before submitting your pull request, be sure to run the following:

1. `./gradlew buildRegistry`: this tests and builds the expected model for the project generator using the plugin manifests
2. `./gradlew buildTestProject`: this generates a sample project using the modified plugin files

We encourage you to experiment with the different options listed here ([manifest.ktor.yaml](https://github.com/ktorio/ktor-plugin-registry/blob/main/templates/manifest.ktor.yaml)) 
and explore the results in the generated test project.

Feel free to reach out with any issues with the registry or generator in a pull request or any of the [channels listed here](https://ktor.io/support/).
