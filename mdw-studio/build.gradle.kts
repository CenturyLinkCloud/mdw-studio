import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.intellij.tasks.PublishTask
import org.jetbrains.intellij.tasks.RunIdeTask

plugins {
    id("org.jetbrains.kotlin.jvm") version "1.3.61"
    id("org.jetbrains.intellij") version "0.4.9"
}

group = "com.centurylink.mdw"
version = "2.2.4-SNAPSHOT"

sourceSets.main {
    withConvention(KotlinSourceSet::class) {
        kotlin.srcDirs("src")
    }
    resources.srcDirs("resources")
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    compile(project(":mdw-draw"))
    compile("com.beust:jcommander:1.72")
    compile("org.eclipse.jgit:org.eclipse.jgit:4.8.0.201706111038-r") { isTransitive = false }
    compile("org.yaml:snakeyaml:1.18")
    compile(files("lib/bpmn-schemas.jar"))
    compile("io.swagger:swagger-codegen-cli:2.3.1") { exclude(group = "org.slf4j") }
}

intellij {
    version = "2020.3" // or like "192.5728-EAP-CANDIDATE-SNAPSHOT"
    setPlugins("java", "git4idea", "YAML")  // "Kotlin"
}

tasks.withType<RunIdeTask> {
    jvmArgs = listOf("-Xmx1G") //, "-XX:CICompilerCount=2")
    // available jbrs here: https://jetbrains.bintray.com/intellij-jdk/
    // temp due to this: https://youtrack.jetbrains.com/issue/JBR-1928
    // jbrVersion("11_0_5b665.1")
}

tasks.withType<PublishTask> {
    username(project.properties["intellijPublishUsername"] ?: "")
    password(project.properties["intellijPublishPassword"] ?: "")
    channels(project.properties["intellijPublishChannel"] ?: "EAP")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
val compileKotlin: KotlinCompile by tasks
compileKotlin.kotlinOptions {
    jvmTarget = "1.8"
}
val compileTestKotlin: KotlinCompile by tasks
compileTestKotlin.kotlinOptions {
    jvmTarget = "1.8"
}