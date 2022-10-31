import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    `java-library`
    val kotlinVersion: String by System.getProperties()
    kotlin("jvm") version kotlinVersion
    kotlin("kapt") version kotlinVersion
    id("io.papermc.paperweight.userdev") version "1.3.8"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.2"
    id("xyz.jpenilla.run-paper") version "1.0.6"
    id("com.github.johnrengelman.shadow") version "7.1.2"
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
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.kryptonmc.org/releases/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://repo.md-5.net/content/groups/public/")
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    paperDevBundle("1.19.2-R0.1-SNAPSHOT")
    implementation("cloud.commandframework:cloud-annotations:1.7.1")
    implementation("cloud.commandframework:cloud-paper:1.7.1")
    implementation("org.litote.kmongo:kmongo-async:4.7.1")
    implementation("org.litote.kmongo:kmongo-coroutine:4.7.1")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.5.Final")
    implementation("io.ktor:ktor-client-core:2.1.2")
    implementation("io.ktor:ktor-client-okhttp:2.1.2")
    //implementation("com.github.twitch4j:twitch4j:1.11.0")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("me.neznamy:tab-api:3.1.5")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0")
    compileOnly("LibsDisguises:LibsDisguises:10.0.31") {
        exclude("org.spigotmc")
    }

    kapt("cloud.commandframework:cloud-annotations:1.7.1")
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

    assemble {
        dependsOn(reobfJar)
    }

    runServer {
        minecraftVersion("1.19.2")
    }

    shadowJar {
        relocate("cloud.commandframework", "net.tjalp.nautilus.lib.cloud")
        relocate("com.github.twitch4j", "net.tjalp.nautilus.lib.twitch4j")
        //relocate("kotlin", "net.tjalp.aquarium.lib.kotlin")
    }
}

bukkit {
    load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
    authors = listOf("tjalp")
    apiVersion = "1.19"
    main = "net.tjalp.nautilus.Nautilus"
    version = project.version.toString()
    name = "Nautilus"
    softDepend = listOf("LibsDisguises", "TAB", "ProtocolLib")
}