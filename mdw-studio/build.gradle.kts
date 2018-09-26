import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.intellij.tasks.PublishTask

plugins {
    kotlin("jvm") version "1.2.61"
    id("org.jetbrains.intellij") version "0.3.7"
}

group = "com.centurylink.mdw"
version = "2018.1.3-SNAPSHOT"

java.sourceSets {
    "main" {
        java.srcDirs("src")
        // kotlin.sourceDirs("src")
        resources.srcDirs("resources")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    compile(project(":mdw-draw"))
    compile("com.beust:jcommander:1.72")
    compile("org.eclipse.jgit:org.eclipse.jgit:4.8.0.201706111038-r") { isTransitive = false }
    compile("org.yaml:snakeyaml:1.18")
    compile(files("lib/bpmn-schemas.jar"))
    compile("io.swagger:swagger-codegen-cli:2.3.1") { exclude(group = "org.slf4j") }
}

intellij {
    version = "2018.2.3"
}

tasks.withType<PublishTask> {
    username(project.properties["intellijPublishUsername"] ?: "")
    password(project.properties["intellijPublishPassword"] ?: "")
    channels(project.properties["intellijPublishChannel"] ?: "Beta")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
