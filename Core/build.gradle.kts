import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

plugins {
    id("su.nightexpress.excellentenchants.java-conventions")
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.3"
    id("net.kyori.indra.git") version "2.1.1"
}

dependencies {
    // The server API
    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")

    // NMS modules
    api(project(":NMS"))
    implementation(project(":V1_17_R1", configuration = "reobf"))
    implementation(project(":V1_18_R2", configuration = "reobf"))
    implementation(project(":V1_19_R2", configuration = "reobf"))
    implementation(project(":V1_19_R3", configuration = "reobf"))

    // 3rd party plugins
    compileOnly("me.clip:placeholderapi:2.11.2")
    compileOnly("fr.neatmonster:nocheatplus:3.16.1-SNAPSHOT")
    compileOnly("com.comphenix.protocol:ProtocolLib:5.0.0-SNAPSHOT")
    compileOnly("com.sk89q.worldguard:worldguard-bukkit:7.0.6") {
        exclude("org.bukkit")
    }
}

description = "Core"
version = "$version".decorateVersion()

fun lastCommitHash(): String = indraGit.commit()?.name?.substring(0, 7) ?: error("Could not determine commit hash")
fun String.decorateVersion(): String = if (endsWith("-SNAPSHOT")) "$this-${lastCommitHash()}" else this

bukkit {
    main = "su.nightexpress.excellentenchants.ExcellentEnchants"
    name = "ExcellentEnchants"
    description = "Vanilla-like enchants for your server."
    version = "${project.version}"
    apiVersion = "1.17"
    authors = listOf("NightExpress")
    load = BukkitPluginDescription.PluginLoadOrder.STARTUP
    loadBefore = listOf("HuskSync")
    depend = listOf("NexEngine")
    softDepend = listOf(
        "ProtocolLib",
        "NoCheatPlus"
    )
    permissions {
        register("excellentenchants.admin") {
            description = "Grants access to all plugin functions."
            default = BukkitPluginDescription.Permission.Default.OP // TRUE, FALSE, OP or NOT_OP
            childrenMap = mapOf(
                "excellentenchants.user" to true,
                "excellentenchants.command" to true
            )
        }
        register("excellentenchants.user") {
            description = "Grants access to basic player plugin functions."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("excellentenchants.command") {
            description = "Grants access to all the plugin commands."
            default = BukkitPluginDescription.Permission.Default.OP
            children = listOf(
                "excellentenchants.command.book",
                "excellentenchants.command.enchant",
                "excellentenchants.command.list",
                "excellentenchants.command.tierbook"
            )
        }
        register("excellentenchants.command.book") {
            description = "Grants access to /eenchants book command."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("excellentenchants.command.enchant") {
            description = "Grants access to /eenchants enchant command."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("excellentenchants.command.list") {
            description = "Grants access to /eenchants list command."
            default = BukkitPluginDescription.Permission.Default.OP
        }
        register("excellentenchants.command.tierbook") {
            description = "Grants access to /eenchants tierbook command."
            default = BukkitPluginDescription.Permission.Default.OP
        }
    }
}

tasks {
    jar {
        archiveClassifier.set("noshade")
    }
    build {
        dependsOn(shadowJar)
    }
    shadowJar {
        minimize {
            exclude(dependency("su.nightexpress.excellentenchants:.*:.*"))
        }
        archiveFileName.set("ExcellentEnchants-${project.version}.jar")
        archiveClassifier.set("")
        destinationDirectory.set(file("$rootDir"))
    }
    processResources {
        filesMatching("**/paper-plugin.yml") {
            expand(mapOf(
                "version" to "${project.version}",
                "description" to project.description
            ))
        }
    }
    register("deployJar") {
        doLast {
            exec {
                commandLine("rsync", shadowJar.get().archiveFile.get().asFile.absoluteFile, "dev:data/dev/jar")
            }
        }
    }
    register("deployJarFresh") {
        dependsOn(build)
        finalizedBy(named("deployJar"))
    }
}

java {
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components["java"])
        }
    }
}
