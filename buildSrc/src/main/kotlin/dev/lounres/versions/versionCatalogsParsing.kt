package dev.lounres.versions

import kotlinx.serialization.Serializable
import net.peanuuutz.tomlkt.Toml
import net.peanuuutz.tomlkt.decodeFromNativeReader
import java.io.File


@Serializable
data class VersionsFile(
    val metadata: Metadata? = null,
    val versions: Map<String, String> = emptyMap(),
    val plugins: Map<String, Plugin> = emptyMap(),
    val libraries: Map<String, Library> = emptyMap(),
    val bundles: Map<String, List<String>> = emptyMap(),
) {
    @Serializable
    data class Metadata(
        val format: Format,
    ) {
        @Serializable
        data class Format(
            val version: String,
        )
    }
    
    @Serializable
    data class Version(
        val ref: String,
    )
    
    @Serializable
    data class Plugin(
        val id: String,
        val version: Version,
    )
    
    @Serializable
    data class Library(
        val group: String,
        val name: String,
        val version: Version,
    )
}

fun File.parseVersions(): VersionsFile = Toml.decodeFromNativeReader(reader(Charsets.UTF_8))