import io.ktor.server.application.*
import com.sksamuel.cohort.*
import com.sksamuel.cohort.cpu.*
import com.sksamuel.cohort.memory.*
import kotlinx.coroutines.Dispatchers
import kotlin.time.Duration.Companion.seconds

// The contents of the `install` function will be used for the project template
public fun Application.install() {

    val healthchecks = HealthCheckRegistry(Dispatchers.Default) {
        register(FreememHealthCheck.mb(250), 10.seconds, 10.seconds)
        register(ProcessCpuHealthCheck(0.8), 10.seconds, 10.seconds)
    }

    install(Cohort) {

        // enable an endpoint to display operating system name and version
        operatingSystem = true

        // enable runtime JVM information such as vm options and vendor name
        jvmInfo = true

        // show current system properties
        sysprops = true

        // enable an endpoint to dump the heap in hprof format
        heapDump = true

        // enable an endpoint to dump threads
        threadDump = true

        // set to true to return the detailed status of the healthcheck response
        verboseHealthCheckResponse = true

        // enable healthchecks for kubernetes
        healthcheck("/health", healthchecks)
    }
}
