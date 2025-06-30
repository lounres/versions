import com.vanniktech.maven.publish.SonatypeHost
import dev.lounres.versions.VersionsFile
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.parseToTomlTable
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.io.reader

plugins {
    `version-catalog`
    alias(libs.plugins.gradle.maven.publish.plugin)
}

repositories {
    mavenCentral()
}

val now: LocalDateTime = LocalDateTime.now(ZoneId.of("UTC"))
version = "${now.year}.${now.month.value}.${now.dayOfMonth}.${now.hour}"

catalog.versionCatalog {
    from(files("gradle/libs.versions.toml"))
}

val versionCatalogsToMerge: Map<String, String> = mapOf(
//    "logKube" to "dev.lounres:logKube.versionCatalog:${libs.versions.logKube.get()}",
    "kone" to "dev.lounres:kone.versionCatalog:${libs.versions.kone.get()}",
    "kotlin-wrappers" to "org.jetbrains.kotlin-wrappers:kotlin-wrappers-catalog:${libs.versions.kotlin.wrappers.get()}"
)

for ((name, dependency) in versionCatalogsToMerge) {
    val versionCatalogCollector = configurations.create("versionCatalogCollectorFor${name.uppercaseFirstChar()}") {
        isCanBeResolved = true
        isTransitive = false
    }
    
    dependencies {
        versionCatalogCollector(dependency)
    }
    
    versionCatalogCollector.files.filter { it.extension == "toml" }.forEach {
        val versionsFile = VersionsFile.fromTomlTable(Toml.parseToTomlTable(it.reader(Charsets.UTF_8)))
        catalog.versionCatalog {
            for ((alias, version) in versionsFile.versions)
                version(alias, version)
            for ((alias, plugin) in versionsFile.plugins) {
                val pluginSpec = plugin("$name-$alias", plugin.id)
                when (val version = plugin.version) {
                    is VersionsFile.Version.Raw -> pluginSpec.version(version.value)
                    is VersionsFile.Version.Ref -> pluginSpec.versionRef(version.value)
                }
            }
            for ((alias, library) in versionsFile.libraries) {
                val librarySpec = library("$name-$alias", library.group, library.name)
                when (val version = library.version) {
                    is VersionsFile.Version.Raw -> librarySpec.version(version.value)
                    is VersionsFile.Version.Ref -> librarySpec.versionRef(version.value)
                }
            }
            for ((alias, bundle) in versionsFile.bundles)
                bundle("$name-$alias", bundle.map { "$name-$it" })
        }
    }
}

mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    
    signAllPublications()
    
    coordinates(groupId = project.group as String, artifactId = "versions", version = project.version as String)
    
    pom {
        name = "versions"
        description = "Common versions that I use in my projects"
        url = "https://github.com/lounres/versions"
        
        licenses {
            license {
                name = "Apache License, Version 2.0"
                url = "https://opensource.org/license/apache-2-0/"
            }
        }
        developers {
            developer {
                id = "lounres"
                name = "Gleb Minaev"
                email = "minaevgleb@yandex.ru"
            }
        }
        scm {
            url = "https://github.com/lounres/versions"
        }
    }
}