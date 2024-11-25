import dev.lounres.versions.parseVersions
import org.gradle.kotlin.dsl.support.uppercaseFirstChar
import java.time.LocalDate
import java.time.ZoneId

plugins {
    `version-catalog`
    `maven-publish`
    signing
    alias(libs.plugins.nexus.publish.plugin)
}

repositories {
    mavenCentral()
}

val today: LocalDate = LocalDate.now(ZoneId.of("UTC"))
version = "${today.year}.${today.month.value}.${today.dayOfMonth}"

catalog.versionCatalog {
    from(files("gradle/libs.versions.toml"))
}

val versionCatalogsToMerge: Map<String, String> = mapOf(
    "logKube" to "dev.lounres:logKube.versionCatalog:${libs.versions.logKube.get()}",
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

// GR-26091
tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

publishing {
    publications {
        create<MavenPublication>("versionCatalog") {
            artifactId = "versions"
            from(components["versionCatalog"])
        }
    }
    publications.withType<MavenPublication> {
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
}

signing {
    sign(publishing.publications)
}

nexusPublishing {
    repositories {
        sonatype {
            nexusUrl.set(uri("https://s01.oss.sonatype.org/service/local/"))
            snapshotRepositoryUrl.set(uri("https://s01.oss.sonatype.org/content/repositories/snapshots/"))
        }
    }
}