import org.jetbrains.compose.compose
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.6.10"
    id("org.jetbrains.compose") version "1.1.1"
}

group = "ru.fbear"
version = "1.0"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(files("libs/libgphoto2-jna.jar"))
    implementation("net.java.dev.jna:jna:5.11.0")
    implementation("org.jetbrains.compose.material:material-icons-extended-desktop:1.1.1")
    implementation("org.jetbrains.compose.components:components-splitpane-desktop:1.1.1")
    implementation("org.burnoutcrew.composereorderable:reorderable:0.7.4")
    implementation("com.godaddy.android.colorpicker:compose-color-picker:0.4.2")
    implementation("org.jetbrains.exposed:exposed-core:0.38.2")
    implementation("org.jetbrains.exposed:exposed-jdbc:0.38.2")
    implementation("org.jetbrains.exposed:exposed-dao:0.38.2")
    implementation("org.xerial:sqlite-jdbc:3.36.0.3")

}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "16"
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Deb)
            packageName = "photo_mirror"
            packageVersion = "1.0.0"
        }
    }
}