import org.jetbrains.kotlin.gradle.tasks.throwGradleExceptionIfError

plugins {
    id("kotlin-kapt")
    id("maven-publish")
    id("signing")
}

group = "org.radarbase"
version = parent?.version
    ?: throwGradleExceptionIfError(org.jetbrains.kotlin.cli.common.ExitCode.SCRIPT_EXECUTION_ERROR)
description =
    "This library provides functionality to add authorization in any spring based application."

java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/radar-cns/org.radarcns")
}

// Dependecy versions
val mpVersion = "0.7.1"
val springVersion = "5.2.4.RELEASE"
val slf4jVersion = "1.7.30"
val aspectJVersion = "1.9.5"
val javaXServletVersion = "2.5"

dependencies {
    api(group = "org.radarbase", name = "radar-auth", version = mpVersion)
    api(group = "org.slf4j", name = "slf4j-api", version = slf4jVersion)
    implementation(group = "org.springframework", name = "spring-web", version = springVersion)
    implementation(group = "org.springframework", name = "spring-context", version = springVersion)
    compileOnly(group = "org.aspectj", name = "aspectjweaver", version = aspectJVersion)
    implementation(group = "javax.servlet", name = "servlet-api", version = javaXServletVersion)
}

tasks.compileKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.compileTestKotlin {
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

tasks.dokka {
    outputFormat = "html"
    outputDirectory = "$buildDir/javadoc"
}

val sourcesJar by tasks.creating(Jar::class) {
    from(tasks.jar)
    description = "Main source Jar for the application"
    archiveClassifier.set("sources")
}


val dokkaJar by tasks.creating(Jar::class) {
    group = JavaBasePlugin.DOCUMENTATION_GROUP
    description = "Assembles Kotlin docs with Dokka"
    archiveClassifier.set("javadoc")
    from(tasks.dokka)
}

// Properties for publishing
val githubRepoName = "RADAR-base/radar-spring"
val githubUrl = "https://github.com/${githubRepoName}"
val website = "https://radar-base.org"

publishing {
    publications {
        create<MavenPublication>("mavenJar") {
            afterEvaluate {
                from(components["java"])
            }
            setArtifacts(arrayListOf(dokkaJar, sourcesJar))
            pom {
                name.set(project.name)
                description.set(project.description)
                url.set(githubUrl)
                licenses {
                    license {
                        name.set("The Apache Software License, Version 2.0")
                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                        distribution.set("repo")
                    }
                }
                developers {
                    developer {
                        id.set("yatharthranjan")
                        name.set("Yatharth Ranjan")
                        email.set("yatharthranjan89@gmail.com")
                    }
                }
                scm {
                    connection.set("scm:git:git://github.com/$githubRepoName.git")
                    developerConnection.set("scm:git:ssh://github.com/$githubRepoName.git")
                    url.set(githubUrl)
                }
                issueManagement {
                    system.set("GitHub")
                    url.set("$githubUrl/issues")
                }
                organization {
                    name.set("RADAR-base")
                    url.set(website)
                }
            }
        }
    }
}

signing {
    useGpgCmd()
    isRequired = true
    sign(tasks["sourcesJar"], tasks["dokkaJar"])
    sign(publishing.publications["mavenJar"])
}

tasks.withType<Sign>().configureEach {
    onlyIf { gradle.taskGraph.hasTask("${project.path}:publish") }
}

tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }
}
