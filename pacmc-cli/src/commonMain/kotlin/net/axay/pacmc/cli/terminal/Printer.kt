package net.axay.pacmc.cli.terminal

import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.rendering.TextStyles
import com.github.ajalt.mordant.terminal.Terminal
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import net.axay.pacmc.app.data.ModSlug
import net.axay.pacmc.app.data.Repository
import net.axay.pacmc.app.database.model.DbInstalledProject
import net.axay.pacmc.app.repoapi.model.CommonProjectResult
import net.axay.pacmc.app.repoapi.model.CommonProjectVersion
import net.axay.pacmc.app.repoapi.repoApiContext
import net.axay.pacmc.repoapi.CachePolicy

private val Repository.textColor
    get() = when (this) {
        Repository.MODRINTH -> TextColors.brightGreen
        Repository.CURSEFORGE -> TextColors.yellow
    }

private fun repoEntry(repository: Repository, entry: String): String {
    return repository.run { textColor(displayName.lowercase() + "/") } +
        TextColors.white(TextStyles.bold(TextStyles.underline(entry)))
}

val ModSlug.terminalString get() = repoEntry(repository, slug)

private val CommonProjectVersion.terminalString get() = repoEntry(
    modId.repository,
    (files.find { it.primary } ?: files.singleOrNull())?.name?.removeSuffix(".jar") ?: name
)

suspend fun CommonProjectVersion.optimalTerminalString(): String {
    // TODO sometimes, only the request using slugs has been pre-cached
    val projectString = repoApiContext(CachePolicy.ONLY_CACHED) { it.getBasicProjectInfo(modId) }
        ?.slug?.terminalString ?: terminalString
    return projectString + " " + TextColors.gray("($number)")
}

suspend fun DbInstalledProject.optimalTerminalString(): String = coroutineScope {
    val modId = readModId()

    val projectString = async {
        repoApiContext(CachePolicy.ONLY_CACHED) { it.getBasicProjectInfo(modId) }
            ?.slug?.terminalString ?: repoEntry(modId.repository, modId.id)
    }

    val versionString = async {
        repoApiContext(CachePolicy.ONLY_CACHED) { it.getProjectVersion(version, modId.repository) }
            ?.number ?: "version id: $version"
    }

    projectString.await() + " " + TextColors.gray("(${versionString.await()})")
}

fun Terminal.printProject(project: CommonProjectResult) = println(buildString {
    append(project.slug.terminalString)
    project.latestVersion?.let { append(" ${TextColors.brightCyan(it.toString())}") }
    append(" ${TextStyles.italic("by")} ${project.author}")
    appendLine()
    append("  ${TextColors.gray(project.description)}")
})
