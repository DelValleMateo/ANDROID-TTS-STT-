import java.net.URI
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

val localPropertiesFile = rootProject.file("local.properties")

val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use(::load)
    }
}

fun String.escapeForBuildConfig(): String =
    replace("\\", "\\\\").replace("\"", "\\\"")

fun normalizeHttpBaseUrl(value: String): String {
    val normalized = value.trim().let { if (it.endsWith('/')) it else "$it/" }
    val uri = runCatching { URI(normalized) }
        .getOrElse { throw GradleException("PTAH_API_BASE_URL no es una URL válida.", it) }

    if (uri.scheme !in setOf("http", "https") || uri.host.isNullOrBlank()) {
        throw GradleException("PTAH_API_BASE_URL debe ser una URL HTTP(S) absoluta.")
    }

    return normalized
}

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

val groqApiKey = localProperty("GROQ_API_KEY")
    ?: providers.gradleProperty("GROQ_API_KEY").orNull?.trim()?.takeIf { it.isNotBlank() }
    ?: ""

val ptahApiBaseUrl = (
    localProperty("PTAH_API_BASE_URL")
        ?: providers.gradleProperty("PTAH_API_BASE_URL").orNull?.trim()?.takeIf { it.isNotBlank() }
        ?: "https://api.groq.com/openai/v1/"
    ).let(::normalizeHttpBaseUrl)

val groqModel = localProperty("GROQ_MODEL")
    ?: providers.gradleProperty("GROQ_MODEL").orNull?.trim()?.takeIf { it.isNotBlank() }
    ?: "openai/gpt-oss-120b"

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
        buildConfigField("String", "GROQ_API_KEY", "\"${groqApiKey.escapeForBuildConfig()}\"")
        buildConfigField("String", "PTAH_API_BASE_URL", "\"${ptahApiBaseUrl.escapeForBuildConfig()}\"")
        buildConfigField("String", "GROQ_MODEL", "\"${groqModel.escapeForBuildConfig()}\"")

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a", "x86_64")
        }
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
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    androidResources {
        noCompress += listOf("onnx", "bin")
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
    implementation(libs.androidx.compose.material.icons.extended)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Cliente HTTP. Groq permanece como proveedor temporal detrás de la capa de datos.
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")

    // Se activa solo en debug desde RetrofitProvider.
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Sherpa-ONNX para síntesis de voz offline local (Kokoro TTS)
    implementation(files("libs/sherpa-onnx-1.13.8.aar"))

    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
