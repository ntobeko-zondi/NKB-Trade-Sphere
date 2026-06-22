plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.nkbtradesphere"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.nkbtradesphere"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // Core Android
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)

    // Navigation (fragments + bottom nav)
    implementation("androidx.fragment:fragment:1.6.2")

    // RecyclerView (for categories, featured, recommended lists)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Product detail image carousel
    implementation("androidx.viewpager2:viewpager2:1.1.0")

    // Wrapping filter chips on search screen
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // CardView (for product cards and search bar)
    implementation("androidx.cardview:cardview:1.0.0")

    // NestedScrollView (for the home screen scroll)
    implementation("androidx.core:core:1.12.0")

    // Splash Screen API (backport to API 23+)
    implementation("androidx.core:core-splashscreen:1.0.1")

    // Glide — loads images from URLs into ImageViews
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // Internet permission is in AndroidManifest — Glide needs it to load images

    // Volley for HTTP requests
    implementation("com.android.volley:volley:1.2.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}
