plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

android {
    namespace = "com.houseofvino.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.houseofvino.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 3
        versionName = "0.3.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Release signing is configured by the developer (see README, "Release builds").
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}", "/META-INF/versions/9/previous-compilation-data.bin")
    }
}

dependencies {
    implementation(projects.composeApp)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.work.runtime)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.compose.runtime)
    implementation(libs.compose.ui)
    androidTestImplementation(libs.androidx.test.runner)
}

// Ship the catalogue pack as an asset (ADR-14).
val catalogueAssets = layout.buildDirectory.dir("generated/catalogueAssets")
val copyCataloguePack by tasks.registering(Copy::class) {
    dependsOn(":data:buildCataloguePack")
    from(project(":data").layout.buildDirectory.file("catalogue/lwin.db"))
    into(catalogueAssets)
}
android.sourceSets.getByName("main").assets.srcDir(catalogueAssets.get().asFile)
tasks.named("preBuild") { dependsOn(copyCataloguePack) }
