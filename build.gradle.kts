import com.vanniktech.maven.publish.SonatypeHost
import dev.lounres.versions.parseVersions
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.time.LocalDateTime
import java.time.ZoneId

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
        val versionsFile = it.parseVersions()
        catalog.versionCatalog {
            for ((alias, version) in versionsFile.versions)
                version(alias, version)
            for ((alias, plugin) in versionsFile.plugins)
                plugin("$name-$alias", plugin.id).versionRef(plugin.version.ref)
            for ((alias, library) in versionsFile.libraries)
                library("$name-$alias", library.group, library.name).versionRef(library.version.ref)
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