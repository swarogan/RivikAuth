plugins {
    id("rivikauth.android.library")
    alias(libs.plugins.kotlin.serialization)
}
android { namespace = "dev.rivikauth.core.model" }
dependencies { implementation(libs.kotlinx.serialization.json) }
