package dev.lounres.versions

import net.peanuuutz.tomlkt.TomlArray
import net.peanuuutz.tomlkt.TomlElement
import net.peanuuutz.tomlkt.TomlLiteral
import net.peanuuutz.tomlkt.TomlTable


data class VersionsFile(
    val versions: Map<String, String> = emptyMap(),
    val plugins: Map<String, Plugin> = emptyMap(),
    val libraries: Map<String, Library> = emptyMap(),
    val bundles: Map<String, List<String>> = emptyMap(),
) {
    sealed interface Version {
        data class Raw(val value: String) : Version
        data class Ref(val value: String) : Version
        
        companion object {
            fun fromToml(toml: TomlElement): Version =
                when (toml) {
                    is TomlLiteral -> {
                        check(toml.type == TomlLiteral.Type.String)
                        Raw(toml.content)
                    }
                    is TomlTable -> Ref((toml["ref"] as TomlLiteral).content)
                    else -> error("Unexpected toml element type to parse version.")
                }
        }
    }
    
    data class Plugin(
        val id: String,
        val version: Version,
    ) {
        companion object {
            fun fromTomlTable(toml: TomlTable): Plugin =
                Plugin(
                    id = (toml["id"] as TomlLiteral).also { check(it.type == TomlLiteral.Type.String) }.content,
                    version = Version.fromToml(toml["version"]!!)
                )
        }
    }
    
    data class Library(
        val group: String,
        val name: String,
        val version: Version,
    ) {
        companion object {
            fun fromTomlTable(toml: TomlTable): Library {
                val moduleLiteral = toml["module"]
                val groupLiteral = toml["group"]
                val nameLiteral = toml["name"]
                val group: String
                val name: String
                val coordinates = when {
                    moduleLiteral is TomlLiteral -> {
                        check(moduleLiteral.type == TomlLiteral.Type.String)
                        val parts = moduleLiteral.content.split(':')
                        group = parts[0]
                        name = parts[1]
                    }
                    groupLiteral is TomlLiteral && nameLiteral is TomlLiteral -> {
                        check(groupLiteral.type == TomlLiteral.Type.String && nameLiteral.type == TomlLiteral.Type.String)
                        group = groupLiteral.content
                        name = nameLiteral.content
                    }
                    else -> error("Cannot parse library coordinates.")
                }
                return Library(
                    group = group,
                    name = name,
                    version = Version.fromToml(toml["version"]!!)
                )
            }
        }
    }
    
    companion object {
        fun fromTomlTable(toml: TomlTable): VersionsFile =
            VersionsFile(
                versions = (toml["versions"] as TomlTable?)?.mapValues { (_, version) ->
                    check(version is TomlLiteral && version.type == TomlLiteral.Type.String)
                    version.content
                } ?: emptyMap(),
                plugins = (toml["plugins"] as TomlTable?)?.mapValues { (_, plugin) ->
                    check(plugin is TomlTable)
                    Plugin.fromTomlTable(plugin)
                } ?: emptyMap(),
                libraries = (toml["libraries"] as TomlTable?)?.mapValues { (_, library) ->
                    check(library is TomlTable)
                    Library.fromTomlTable(library)
                } ?: emptyMap(),
                bundles = (toml["bundles"] as TomlTable?)?.mapValues { (_, bundle) ->
                    check(bundle is TomlArray)
                    bundle.map {
                        check(it is TomlLiteral && it.type == TomlLiteral.Type.String)
                        it.content
                    }
                } ?: emptyMap(),
            )
    }
}

//fun File.parseVersionsFile(): VersionsFile {
//    val versionsFileTable = Toml.parseToTomlTable(reader(Charsets.UTF_8))
//}