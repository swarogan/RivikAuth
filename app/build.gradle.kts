plugins {
    id("rivikauth.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
    id("rivikauth.hilt")
}
android {
    namespace = "dev.rivikauth.app"
    defaultConfig {
        applicationId = "dev.rivikauth"
        versionCode = 1
        versionName = "0.1.0"
    }
    signingConfigs {
        create("release") {
            storeFile = rootProject.file("rivikauth-release.jks")
            storePassword = "rivikauth123"
            keyAlias = "rivikauth"
            keyPassword = "rivikauth123"
        }
    }
    buildFeatures { compose = true }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/versions/9/OSGI-INF/MANIFEST.MF",
                "META-INF/DEPENDENCIES",
            )
        }
    }
    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "RivikAuth DEBUG")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    applicationVariants.all {
        outputs.all {
            val output = this as com.android.build.gradle.internal.api.BaseVariantOutputImpl
            output.outputFileName = "rivikauth-${buildType.name}-v${versionName}.apk"
        }
    }
}
dependencies {
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material.icons)
    implementation(libs.activity.compose)
    implementation(libs.navigation.compose)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core:common"))
    implementation(project(":core:model"))
    implementation(project(":core:crypto"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":feature:otp"))
    implementation(project(":feature:fido"))
    implementation(project(":feature:vault"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:scanner"))
    implementation(project(":feature:import-export"))
    implementation(project(":service:credential"))
    implementation(project(":service:ble"))
    debugImplementation(libs.compose.ui.tooling)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
