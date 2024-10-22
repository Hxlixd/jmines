plugins {
    id("java")
    kotlin("jvm") version "1.7.20"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.hxlixd"
version = "1.0"

repositories {
    mavenCentral()

}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    implementation ("com.formdev:flatlaf:3.5.2")
}

tasks {
    shadowJar {
        archiveBaseName.set("jmines")
        archiveClassifier.set("")
        mergeServiceFiles()
    }
}

tasks.jar {
    manifest {
        attributes(
            "Main-Class" to "me.hxlixd.Main",
            "Implementation-Title" to "jmines",
            "Implementation-Version" to "1.0"
        )
    }
}

tasks.test {
    useJUnitPlatform()
}