import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val releaseKeystorePropertiesFile: File? = run {
    val envPath = System.getenv("PIECER_KEYSTORE_PROPERTIES")?.trim()?.takeIf { it.isNotEmpty() }
    val userProfileDir = System.getenv("USERPROFILE")?.trim()?.takeIf { it.isNotEmpty() }
        ?: System.getProperty("user.home")
    val candidates = buildList {
        if (envPath != null) add(file(envPath))
        add(file("$userProfileDir/.piecer-signing/keystore.properties"))
    }
    candidates.firstOrNull { it.isFile }
}

val releaseKeystoreProperties: Properties? = releaseKeystorePropertiesFile?.let { propsFile ->
    Properties().apply {
        propsFile.inputStream().use(::load)
    }
}

android {
    namespace = "com.piecer.app"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.piecer.app"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        create("release") {
            val props = releaseKeystoreProperties
            val propsFile = releaseKeystorePropertiesFile
            if (props != null && propsFile != null) {
                val storeFilePath = props.getProperty("storeFile")?.trim().orEmpty()
                if (storeFilePath.isNotEmpty()) {
                    val candidate = file(storeFilePath)
                    storeFile = if (candidate.isAbsolute) candidate else propsFile.parentFile.resolve(storeFilePath)
                }
                storePassword = props.getProperty("storePassword")
                keyAlias = props.getProperty("keyAlias")
                keyPassword = props.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "CRASH_DIAGNOSTICS_ENABLED", "true")
            buildConfigField("boolean", "UPDATE_ENABLED", "false")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("boolean", "CRASH_DIAGNOSTICS_ENABLED", "false")
            buildConfigField("boolean", "UPDATE_ENABLED", "true")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        create("staging") {
            initWith(getByName("release"))
            signingConfig = signingConfigs.getByName("debug")
            applicationIdSuffix = ".staging"
            versionNameSuffix = "-staging"
            matchingFallbacks += listOf("release")
            buildConfigField("boolean", "CRASH_DIAGNOSTICS_ENABLED", "true")
            buildConfigField("boolean", "UPDATE_ENABLED", "false")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

gradle.taskGraph.whenReady {
    val requiresReleaseSigning = allTasks.any { task ->
        task.project == project &&
            task.name.endsWith("Release") &&
            (task.name.startsWith("assemble") ||
                task.name.startsWith("bundle") ||
                task.name.startsWith("package") ||
                task.name.startsWith("sign") ||
                task.name.startsWith("validateSigning"))
    }
    if (requiresReleaseSigning) {
        val propsFile = releaseKeystorePropertiesFile
            ?: throw GradleException(
                "Release signing gagal: berkas keystore.properties tidak ditemukan. " +
                    "Set env PIECER_KEYSTORE_PROPERTIES atau buat %USERPROFILE%\\.piecer-signing\\keystore.properties (lihat SIGNING.md)."
            )
        val props = releaseKeystoreProperties
            ?: throw GradleException("Release signing gagal: tidak dapat membaca ${propsFile.name}.")
        val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
        val missingKeys = requiredKeys.filter { props.getProperty(it).isNullOrBlank() }
        if (missingKeys.isNotEmpty()) {
            throw GradleException(
                "Release signing gagal: properti wajib belum diisi di keystore.properties (${missingKeys.joinToString(", ")})."
            )
        }
        val releaseStoreFile = android.signingConfigs.getByName("release").storeFile
        if (releaseStoreFile == null || !releaseStoreFile.isFile) {
            throw GradleException(
                "Release signing gagal: berkas keystore tidak ditemukan di lokasi storeFile."
            )
        }
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
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.biometric)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
