import java.util.Base64
import java.util.Properties

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

/**
 * Backend settings come from the environment (CI) or from `local.properties` (developers), never
 * from the repository. Both are optional: without them the app runs in local-only mode.
 */
val localProps = Properties().apply {
    rootProject.file("local.properties").takeIf { it.exists() }?.inputStream()?.use { load(it) }
}

fun setting(name: String): String =
    providers.environmentVariable(name).orNull ?: localProps.getProperty(name) ?: ""

val supabaseUrl = setting("SUPABASE_URL")
val supabaseAnonKey = setting("SUPABASE_ANON_KEY")

/** Mirrors core's KeyGuard so a privileged key fails the build instead of shipping. */
fun isPrivilegedKey(key: String): Boolean {
    if (key.isBlank()) return false
    if (key.startsWith("sb_secret_")) return true
    if (key.startsWith("sb_publishable_")) return false
    val parts = key.split('.')
    if (parts.size != 3) return true // unknown format: refuse rather than guess
    val payload = runCatching { String(Base64.getUrlDecoder().decode(parts[1].trimEnd('='))) }.getOrElse { return true }
    val role = Regex("\"role\"\\s*:\\s*\"([^\"]+)\"").find(payload)?.groupValues?.get(1)
    return role != "anon" && role != "authenticated"
}

check(!isPrivilegedKey(supabaseAnonKey)) {
    "SUPABASE_ANON_KEY is not a publishable key. Never put the service-role / secret key in an app build."
}

android {
    namespace = "com.houseofvino.android"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.houseofvino.app"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
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
