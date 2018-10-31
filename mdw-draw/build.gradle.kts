import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
}

group = "com.centurylink.mdw"
version = "1.1.3-SNAPSHOT"

java.sourceSets {
    "main" {
        java.srcDirs("src")
        resources.srcDirs("resources")
    }
}

repositories {
    mavenCentral()
    maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots") }
}

dependencies {
    compile(kotlin("stdlib-jdk8"))
    compile(kotlin("reflect"))
    compile("com.centurylink.mdw:mdw-common:6.1.10-SNAPSHOT") { isTransitive = false }
    //compile(files("../../mdw/mdw/deploy/app/mdw-common-6.1.10-SNAPSHOT.jar"))
    compile("io.limberest:limberest:1.2.4") { isTransitive = false }
    compile("org.json:json:20180130")
    compile("com.google.code.gson:gson:2.8.5")
    compile("com.vladsch.flexmark:flexmark:0.32.22")
    compile("com.vladsch.flexmark:flexmark-util:0.32.22")
    compile("com.vladsch.flexmark:flexmark-ext-anchorlink:0.32.22")
    compile("com.vladsch.flexmark:flexmark-ext-autolink:0.32.22")
    compile("com.vladsch.flexmark:flexmark-ext-superscript:0.32.22")
    compile("com.vladsch.flexmark:flexmark-ext-tables:0.32.22")
    compile("com.vladsch.flexmark:flexmark-ext-typographic:0.32.22")
    compile("com.vladsch.flexmark:flexmark-formatter:0.32.22")
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}
