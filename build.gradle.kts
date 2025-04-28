plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.nmcp)
    alias(libs.plugins.benManes.versions)
    `maven-publish`
    signing
}

group = "ru.raysmith"
version = "3.0.2"

repositories {
    mavenLocal()
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/raysmith-ttc/utils")
        credentials {
            username = System.getenv("GIT_USERNAME")
            password = System.getenv("GIT_TOKEN_READ")
        }
    }
}

dependencies {
    api(libs.exposed.core)
    implementation(libs.raysmith.utils)
    implementation(kotlin("reflect"))

    testImplementation(libs.kotest)
    testImplementation(libs.h2)
    testImplementation(libs.exposed.jdbc)
    testImplementation(libs.logback)
}

java {
    withSourcesJar()
    withJavadocJar()
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
    }
}

tasks {
    test {
        useJUnitPlatform()
    }
}

publishing {
    publications {
        create<MavenPublication>("release") {
            artifactId = project.name
            groupId = project.group.toString()
            version = project.version.toString()
            from(components["java"])
            pom {
                packaging = "jar"
                name.set("exposed-option")
                url.set("https://github.com/RaySmith-ttc/exposed-option")
                description.set("Delegate for key-value table for kotlin exposed")

                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:https://github.com/RaySmith-ttc/exposed-option.git")
                    developerConnection.set("scm:git@github.com:RaySmith-ttc/exposed-option.git")
                    url.set("https://github.com/RaySmith-ttc/exposed-option")
                }

                developers {
                    developer {
                        id.set("RaySmith-ttc")
                        name.set("Ray Smith")
                        email.set("raysmith.ttcreate@gmail.com")
                    }
                }
            }
        }
    }
    repositories {
        maven {
            name = "OSSRH"
            val releasesUrl = uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            val snapshotsUrl = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            url = if (version.toString().matches(".*(SNAPSHOT|rc.\\d+)".toRegex())) snapshotsUrl else releasesUrl
            credentials {
                username = System.getenv("SONATYPE_USER")
                password = System.getenv("SONATYPE_PASS")
            }
        }
    }
}

nmcp {
    publish("release") {
        username.set(System.getenv("CENTRAL_SONATYPE_USER"))
        password.set(System.getenv("CENTRAL_SONATYPE_PASS"))
        publicationType.set("AUTOMATIC")
    }
}

signing {
    sign(configurations.archives.get())
    sign(publishing.publications["release"])
}