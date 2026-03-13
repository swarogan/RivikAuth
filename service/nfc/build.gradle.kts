plugins {
    id("rivikauth.android.library")
    id("rivikauth.hilt")
}
android { namespace = "dev.rivikauth.service.nfc" }
dependencies {
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation("dev.rivik.fido:webauthn")
    implementation("dev.rivik.fido:cable")
}
