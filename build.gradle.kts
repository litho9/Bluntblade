import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.id
import com.google.protobuf.gradle.protobuf
import com.google.protobuf.gradle.protoc
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("com.google.protobuf") version "0.8.18" // Apply the protobuf auto generator
    kotlin("jvm") version "1.7.10"
    kotlin("plugin.serialization") version "1.4.0"
    idea
    application
}

group = "me.blunt"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
//    implementation(fileTree(mapOf("dir" to "lib", "include" to listOf("*.jar"))))
    implementation(files("/lib/kcp.jar"))

    implementation("org.slf4j:slf4j-api:1.7.36")
    implementation("ch.qos.logback:logback-core:1.2.11")
    implementation("ch.qos.logback:logback-classic:1.2.11")
    implementation("io.netty:netty-all:4.1.79.Final")

    implementation("org.litote.kmongo:kmongo-serialization:4.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.3.3")

    implementation("com.google.protobuf:protobuf-kotlin:3.21.1")
    implementation("com.google.protobuf:protobuf-java:3.21.1")
    testImplementation(kotlin("test"))
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.21.1"
    }
    // Enable Kotlin generation
    generateProtoTasks {
        all().forEach {
            it.builtins {
                id("kotlin")
            }
        }
    }
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "17"
}

application {
    mainClass.set("MainKt")
}