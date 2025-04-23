# Exposed-Option

A library for type-safe application settings management stored in a database via the Exposed ORM.

## Features

- 🔒 **Type Safety**: Work with settings while preserving Kotlin types
- 🧠 **Caching**: Reduce database load with optional value caching
- 🔄 **Property Delegation**: Convenient access to settings via Kotlin property delegates
- 🔧 **Transformers**: Flexible conversion between string representation in DB and typed objects
- 📋 **Required Settings Tracking**: Verification of necessary settings in the database
- 🧮 **Arithmetic Operations**: Increment, decrement, addition and subtraction for numeric options

## Installation

```kotlin
// build.gradle.kts
dependencies {
    implementation("ru.raysmith:exposed-option:3.0.0")
}
```

or if you are using Gradle with a version catalog:
```toml
# libs.versions.toml
[versions]
raysmith-exposed-option = "3.0.0"

[libraries]
exposed-option = { module = "ru.raysmith:exposed-option", version.ref = "raysmith-exposed-option" }
```

## Usage

### Project Setup

```kotlin
transaction {
    // Create settings table if it doesn't exist
    SchemaUtils.create(Options)
}
```
The `SchemaUtils.create(Options)` call creates a database table for storing options if it doesn't already exist.
This is a necessary step before using the library, as all option values will be stored in this table.
This operation only needs to be performed once when the application starts after the database connection is established.

### Basic Examples

```kotlin
// String option with default value
var appName by option<String>("app.name") { getOrSet("Default App Name") }

// Optional (nullable) option
val welcomeMessage by option<String?>("app.welcome_message") { getOrNull() }

// Reading and writing values
println(appName)
appName = "New App Name"
```

### Caching Options

```kotlin
var appName by option<String>("app.name", cacheTime = 1.hours) { 
    getOrSet("Default App Name") 
}
```

Caching reduces database load by storing option values in memory for the specified duration. 
The `cacheTime` parameter determines how long a value remains cached before being fetched from the database again. 
This is particularly useful for frequently accessed options that change infrequently. 
When an option value is updated through the API, the cache is automatically invalidated.

[//]: # (TODO: Add link to utils library with Cachable docs)

### Working with Custom Types

```kotlin
// Option with a specific transformer for a complex type
data class ServerConfig(val host: String, val port: Int)

val serverConfig by option<ServerConfig>(
    key = "app.server_config",
    transformer = transformer(
        unwrap = { "${it.host}:${it.port}" },
        wrap = {
            val (host, port) = it.split(":")
            ServerConfig(host, port.toInt())
        }
    )
) { getOrSet(ServerConfig("localhost", 8080)) }
```

### Arithmetic Operations

```kotlin
// Increment and decrement
val counter by option<Int>("app.counter") { getOrSet(0) }
counter++ // Increases value by 1
counter-- // Decreases value by 1

// Arithmetic operators with assign
val score by option<Double>("game.score") { getOrSet(0.0) }
score += 100.0 // Adds 100 to the value
score *= 10.0 // Multiplies the value by 10

// and even more
```

### Checking Required Settings

```kotlin
// All non-nullable options are automatically tracked
val appId by option<String>("app.id")
val appToken by option<String>("app.token")
val appName by option<String?>("app.name")

// Check that all required settings are set
Options.checkRequired() // Will throw an exception if app.id or app.token options are not set
```
This validation method is especially useful during application startup to ensure all essential configuration is 
available before any business logic executes.

> Be aware that required option tracking happens at delegate instantiation time. 
> Options declared in classes or objects that haven't been initialized yet, or in top-level properties within files
> that haven't been loaded, will not be included when `Options.checkRequired()` is called. 
> To ensure all options are properly tracked, make sure all option-containing classes and files are initialized before
> validation.