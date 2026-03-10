plugins {
    id("rivikauth.android.library")
    id("rivikauth.hilt")
}
android { namespace = "dev.rivikauth.service.credential" }
dependencies {
    implementation(libs.credentials)
    implementation(libs.biometric)
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:crypto"))
    implementation("dev.rivik.fido:webauthn")
    implementation("dev.rivik.fido:attestation")
}
