plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
    alias(libs.plugins.paparazzi)
}

android {
    namespace = "com.aiaccounting"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.aiaccounting"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "0.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
    arg("room.generateKotlin", "true")
}

/**
 * 仪器测试的护栏。
 *
 * `connectedAndroidTest` 跑完会**卸载 App**——如果真机连着 adb，测试又跑到真机上，
 * 手机上真实记账数据会被一起清掉（已经发生过两次）。
 * 这里在任务开始前检查设备列表：只要连着非模拟器设备就直接失败，别让它跑。
 */
val adbExecutable: String =
    runCatching { android.sdkDirectory.resolve("platform-tools/adb").absolutePath }
        .getOrElse { "adb" }

tasks.matching { it.name.startsWith("connected") && it.name.endsWith("AndroidTest") }
    .configureEach {
        doFirst {
            val devices = runCatching {
                providers.exec { commandLine(adbExecutable, "devices") }
                    .standardOutput.asText.get()
            }.getOrDefault("")
            val physical = devices.lines()
                .map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("List of devices") }
                .filter { !it.startsWith("emulator-") }
            if (physical.isNotEmpty()) {
                throw GradleException(
                    "检测到真机连接（${physical.joinToString { it.substringBefore('\t') }}）：" +
                        "仪器测试会在真机上卸载 App 并清空记账数据。请先断开真机（adb disconnect），再跑测试。",
                )
            }
        }
    }

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.paparazzi)
    testImplementation(libs.okhttp.mockwebserver)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
