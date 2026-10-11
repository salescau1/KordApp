plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.chordapp"
    compileSdk = 34

    defaultConfig {
        // Brand: KordApp by kekalclip. namespace tetap com.chordapp (lokasi
        // package/R class) supaya tidak perlu refactor seluruh source; hanya
        // applicationId (identitas publik permanen di store) yang di-rebrand.
        applicationId = "com.kekalclip.kordapp"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            signingConfig = signingConfigs.getByName("debug")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
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
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    androidResources {
        // catalog.db.xz sudah terkompres; jangan biarkan APK men-deflate ulang
        // (buang CPU, tak mengecil). Simpan apa adanya.
        noCompress += "xz"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2024.02.00"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    // Pull-to-refresh (Material2 pullRefresh) — API stabil lintas versi, dipakai Top 20.
    implementation("androidx.compose.material:material")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // HTML DOM parser untuk GuitarTuna scraper (1:1 port dari BeautifulSoup di PC)
    implementation("org.jsoup:jsoup:1.17.2")

    // Pemuat gambar untuk popup promo (dari jsDelivr). Ringan, Compose-native.
    implementation("io.coil-kt:coil-compose:2.6.0")

    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("androidx.sqlite:sqlite:2.4.0")

    // XZ/LZMA2 decoder (pure Java): katalog dikirim sebagai catalog.db.xz lalu
    // di-decompress ke DB path saat pertama buka (Room.createFromInputStream).
    implementation("org.tukaani:xz:1.9")

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("androidx.test.ext:junit-ktx:1.1.5")
    testImplementation("androidx.test:core-ktx:1.5.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2024.02.00"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}


ksp {
    arg("room.generateKotlin", "true")
}
