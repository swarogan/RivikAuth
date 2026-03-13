plugins { id("rivikauth.android.library") }
android { namespace = "dev.rivikauth.core.common" }
dependencies {
    implementation(libs.core.ktx)
    implementation(project(":core:model"))
}
