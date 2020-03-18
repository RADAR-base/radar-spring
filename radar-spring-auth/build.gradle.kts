plugins {
    id("kotlin-kapt")
    id("maven-publish")
    id("com.jfrog.bintray") version ("1.8.4")
}

group = "org.radarbase"
version = "1.0.0-SNAPSHOT"
description =
    "This library provides functionality to add authorization in any spring based application."

java.sourceCompatibility = JavaVersion.VERSION_1_8

repositories {
    mavenCentral()
    maven("https://dl.bintray.com/radar-cns/org.radarcns")
}

// Dependecy versions
val mpVersion = "0.5.8"
val springVersion = "5.2.4.RELEASE"
val slf4jVersion = "1.7.30"
val aspectJVersion = "1.9.5"
val javaXServletVersion = "2.5"

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

val wrapper by tasks.creating(Wrapper::class) {
    gradleVersion = "6.0.1"
}

// Properties for publishing
val githubRepoName = "RADAR-base/radar-spring"
val githubUrl = "https://github.com/${githubRepoName}.git"
val website = "https://radar-base.org"

publishing {
    publications {
        create<MavenPublication>("mavenJar") {
            from(components["java"])
            setArtifacts(arrayListOf(dokkaJar, tasks.jar.get()))
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
    repositories {
        maven {
            url = uri("$buildDir/repository")
        }
    }
}

bintray {
    user =
        (project.properties["bintrayUser"] ?: System.getenv("BINTRAY_USER"))?.toString()
    key =
        (project.properties["bintrayApiKey"] ?: System.getenv("BINTRAY_API_KEY"))?.toString()
    override = false
    setPublications("mavenJar")
    with(pkg) {
        repo = project.group as String?
        name = project.name
        userOrg = "radar-base"
        desc = project.description
        setLicenses("Apache-2.0")
        websiteUrl = website
        issueTrackerUrl = "$githubUrl/issues"
        vcsUrl = githubUrl
        githubRepo = githubRepoName
        githubReleaseNotesFile = "README.md"
        with(version) {
            name = project.version as String?
            desc = project.description
            vcsTag = System.getenv("TRAVIS_TAG")
        }
    }
}

tasks {
    withType(com.jfrog.bintray.gradle.tasks.BintrayUploadTask::class.java) {
        dependsOn(assemble)
    }
}
