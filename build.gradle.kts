plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

version = "1.0.0"

android {
    namespace = "androidx.iot"
    compileSdk = 35

    defaultConfig {
        minSdk = 24

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    packaging {
        resources {
            excludes += "META-INF/DEPENDENCIES"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.gson)
    implementation(libs.okhttp)
    implementation(files("libs/sshd-common-2.12.1.jar"))
    implementation(files("libs/sshd-core-2.12.1.jar"))
    implementation(files("libs/sshd-scp-2.12.1.jar"))
    implementation(files("libs/sshd-sftp-2.12.1.jar"))
    implementation(files("libs/slf4j-api-1.7.32.jar"))
    implementation(files("libs/jcl-over-slf4j-1.7.32.jar"))
    implementation(libs.androidx.runtime.android)
    implementation(libs.androidx.activity)

    api(files("libs/bugly-4.1.9.3.jar"))
    api(files("libs/mqttv3-1.1.0.jar"))
    api(files("libs/service-1.1.4.jar"))
}
