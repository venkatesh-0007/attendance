plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.hilt.android)
}

android {
    namespace = "com.attendance.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.attendance.app"
        minSdk = 24
        targetSdk = 36
        versionCode = 100
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }
}

kotlin {
    jvmToolchain(17)
}

val releasesDir = file("/Users/venkatesh/Documents/Projects/Releases")

tasks.register<Copy>("copyApkToReleases") {
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    include("*.apk")
    into(releasesDir)
    rename { "Attendance-v1-debug.apk" }
}

tasks.register<Copy>("copyApkToReleasesDefault") {
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    include("*.apk")
    into(releasesDir)
    rename { "Attendance-debug.apk" }
}

tasks.register<Copy>("copyApkToReleasesV1") {
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    include("*.apk")
    into(releasesDir)
    rename { "Attendance-v1.0-debug.apk" }
}

afterEvaluate {
    tasks.findByName("assembleDebug")?.finalizedBy("copyApkToReleases", "copyApkToReleasesDefault", "copyApkToReleasesV1")
}




dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)

  // DI (Hilt)
  implementation(libs.hilt.android)
  ksp(libs.hilt.compiler)
  implementation(libs.androidx.hilt.navigation.compose)

  // Retrofit & OkHttp
  implementation(libs.retrofit)
  implementation(libs.retrofit.converter.kotlinx.serialization)
  implementation(libs.okhttp.logging.interceptor)

  // Serialization
  implementation(libs.kotlinx.serialization.json)

  // WorkManager
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.androidx.hilt.work)
  ksp(libs.androidx.hilt.work.compiler)

  // Security (EncryptedSharedPreferences)
  implementation(libs.androidx.security.crypto)

  // Glance Widgets
  implementation(libs.androidx.glance.appwidget)
  implementation(libs.androidx.glance.material3)
}
