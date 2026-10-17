import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidKmpLibrary)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.sqldelight)
}

kotlin {
    android {
        namespace = "com.houseofvino.data"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    jvm {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
    }
    iosArm64()
    iosSimulatorArm64()

    compilerOptions {
        optIn.addAll("kotlin.uuid.ExperimentalUuidApi", "kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain.dependencies {
            api(projects.core)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
            // Ktor only for the Open Food Facts lookup (ADR-09).
            implementation(libs.ktor.client.core)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
            implementation(libs.ktor.client.okhttp)
        }
        iosMain.dependencies {
            implementation(libs.sqldelight.native.driver)
            implementation(libs.ktor.client.darwin)
        }
        jvmMain.dependencies {
            implementation(libs.sqldelight.sqlite.driver)
            implementation(libs.ktor.client.okhttp)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
        }
    }
}

sqldelight {
    databases {
        create("HouseOfVinoDatabase") {
            packageName.set("com.houseofvino.data.db")
            dialect("app.cash.sqldelight:sqlite-3-25-dialect:${libs.versions.sqldelight.get()}")
            // The schema is exactly the result of running migrations 1..N, so fresh installs and
            // upgraded installs cannot drift apart.
            deriveSchemaFromMigrations.set(true)
            generateAsync.set(false)
        }
    }
}

// The bundled wine catalogue (ADR-14, docs/CATALOGUE.md): tools/lwin/lwin.tsv.gz -> build/catalogue/lwin.db,
// built with the same text normaliser the app searches with.
val catalogueSource = rootProject.layout.projectDirectory.file("tools/lwin/lwin.tsv.gz")
val cataloguePack = layout.buildDirectory.file("catalogue/lwin.db")
val buildCataloguePack by tasks.registering(JavaExec::class) {
    group = "build"
    description = "Builds the LWIN catalogue pack shipped in the apps."
    val jvmMain = kotlin.jvm().compilations.getByName("main")
    dependsOn(jvmMain.compileTaskProvider)
    classpath = files(jvmMain.output.allOutputs, jvmMain.runtimeDependencyFiles)
    mainClass.set("com.houseofvino.data.catalogue.BuildCataloguePackKt")
    inputs.file(catalogueSource)
    outputs.file(cataloguePack)
    args(catalogueSource.asFile.absolutePath, cataloguePack.get().asFile.absolutePath)
}
