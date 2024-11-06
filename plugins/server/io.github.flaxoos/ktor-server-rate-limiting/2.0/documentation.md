# Ktor Server Rate Limiter

![Build Status](https://img.shields.io/github/actions/workflow/status/flaxoos/extra-ktor-plugins/build-and-publish-main.yml?event=push&logo=githubactions&style=for-the-badge)
[![Maven-central](https://img.shields.io/maven-central/v/io.github.flaxoos/ktor-server-task-scheduling?style=for-the-badge&logo=apachemaven)](https://search.maven.org/search?q=io.github.flaxoos%20AND%20ktor)
![Language: Kotlin](https://img.shields.io/github/languages/top/flaxoos/flax-ktor-plugins?color=blue&logo=kotlin&style=for-the-badge)
[![Koverage: 94.42%](https://img.shields.io/badge/94.42-green?logo=kotlin&label=koverage&style=for-the-badge)](file:/Users/ido/IdeaProjects/flax-ktor-plugins/build/reports/kover/html/index.html)
[![Docs](https://custom-icon-badges.demolab.com/badge/Pages-blue.svg?label=Docs&logo=github&logoColor=white?icon=githubpages&style=for-the-badge)](https://github.com/Flaxoos/extra-ktor-plugins/actions/workflows/pages/pages-build-deployment)
[![Awesome Kotlin Badge](https://custom-icon-badges.demolab.com/badge/awesome-kotlin-orange.svg?labelColor=blue&style=for-the-badge)](https://github.com/KotlinBy/awesome-kotlin)

Manage request rate limiting as you see fit with RateLimiting in your Ktor server, ensuring you protect your application from potential abuse or resource overload.

## Features:
### Three Strategies Available:

1) **Token Bucket:** Supports variable request rate and is suitable for handling bursts of requests.

2) **Leaky Bucket:** Guarantees a constant request rate, providing fair distribution between clients

3) **Sliding Window:** Allows a specific weight of calls to be made over a designated duration, considering the rate and call weight configured.

### Robust Configurability

- **Configurable capacity unit of measure**: Measure call count or call weight in bytes

- **Configurable call weighting**: Calls can be made to take up more capacity based on a given function

- **Whitelist & Blacklist**:
  - Whitelist or blacklist based on the client's host, user-agent, or principal.

- **Customizable Response**: Set your custom response when the rate limit is exceeded. The default response status is `429 Too Many Requests`.

- **Logging**: Log rate limit hits for better monitoring and debugging.

## Documentation

[Project Github Pages](https://flaxoos.github.io/extra-ktor-plugins/ktor-server-rate-limiting/)

## Installation:

To apply the `RateLimitingPlugin`, you need to `install` it in your Ktor route and configure as per your requirements:

```kotlin
routing {
  route("limited-route") {
    install(RateLimiting) {
      rateLimiter {
        type = TokenBucket::class
        rate = 1.seconds
        capacity = 100
      }
      whiteListedHosts = setOf("trusted-host.com")
      blackListedAgents = setOf("malicious-agent")
      blackListedCallerCallHandler = { call ->
          call.respond(HttpStatusCode.Forbidden, "You are blacklisted and cannot access the API.")
      }
    }
    
    get {
      call.respondText("Welcome to our limited route!")
    }
  }
  route("unlimited-route") {
    get {
      call.respondText("Welcome to our unlimited route!")
    }
  }
}

```


## Usage

See usage example in the [Example project repository](https://github.com/Flaxoos/flax-ktor-plugins-examples)

### Detailed Configuration Options:

```kotlin
  install(RateLimiting) {
    // Configuring Rate Limiter
      rateLimiter {
          type = TokenBucket::class // Using Token Bucket rate limiting strategy
          rate = 10.milliseconds          // 1 token is added per 10 milliseconds
          capacity = 1000           // Up to 1000 tokens can be held for bursty traffic
          clock = { Clock.System.now().toEpochMilliseconds() }  // Using system time
          callVolumeUnit = CallVolumeUnit.Calls()   // Measuring by the number of API calls
      }

    // Whitelisting Configurations
    whiteListedHosts = setOf("192.168.1.1") // IP address exempted from rate limiting
    whiteListedPrincipals = setOf(Principal("trustedUser")) // Trusted user with unrestricted access
    whiteListedAgents = setOf("trusted-agent") // A user-agent that is allowed unrestricted access

    // Blacklisting Configurations
    blackListedHosts = setOf("192.168.1.2") // IP address completely restricted from API access
    blackListedPrincipals = setOf(Principal("maliciousUser")) // User that is denied access to the API
    blackListedAgents = setOf("malicious-agent") // A user-agent that is blocked from making API calls

    // Handlers
    blackListedCallerCallHandler = { call ->
        // Respond with a 403 Forbidden status to blacklisted callers
        call.respond(HttpStatusCode.Forbidden, "You are blacklisted and cannot access the API.")
    }

    callAcceptedHandler = { notLimited ->
        // Add rate-limiting headers for accepted calls
        response.headers.append("X-RateLimit-Remaining", "${notLimited.remaining}")
        response.headers.append("X-RateLimit-Measured-by", notLimited.rateLimiter.callVolumeUnit.name)
    }

    rateLimitExceededHandler = { limitedBy ->
        // Respond with a 429 status and appropriate headers for rate-limited callers
        respond(HttpStatusCode.TooManyRequests, "Rate limit exceeded: ${limitedBy.message}")
        response.headers.append("X-RateLimit-Limit", "${limitedBy.rateLimiter.capacity}")
        response.headers.append("X-RateLimit-Measured-by", limitedBy.rateLimiter.callVolumeUnit.name)
        response.headers.append("X-RateLimit-Reset", "${limitedBy.resetIn.inWholeMilliseconds}")
    }
}
```

