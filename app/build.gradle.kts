
plugins {
  id("com.google.gms.google-services")
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.aistudio.pulsemusic.kzvpmx"
    minSdk = 24
    targetSdk = 36
    versionCode = 17
    versionName = "2.7.1"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystoreFile = file("${rootDir}/release.keystore")
      if (keystoreFile.exists()) {
          storeFile = keystoreFile
          storePassword = System.getenv("KEYSTORE_PASSWORD").orEmpty()
          keyAlias = System.getenv("KEY_ALIAS") ?: "upload"
          keyPassword = System.getenv("KEY_PASSWORD") ?: System.getenv("KEYSTORE_PASSWORD").orEmpty()
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      if (file("${rootDir}/release.keystore").exists()) {
          signingConfig = signingConfigs.getByName("release")
      }
    }
    debug { signingConfig = signingConfigs.getByName("debug") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }

  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
}

// Release builds must never silently fall back to the debug key. Debug builds and
// unit tests remain usable without a keystore. The check runs when a release-
// producing task is requested, including aggregate tasks such as `build`.
val releaseKeystoreFile = file("${rootDir}/release.keystore")
val requestedTaskNames = gradle.startParameter.taskNames.map { it.substringAfterLast(':') }
val releaseBuildRequested = requestedTaskNames.any { taskName ->
    taskName.contains("release", ignoreCase = true) ||
        taskName == "build" ||
        taskName == "assemble" ||
        taskName == "bundle" ||
        taskName == "publish"
}
if (releaseBuildRequested) {
    check(releaseKeystoreFile.exists()) {
        "release.keystore is required for release builds. Restore it from the KEYSTORE_BASE64 secret or place it at the repository root."
    }
    check(!System.getenv("KEYSTORE_PASSWORD").isNullOrBlank()) {
        "KEYSTORE_PASSWORD must be set for release builds."
    }
}


dependencies {
  implementation(platform("com.google.firebase:firebase-bom:32.8.1"))
  implementation("com.google.firebase:firebase-database")
  implementation("com.google.firebase:firebase-auth")
  implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
  implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.5")
  implementation("org.mozilla:rhino:1.7.14")
  implementation("org.jsoup:jsoup:1.17.2")
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.exoplayer.hls)
  implementation(libs.androidx.media3.session)
  implementation(libs.androidx.media3.ui)
  implementation(libs.androidx.media3.datasource)
  implementation(libs.androidx.media3.datasource.okhttp)
  implementation(libs.androidx.media3.common)
  implementation(libs.androidx.media)
  implementation(libs.converter.moshi)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.logging.interceptor)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation("dev.toastbits.ytmkt:ytmkt:0.3.2")
  implementation("io.ktor:ktor-client-okhttp:3.0.3")
  
  // testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation("org.robolectric:robolectric:4.11.1")
  testImplementation(libs.kotlinx.coroutines.test)
  // androidTestImplementation(platform(libs.androidx.compose.bom))
  // androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  // androidTestImplementation(libs.androidx.espresso.core)
  // androidTestImplementation(libs.androidx.junit)
  // androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
  "ksp"(libs.moshi.kotlin.codegen)
}
