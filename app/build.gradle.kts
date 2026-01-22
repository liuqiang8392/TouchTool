import com.android.build.api.dsl.ApplicationExtension
import com.android.build.gradle.tasks.PackageAndroidArtifact
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

plugins {
    alias(libs.plugins.application)
    alias(libs.plugins.nav.safeargs)
}

val pattern: DateTimeFormatter? = DateTimeFormatter.ofPattern("yyMMdd_HHmm")
val now: String? = LocalDateTime.now().format(pattern)

configure<ApplicationExtension> {
    namespace = "top.bogey.touch_tool"
    compileSdk = common.versions.targetSdk.get().toInt()
    ndkVersion = common.versions.ndkVersion.get()
    buildToolsVersion = common.versions.buildToolsVersion.get()

    defaultConfig {
        applicationId = "top.bogey.touch_tool"
        minSdk = common.versions.minSdk.get().toInt()
        targetSdk = common.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = now

        @Suppress("UnstableApiUsage")
        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++14", "-Wno-format")
                arguments += listOf("-DANDROID_STL=c++_shared")
            }
        }

        ndk {
            abiFilters.add("arm64-v8a")
        }
    }

    buildTypes {

        debug {
            applicationIdSuffix = ".debug"
            resValue("string", "app_name", "点击助手Debug")
        }

        release {
            isMinifyEnabled = false
            isShrinkResources = false
            resValue("string", "app_name", "点击助手")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = common.versions.cmakeVersion.get()
        }
    }

    buildFeatures {
        viewBinding = true
        aidl = true
    }
}

tasks.withType<PackageAndroidArtifact>().configureEach {
    if (name.contains("release", true)) {
        doLast {
            val dir = outputDirectory.get().asFile
            val apk = dir.listFiles()?.firstOrNull { it.extension == "apk" } ?: return@doLast

            val target = File(dir, "点击助手_${now}.APK")
            apk.copyTo(target, true)
        }
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)

    implementation(libs.nav.fragment)
    implementation(libs.nav.ui)

    implementation(libs.flexbox)

    implementation(libs.mmkv)
    implementation(libs.gson)
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    implementation(libs.exp4j)
    implementation(libs.zxinglite)
    implementation(libs.tinypinyin)
    implementation(libs.hiddenapibypass)
}