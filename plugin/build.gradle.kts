plugins {
    id("io.papermc.paperweight.userdev")
}

group = "net.tjalp.nautilus"
version = "0.1.0"

dependencies {
    implementation("io.ktor:ktor-client-okhttp-jvm:2.3.0")
    implementation("io.ktor:ktor-server-core-jvm:2.3.0")
    implementation("io.ktor:ktor-server-netty-jvm:2.3.0")
    implementation("io.ktor:ktor-client-core-jvm:2.3.0")
    paperweight.paperDevBundle("1.20.4-R0.1-SNAPSHOT")

    compileOnly(project(":nautilus-mod"))

    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
    implementation("cloud.commandframework:cloud-annotations:1.8.2")
    implementation("cloud.commandframework:cloud-paper:1.8.2")
//    implementation("org.litote.kmongo:kmongo-async:4.8.0")
    implementation("org.litote.kmongo:kmongo-coroutine:4.8.0")
    implementation("org.ocpsoft.prettytime:prettytime:5.0.6.Final")
    implementation("org.incendo.interfaces:interfaces-paper:1.0.0-SNAPSHOT")
    implementation("org.incendo.interfaces:interfaces-kotlin:1.0.0-SNAPSHOT")
//    implementation("org.incendo.interfaces:interfaces-next:1.0.0-SNAPSHOT")
    implementation("com.jeff_media:MorePersistentDataTypes:2.3.1")
    implementation("com.jeff_media:CustomBlockData:2.2.0")
    //implementation("com.github.twitch4j:twitch4j:1.11.0")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("me.neznamy:tab-api:4.0.2")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.1.0")
//    compileOnly("com.github.dmulloy2:ProtocolLib:master-SNAPSHOT")
    compileOnly("org.geysermc.floodgate:api:2.2.2-SNAPSHOT")
    compileOnly("LibsDisguises:LibsDisguises:10.0.44") {
        exclude("org.spigotmc")
    }

    kapt("cloud.commandframework:cloud-annotations:1.8.2")
}

tasks {
    assemble {
        dependsOn("reobfJar")
    }

    build {
        doLast {
            val nautilusPluginOut: String by project

            if (project.hasProperty("nautilusPluginOut")) {
                copy {
                    from(buildDir.resolve("libs").resolve("${project.name}-$version.jar"))
                    into(nautilusPluginOut)

                    rename("${project.name}-$version.jar", "${project.name}.jar")
                }
            }
        }
    }
}