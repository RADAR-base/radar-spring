plugins {
    id("kotlin-kapt")
    id("maven-publish")
}

group = "org.radarbase"
version = "1.0.0-SNAPSHOT"

java.sourceCompatibility = JavaVersion.VERSION_1_8

val mpVersion = "0.5.8"
val springVersion = "5.2.4.RELEASE"
val slf4jVersion = "1.7.30"
val aspectJVersion = "1.9.5"
val javaXServletVersion = "2.5"

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/radar-cns/org.radarcns")
}

dependencies {
    api(group = "org.radarcns", name = "radar-auth", version = mpVersion)
    api(group = "org.slf4j", name = "slf4j-api", version = slf4jVersion)
    compile(group = "org.springframework", name = "spring-web", version = springVersion)
    compile(group = "org.springframework", name = "spring-context", version = springVersion)
    compileOnly(group = "org.aspectj", name = "aspectjweaver", version = aspectJVersion)
    implementation(group = "javax.servlet", name = "servlet-api", version = javaXServletVersion)
}

tasks.compileKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.compileTestKotlin {
    kotlinOptions.jvmTarget = "1.8"
}

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    classifier = "javadoc"
    from(tasks.dokka)
}

publishing {
    publications {
        create<MavenPublication>("default") {
            from(components["java"])
            setArtifacts(arrayListOf(dokkaJar, tasks.jar.get()))
        }
    }
    repositories {
        maven {
            url = uri("$buildDir/repository")
        }
    }
}
