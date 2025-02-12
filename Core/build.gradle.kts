plugins {
    id("su.nightexpress.project-conventions")
    id("cc.mewcraft.publishing-conventions")
    id("cc.mewcraft.deploy-conventions")
    id("cc.mewcraft.paper-plugins")
}

project.ext.set("name", "ExcellentEnchants")

dependencies {
    // The server API
    compileOnly(libs.server.paper)

    // NMS modules
    api(project(":NMS"))
    implementation(project(":V1_18_R2", configuration = "reobf"))
    implementation(project(":V1_19_R3", configuration = "reobf"))
    implementation(project(":V1_20_R1", configuration = "reobf"))

    // 3rd party plugins
    compileOnly(libs.papi)
    compileOnly(libs.nochestplus)
    compileOnly(libs.protocollib)
    compileOnly(libs.worldguard) {
        exclude("org.bukkit")
    }
    compileOnly(libs.mythicmobs) {
        isTransitive = false
    }

    // Adds support for custom items from various plugins
    compileOnly(libs.spatula.item)
}
