package kastle

import io.ktor.server.application.Application

private val hasHttp: Boolean by _properties
private val hasMonitoring: Boolean by _properties
private val hasSerialization: Boolean by _properties
private val hasDatabase: Boolean by _properties
private val hasSecurity: Boolean by _properties
private val serverModules: List<String> by _properties

fun Application.rootModule() {
    configureRouting()
    for (serverModule in serverModules) {
        _unsafe<Unit>("${serverModule.substringAfterLast(".")}()")
    }
    if (hasHttp) {
        configureHttp()
    }
    if (hasMonitoring) {
        configureMonitoring()
    }
    if (hasSerialization) {
        configureSerialization()
    }
    if (hasDatabase) {
        configureDatabases()
    }
    if (hasSecurity) {
        configureSecurity()
    }
}
