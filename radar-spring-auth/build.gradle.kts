plugins {
    id("kotlin-kapt")
    id("org.jlleitschuh.gradle.ktlint")
}

description = "This library provides functionality to add RADAR-base authorization in any Spring based application."

dependencies {
    val mpVersion: String by project
    api(group = "org.radarbase", name = "radar-auth", version = "2.1.0")

    val slf4jVersion: String by project
    api(group = "org.slf4j", name = "slf4j-api", version = slf4jVersion)

    val springVersion: String by project
    implementation(group = "org.springframework", name = "spring-web", version = springVersion)
    implementation(group = "org.springframework", name = "spring-context", version = springVersion)

    val aspectJVersion: String by project
    compileOnly(group = "org.aspectj", name = "aspectjweaver", version = aspectJVersion)

    val jakartaServletVersion: String by project
    implementation("jakarta.servlet:jakarta.servlet-api:$jakartaServletVersion")
}

ktlint {
    val ktlintVersion: String by project
    version.set(ktlintVersion)
}
