plugins {
    id("java")
}

group = "com.nikolaybotev"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.github.spotbugs:spotbugs-annotations:4.9.3")
    implementation("com.google.cloud:google-cloud-monitoring:3.61.0")

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}