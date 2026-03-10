plugins {
    id("rivikauth.android.compose")
    id("rivikauth.hilt")
}
android {
    namespace = "dev.rivikauth.feature.settings"
    buildFeatures { buildConfig = true }
    defaultConfig {
        val gitHash = providers.exec {
            commandLine("git", "rev-parse", "--short", "HEAD")
        }.standardOutput.asText.get().trim()
        buildConfigField("String", "GIT_HASH", "\"$gitHash\"")
    }
}
dependencies {
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.lifecycle.viewmodel.compose)
    implementation(libs.compose.material.icons)
    implementation(libs.appcompat)
    implementation(libs.biometric)
    implementation(project(":core:datastore"))
    implementation(project(":core:crypto"))
    implementation(project(":core:database"))
    implementation(project(":core:model"))
}
