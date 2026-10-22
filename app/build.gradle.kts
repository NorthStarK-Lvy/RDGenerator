plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.rdgen.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.rdgen.app"
        // 正整数随机数生成器，不依赖任何新系统 API，放宽到 Android 8.0 也能装。
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false

            // 用 debug 证书签名：保证 assembleRelease 出的是可直接安装的包，
            // 而不是 app-release-unsigned.apk；debuggable 仍然关掉。
            // 发布到应用商店时请换成自己的 signingConfig。
            isDebuggable = false
            signingConfig = signingConfigs.getByName("debug")

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    testOptions {
        unitTests.all {
            it.maxHeapSize = "1024m"
            it.jvmArgs("-XX:+UseSerialGC", "-XX:MaxMetaspaceSize=512m")
        }
        unitTests.isIncludeAndroidResources = true

        // 截图只用于开发期肉眼核对（没有真机/模拟器时），默认不跑；
        // 需要时加 -Prdgen.screenshots=true
        unitTests.all {
            it.systemProperty("rdgen.screenshots", project.findProperty("rdgen.screenshots") ?: "false")
            it.filter {
                if ((project.findProperty("rdgen.screenshots") ?: "false") != "true") {
                    excludeTestsMatching("com.rdgen.app.Screenshot*")
                }
            }
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.17.0")
    implementation("androidx.activity:activity-compose:1.12.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.4")

    // Compose 运行时统一到 1.12.0；material3 用 1.4.0 稳定版。
    implementation("androidx.compose.ui:ui:1.12.0")
    implementation("androidx.compose.ui:ui-graphics:1.12.0")
    implementation("androidx.compose.foundation:foundation:1.12.0")
    implementation("androidx.compose.animation:animation:1.12.0")
    implementation("androidx.compose.material3:material3:1.4.0")

    testImplementation("junit:junit:4.13.2")

    // 仅用于 JVM 上渲染截图核对排版（不会进 APK）
    testImplementation("org.robolectric:robolectric:4.17")
    testImplementation("androidx.test:core-ktx:1.7.0")
    testImplementation("androidx.compose.ui:ui-test-junit4:1.12.0")
}
