package net.axay.pacmc.requests.modrinth.data

import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.axay.pacmc.data.MinecraftVersion
import net.axay.pacmc.data.ReleaseType
import net.axay.pacmc.data.Repository
import net.axay.pacmc.requests.common.CommonConvertible
import net.axay.pacmc.requests.common.data.CommonModVersion

data class ModrinthModVersion(
    val id: String,
    @SerialName("mod_id") val modId: String,
    val name: String,
    @SerialName("version_number") val versionNumber: String,
    val changelog: String? = null,
    @SerialName("date_published") val datePublished: String,
    @SerialName("version_type") val versionType: String,
    val files: List<File>,
    val dependencies: List<String>,
    @SerialName("game_versions") val gameVersions: List<String>,
    val loaders: List<String>,
) : CommonConvertible<CommonModVersion> {
    @Serializable
    data class File(
        val url: String,
        val filename: String,
    )

    override fun convertToCommon() = CommonModVersion(
        Repository.MODRINTH,
        id,
        name,
        versionNumber,
        changelog,
        Instant.parse(datePublished),
        when (versionType) {
            "release" -> ReleaseType.RELEASE
            "beta" -> ReleaseType.BETA
            "alpha" -> ReleaseType.ALPHA
            else -> error("Received invalid or unknown version type '$versionType' from Modrinth")
        },
        gameVersions.mapNotNull { MinecraftVersion.fromString(it) },
        loaders,
        files.map { CommonModVersion.File(it.url, it.filename) },
        dependencies.map { CommonModVersion.Dependency(modId, it) }
    )
}