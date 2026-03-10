plugins {
    id("rivikauth.android.library")
    id("rivikauth.hilt")
}
android { namespace = "dev.rivikauth.core.crypto" }
dependencies {
    implementation(libs.argon2kt)
    implementation(libs.bouncycastle)
    implementation(libs.biometric)
    implementation(project(":core:model"))
    testImplementation(libs.junit5.api)
    testRuntimeOnly(libs.junit5.engine)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
