import java.time.LocalDate
import java.time.ZoneId

plugins {
    `version-catalog`
    `maven-publish`
    signing
    alias(libs.plugins.nexus.publish.plugin)
}

val today: LocalDate = LocalDate.now(ZoneId.of("UTC"))
version = "${today.year}.${today.month.value}.${today.dayOfMonth}"

catalog.versionCatalog {
    from(files("gradle/libs.versions.toml"))
}

// GR-26091
tasks.withType<AbstractPublishToMaven>().configureEach {
    val signingTasks = tasks.withType<Sign>()
    mustRunAfter(signingTasks)
}

publishing {
    repositories {
        maven {
            name = "sonatype"
            setUrl("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")
            credentials {
                username = project.properties["ossrhUsername"].toString()
                password = project.properties["ossrhPassword"].toString()
            }
        }
    }
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