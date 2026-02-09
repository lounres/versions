rootProject.name = "versions"

val localProperties = java.util.Properties()
file("local.properties").let { localPropertiesFile ->
    if (localPropertiesFile.exists()) localPropertiesFile.inputStream().use {
        localProperties.load(it)
    }
}

gradle.projectsLoaded {
    for ((key, property) in localProperties) gradle.rootProject.extra[key.toString()] = property
}