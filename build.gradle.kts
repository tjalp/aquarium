plugins {
    `java-library`
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("io.papermc.paperweight.userdev") version "1.5.11" apply false
    id("xyz.jpenilla.run-paper") version "2.2.2"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

base {
    val archivesBaseName: String by project
    archivesName.set(archivesBaseName)
    project.version = "1.0.0"
}

val mavenGroup: String by project
group = mavenGroup

repositories {
    mavenCentral()
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "org.jetbrains.kotlin.kapt")
    apply(plugin = "xyz.jpenilla.run-paper")
    apply(plugin = "com.github.johnrengelman.shadow")

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven("https://repo.kryptonmc.org/releases/")
        maven("https://repo.dmulloy2.net/repository/public/")
        maven("https://jitpack.io")
        maven("https://repo.md-5.net/content/groups/public/")
        maven("https://repo.opencollab.dev/main/")
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
        maven("https://hub.jeff-media.com/nexus/repository/jeff-media-public/")
        maven("https://repo.spongepowered.org/maven/")
    }

    tasks {
        val javaVersion = JavaVersion.VERSION_17

        withType<JavaCompile> {
            options.encoding = "UTF-8"
            sourceCompatibility = javaVersion.toString()
            targetCompatibility = javaVersion.toString()
            options.release.set(javaVersion.toString().toInt())
        }

        withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
            kotlinOptions { jvmTarget = javaVersion.toString() }
        }

        jar { from("LICENSE") { rename { "${it}_${base.archivesName}" } } }

        processResources {
            inputs.property("version", project.version)
        }

        java {
            toolchain { languageVersion.set(JavaLanguageVersion.of(javaVersion.toString())) }
            sourceCompatibility = javaVersion
            targetCompatibility = javaVersion
            withSourcesJar()
        }

        runServer {
            minecraftVersion("1.20.4")
        }

        shadowJar {
            relocate("cloud.commandframework", "net.tjalp.nautilus.lib.cloud")
            relocate("com.github.twitch4j", "net.tjalp.nautilus.lib.twitch4j")
            relocate("com.jeff_media.morepersistentdatatypes", "net.tjalp.nautilus.lib.persistentdatatypes")
            relocate("com.jeff_media.customblockdata", "net.tjalp.nautilus.lib.customblockdata")
            relocate("org.incendo.interfaces", "net.tjalp.nautilus.lib.interfaces")
            //relocate("kotlin", "net.tjalp.aquarium.lib.kotlin")
        }
    }
}