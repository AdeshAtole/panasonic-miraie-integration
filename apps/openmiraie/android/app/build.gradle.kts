plugins { id("com.android.application") }
android {
    namespace = "in.openmiraie.android"
    compileSdk = 35
    buildToolsVersion = "35.0.0"
    defaultConfig {
        applicationId = "in.openmiraie.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "in.openmiraie.android.LayoutReviewTest"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildTypes {
        release { isMinifyEnabled = false }
        create("review") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".review"
            matchingFallbacks += "debug"
        }
    }
    testBuildType = "review"
    lint { abortOnError = true }
}
java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }
dependencies {
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
configurations.configureEach { resolutionStrategy.activateDependencyLocking() }
