plugins {
    id("rivikauth.android.library")
    id("rivikauth.hilt")
}
android { namespace = "dev.rivikauth.service.ble" }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation("dev.rivik.fido:webauthn")
    implementation("dev.rivik.fido:attestation")
    implementation("dev.rivik.fido:cable")
}
