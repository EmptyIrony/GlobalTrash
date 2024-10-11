import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.izzel.taboolib.gradle.*
import io.izzel.taboolib.gradle.Basic
import io.izzel.taboolib.gradle.Bukkit
import io.izzel.taboolib.gradle.BukkitUtil
import io.izzel.taboolib.gradle.BukkitUI
import io.izzel.taboolib.gradle.BukkitHook
import io.izzel.taboolib.gradle.DatabaseAlkaidRedis
import io.izzel.taboolib.gradle.Database


plugins {
    java
    id("io.izzel.taboolib") version "2.0.18"
    id("org.jetbrains.kotlin.jvm") version "1.8.22"
}

taboolib {
    env {
        install(Basic)
        install(Bukkit)
        install(BukkitUtil)
        install(BukkitUI)
        install(BukkitHook)
        install(DatabaseAlkaidRedis)
        install(Database)
        install(BukkitNMS)
        install(BukkitNMSUtil)
    }
    description {
        name = "GlobalTrash"
        contributors {
            name("存在")
        }
    }

    relocate("redis.clients.jedis", "redis.clients.jedis_4_2_3")
    version { taboolib = "6.2.0-beta17" }
}

repositories {
    mavenCentral()
}

dependencies {
    compileOnly("redis.clients:jedis:5.2.0")
    compileOnly("com.google.code.gson:gson:2.8.6")
    compileOnly("ink.ptms.core:v12004:12004:mapped")
    compileOnly("ink.ptms.core:v12004:12004:universal")
    compileOnly(kotlin("stdlib"))
    compileOnly(fileTree("libs"))
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "1.8"
        freeCompilerArgs = listOf("-Xjvm-default=all")
    }
}

configure<JavaPluginConvention> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}
