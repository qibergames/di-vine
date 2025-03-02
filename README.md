# The DiVine Dependency Injection Tool

DiVine is an advanced [dependency injection](https://en.wikipedia.org/wiki/Dependency_injection) tool for Java, 
that is inspired by the design of the TypeScript [typedi](https://github.com/typestack/typedi) library.
<p>
It is designed to be simple to use, and to provide a powerful and flexible way to manage dependencies in your Java applications.

## Purpose

One of the main purposes behind a dependency injection tool, is to minimize the need of manual code initialization.
Rather than spending time on developing the business logic of applications, you have to do a lot of work of making,
passing and deleting instances throughout your entire codebase.
<p>
This is where DiVine comes into place. It minimizes the code for service registration and dependency requests, so you
can have your focus kept on implementing actual logic.

## Documentation

Check out the [documentation](https://divine.qibergames.com/) to learn the basic and advanced usage.

## Installation

You may use the following code to use DiVine in your project.
Check out our [jitpack](https://jitpack.io/#qibergames/di-vine) page for the latest version.

### Maven

```xml
<repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
</repository>
```

```xml
<dependency>
    <groupId>com.github.qibergames</groupId>
    <artifactId>di-vine</artifactId>
    <version>VERSION</version>
</dependency>
```

### Gradle

```gradle
repositories {
    maven { url 'https://jitpack.io' }
}
```

```gradle
dependencies {
    implementation 'com.github.qibergames:di-vine:VERSION'
}
```
