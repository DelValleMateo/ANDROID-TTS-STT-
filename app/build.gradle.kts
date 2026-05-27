import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localPropertiesFile = rootProject.file("local.properties")

val localProperties = Properties().apply {
    val localPropertiesFile = rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

fun localProperty(name: String): String? {
    localProperties.getProperty(name)
        ?.trim()
        ?.takeIf { it.isNotBlank() }
        ?.let { return it }

    if (!localPropertiesFile.exists()) return null

    return localPropertiesFile
        .readLines(Charsets.UTF_8)
        .firstNotNullOfOrNull { line ->
            val cleanLine = line
                .removePrefix("\uFEFF")
                .trim()

            if (cleanLine.startsWith("$name=")) {
                cleanLine.substringAfter("=").trim().takeIf { it.isNotBlank() }
            } else {
                null
            }
        }
}

val googleAiApiKey = localProperty("GOOGLE_AI_API_KEY")
    ?: providers.gradleProperty("GOOGLE_AI_API_KEY").orNull?.trim()?.takeIf { it.isNotBlank() }
    ?: ""

logger.lifecycle("GOOGLE_AI_API_KEY configurada: ${googleAiApiKey.isNotBlank()}")
logger.lifecycle("Longitud de GOOGLE_AI_API_KEY: ${googleAiApiKey.length} caracteres")

android {
    namespace = "com.uader.ptah"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.uader.ptah"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "GOOGLE_AI_API_KEY", "\"${googleAiApiKey.escapeForBuildConfig()}\"")
        buildConfigField("String", "GOOGLE_AI_MODEL", "\"gemini-2.0-flash\"")
    }

    buildTypes {
        release {
            // R8 elimina codigo muerto, ofusca y optimiza el APK final.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Identifica claramente el build de desarrollo.
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEBUG"
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Retrofit y Gson para hacer peticiones HTTP a Google IA.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Se activa solo en debug desde RetrofitProvider.
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
}
