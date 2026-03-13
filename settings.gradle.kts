pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RivikAuthenticator"

includeBuild("../rivik-fido-sdk") {
    dependencySubstitution {
        substitute(module("dev.rivik.fido:webauthn")).using(project(":webauthn"))
        substitute(module("dev.rivik.fido:attestation")).using(project(":attestation"))
        substitute(module("dev.rivik.fido:cable")).using(project(":cable"))
        substitute(module("dev.rivik.fido:crypto")).using(project(":crypto"))
    }
}

include(":app")
include(":core:common")
include(":core:model")
include(":core:crypto")
include(":core:database")
include(":core:datastore")
include(":feature:otp")
include(":feature:fido")
include(":feature:vault")
include(":feature:settings")
include(":feature:scanner")
include(":feature:import-export")
include(":service:credential")
include(":service:ble")
include(":service:nfc")
