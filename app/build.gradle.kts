plugins { id("com.android.application"); id("org.jetbrains.kotlin.android"); id("org.jetbrains.kotlin.kapt") }
android {
 namespace = "dev.elm.prototype"
 compileSdk = 35
 defaultConfig { applicationId = "dev.elm.prototype"; minSdk = 26; targetSdk = 35; versionCode = 1; versionName = "0.1-dev"; testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner" }
 buildFeatures { compose = true }
 composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
 compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
 kotlinOptions { jvmTarget = "17" }
}
dependencies {
 implementation("androidx.activity:activity-compose:1.9.2")
 implementation("androidx.compose.ui:ui:1.6.8")
 implementation("androidx.compose.ui:ui-tooling-preview:1.6.8")
 debugImplementation("androidx.compose.ui:ui-tooling:1.6.8")
 implementation("androidx.compose.material3:material3:1.2.1")
 implementation("androidx.room:room-runtime:2.6.1")
 implementation("androidx.room:room-ktx:2.6.1")
 kapt("androidx.room:room-compiler:2.6.1")
 implementation("androidx.media3:media3-exoplayer:1.4.1")
 implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
 testImplementation("junit:junit:4.13.2")
 androidTestImplementation("androidx.test:runner:1.6.2")
 androidTestImplementation("androidx.test.ext:junit:1.2.1")
}
