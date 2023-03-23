import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

plugins {
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.dokka") apply false
    id("io.github.gradle-nexus.publish-plugin")
    id("com.github.ben-manes.versions")
    id("org.jlleitschuh.gradle.ktlint") apply false
    `maven-publish`
    `signing`
}

allprojects {
    group = "org.radarbase"
    version = "1.2.1-SNAPSHOT"
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "org.jetbrains.dokka")
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
    apply(plugin = "maven-publish")
    apply(plugin = "signing")

    val myProject = this

    val githubRepoName = "RADAR-base/radar-spring"
    val githubUrl = "https://github.com/$githubRepoName.git"
    val githubIssueUrl = "https://github.com/$githubRepoName/issues"

    repositories {
        mavenCentral()
        mavenLocal()
    }

    // Ensure that dokka does not use vulnerable packages
    dependencies {
        val dokkaVersion: String by project
        configurations["dokkaHtmlPlugin"]("org.jetbrains.dokka:kotlin-as-java-plugin:$dokkaVersion")

        val jacksonVersion: String by project
        val jsoupVersion: String by project
        val kotlinVersion: String by project

        sequenceOf("dokkaPlugin", "dokkaRuntime")
            .map { configurations[it] }
            .forEach { conf ->
                conf(platform("com.fasterxml.jackson:jackson-bom:$jacksonVersion"))
                conf("org.jsoup:jsoup:$jsoupVersion")
                conf(platform("org.jetbrains.kotlin:kotlin-bom:$kotlinVersion"))
            }
    }

    val sourcesJar by tasks.registering(Jar::class) {
        from(myProject.the<SourceSetContainer>()["main"].allSource)
        archiveClassifier.set("sources")
        description = "Main source Jar for the application"
        val classes by tasks
        dependsOn(classes)
    }

    val dokkaJar by tasks.registering(Jar::class) {
        from("$buildDir/dokka/javadoc")
        archiveClassifier.set("javadoc")
        description = "Assembles Kotlin docs with Dokka"
        val dokkaJavadoc by tasks
        dependsOn(dokkaJavadoc)
    }

    tasks.withType<JavaCompile> {
        options.release.set(17)
    }

    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17"
            apiVersion = "1.8"
            languageVersion = "1.8"
        }
    }

    afterEvaluate {
        configurations.all {
            resolutionStrategy.cacheChangingModulesFor(0, TimeUnit.SECONDS)
            resolutionStrategy.cacheDynamicVersionsFor(0, TimeUnit.SECONDS)
        }

        tasks.withType<Test> {
            testLogging {
                events("passed", "skipped", "failed")
                showStandardStreams = true
                exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
            }
            useJUnitPlatform()
            systemProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager")
        }

        tasks.withType<Tar> {
            compression = Compression.GZIP
            archiveExtension.set("tar.gz")
        }

        tasks.withType<Jar> {
            manifest {
                attributes(
                    "Implementation-Title" to myProject.name,
                    "Implementation-Version" to myProject.version
                )
            }
        }

        val assemble by tasks
        assemble.dependsOn(sourcesJar)
        assemble.dependsOn(dokkaJar)

        publishing {
            publications {
                create<MavenPublication>("mavenJar") {
                    afterEvaluate {
                        from(components["java"])
                    }
                    artifact(sourcesJar)
                    artifact(dokkaJar)

                    pom {
                        name.set(myProject.name)
                        description.set(myProject.description)
                        url.set(githubUrl)
                        licenses {
                            license {
                                name.set("The Apache Software License, Version 2.0")
                                url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
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
                        issueManagement {
                            system.set("GitHub")
                            url.set(githubIssueUrl)
                        }
                        organization {
                            name.set("RADAR-base")
                            url.set("https://radar-base.org")
                        }
                        scm {
                            connection.set("scm:git:$githubUrl")
                            url.set(githubUrl)
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

        tasks.withType<Sign> {
            onlyIf { gradle.taskGraph.hasTask(myProject.tasks["publish"]) }
        }
    }
}

fun Project.propertyOrEnv(propertyName: String, envName: String): String? {
    return if (hasProperty(propertyName)) {
        property(propertyName)?.toString()
    } else {
        System.getenv(envName)
    }
}

nexusPublishing {
    repositories {
        sonatype {
            username.set(propertyOrEnv("ossrh.user", "OSSRH_USER"))
            password.set(propertyOrEnv("ossrh.password", "OSSRH_PASSWORD"))
        }
    }
}

tasks.named<DependencyUpdatesTask>("dependencyUpdates") {
    val regex = "RELEASE|FINAL|GA|-jre|^[0-9,.v-]+(-r)?$".toRegex(RegexOption.IGNORE_CASE)
    rejectVersionIf {
        !regex.containsMatchIn(candidate.version)
    }
}

tasks.wrapper {
    gradleVersion = "8.0.2"
}
