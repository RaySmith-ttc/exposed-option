import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.9.10"
    `maven-publish`
    signing
    id("com.gradleup.nmcp") version "0.0.7"
}

group = "ru.raysmith"
version = "1.7"

repositories {
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
    val exposedVersion = "0.44.1"
    api("org.jetbrains.exposed:exposed-core:$exposedVersion")
    api("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("ru.raysmith:utils:2.2.0")

    testImplementation("io.kotest:kotest-runner-junit5-jvm:5.7.2")
    testImplementation("com.h2database:h2:2.2.220")
    testImplementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
}

java {
    withSourcesJar()
    withJavadocJar()
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "1.8"
        }
    }

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
                name.set("Google")
                url.set("https://github.com/RaySmith-ttc/google")
                description.set("DSL wrappers for java google libs")



                licenses {
                    license {
                        name.set("The Apache License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                    }
                }

                scm {
                    connection.set("scm:https://github.com/RaySmith-ttc/google.git")
                    developerConnection.set("scm:git@github.com:RaySmith-ttc/google.git")
                    url.set("https://github.com/RaySmith-ttc/google")
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
        publicationType.set("USER_MANAGED")
        publicationType.set("AUTOMATIC")
    }
}

signing {
    sign(configurations.archives.get())
    sign(publishing.publications["release"])
}