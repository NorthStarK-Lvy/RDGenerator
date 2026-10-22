// 顶层构建脚本。
// 版本组合：Gradle 9.6.1 / AGP 9.3.1 / Kotlin 2.4.10（AGP 9 内置 Kotlin 支持）
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:9.3.1")
        classpath("org.jetbrains.kotlin:compose-compiler-gradle-plugin:2.4.10")
    }
}
