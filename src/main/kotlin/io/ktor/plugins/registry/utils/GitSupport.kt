/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import io.ktor.plugins.registry.utils.GitSupport.Remote.Main
import io.ktor.plugins.registry.utils.GitSupport.Remote.Fork
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
import org.eclipse.jgit.transport.SshSessionFactory
import org.eclipse.jgit.transport.sshd.SshdSessionFactory
import org.eclipse.jgit.treewalk.AbstractTreeIterator
import org.eclipse.jgit.treewalk.CanonicalTreeParser
import org.eclipse.jgit.treewalk.FileTreeIterator
import org.eclipse.jgit.util.io.DisabledOutputStream
import java.io.File

object GitSupport {

    /**
     * Generate a diff of all files compared to the given branch (main).  Any files changed under "plugins/server..."
     * will be included in the changed plugins list.
     *
     * Forks are also supported here - when there are multiple remotes, we'll compare to the non-"origin" remote.
     *
     * @param mainBranchName branch to compare your local copy with
     * @param expectedRemoteGroup the org name for the upstream project "ktorio"
     * @param target target directory to look for plugin changes
     */
    fun getChangedPluginIds(
        mainBranchName: String = "main",
        expectedRemoteGroup: String = "ktorio",
        target: String = "server",
    ): List<String> {
        // initialize the ssh factory for remote fetching
        SshSessionFactory.setInstance(SshdSessionFactory())

        val git = Git.open(File(""))
        val repository = git.repository
        val currentIndex = FileTreeIterator(repository)
        val mainTree = git.getMainTree(repository, mainBranchName, expectedRemoteGroup)
        val diffEntries = DiffFormatter(DisabledOutputStream.INSTANCE).run {
            setRepository(repository)
            scan(currentIndex, mainTree)
        }

        val extractPluginId = Regex("plugins/$target/[^/]+/([^/]+).*")
        val changedPluginIds = diffEntries.asSequence().mapNotNull {
            extractPluginId.matchEntire(it.newPath)?.let { pluginMatch ->
                    pluginMatch.groups[1]?.value
                }
        }.distinct()

        return changedPluginIds.toList()
    }

    private fun Git.getMainTree(
        repository: Repository,
        mainBranchName: String,
        expectedRemoteGroup: String,
    ): AbstractTreeIterator {
        val remotes = getRemotes(repository, expectedRemoteGroup)

        return when(remotes) {
            // when there is no upstream, we must add it
            is Fork -> {
                val upstream = "upstream"
                val remoteUrl = remotes.url.replaceGitName(expectedRemoteGroup)
                addRemote(upstream, remoteUrl)
                fetchRemoteBranch(upstream, mainBranchName)
                getTreeIteratorForRemoteBranch(repository, mainBranchName, upstream)
            }
            // when only main repository, we only need to compare branches
            is Main -> getTreeIteratorForBranch(repository, mainBranchName)
            // compare with upstream normally
            is Remotes.MainAndFork -> {
                fetchRemoteBranch(remotes.main, mainBranchName)
                getTreeIteratorForRemoteBranch(repository, mainBranchName, remotes.main)
            }
            is Remotes.Multiple -> throw IllegalArgumentException("Multiple remotes found, can't infer changes")
            is Remotes.None -> throw IllegalArgumentException("Couldn't find remotes, can't infer changes")
        }
    }

    private fun getRemotes(repo: Repository, expectedRemoteGroup: String): Remotes {
        val config = repo.config
        return config.getSubsections("remote").map { remoteName ->
            val remoteUrl = config.getString("remote", remoteName, "url")
            when(remoteUrl.gitUsername) {
                expectedRemoteGroup -> Main(remoteName, remoteUrl)
                else -> Fork(remoteName, remoteUrl)
            }
        }.run {
            when(size) {
                0 -> Remotes.None
                1 -> get(0)
                2 -> firstOrNull { it is Main }?.let { main ->
                    Remotes.MainAndFork(
                        main.name,
                        minus(main).first().name
                    )
                } ?: Remotes.Multiple
                else -> Remotes.Multiple
            }
        }
    }

    private fun Git.addRemote(remoteName: String, remoteUrl: String) {
        remoteAdd()
            .setName(remoteName)
            .setUri(org.eclipse.jgit.transport.URIish(remoteUrl))
            .call()
    }

    private fun Git.fetchRemoteBranch(remoteName: String, branch: String) {
        fetch()
            .setRemote(remoteName)
            .setRefSpecs("+refs/heads/$branch:refs/remotes/$remoteName/$branch")
            .call()
    }

    private fun getTreeIteratorForBranch(repo: Repository, name: String): AbstractTreeIterator {
        val objectId = repo.resolve("${name}^{tree}")
        val reader = repo.newObjectReader()

        val treeParser = CanonicalTreeParser()
        treeParser.reset(reader, objectId)

        return treeParser
    }

    private fun getTreeIteratorForRemoteBranch(
        repo: Repository,
        name: String,
        remoteName: String
    ): AbstractTreeIterator {
        val objectId = repo.resolve("refs/remotes/$remoteName/$name^{tree}")
        val reader = repo.newObjectReader()

        val treeParser = CanonicalTreeParser()
        treeParser.reset(reader, objectId)

        return treeParser
    }

    sealed interface Remotes {
        data class MainAndFork(val main: String, val fork: String): Remotes
        data object Multiple: Remotes
        data object None: Remotes
    }

    sealed class Remote(
        val name: String,
        val url: String
    ): Remotes {

        class Main(name: String, url: String): Remote(name, url)
        class Fork(name: String, url: String): Remote(name, url)
    }

    private val sshRegex = Regex("""git@github\.com:([^/]+)/.+?\.git""")
    private val httpsRegex = Regex("""https://github\.com/([^/]+)/.+?\.git""")

    private fun String.replaceGitName(newValue: String): String {
        val currentName = (sshRegex.matchEntire(this) ?: httpsRegex.matchEntire(this))?.let { match ->
            match.groups[1]
        } ?: throw IllegalArgumentException("Cannot infer username from $this")

        return replace(currentName.value, newValue)
    }
    private val String.gitUsername: String
        get() {
            return (sshRegex.matchEntire(this) ?: httpsRegex.matchEntire(this))?.let { match ->
                match.groups[1]?.value
            } ?: throw IllegalArgumentException("Cannot infer username from $this")
        }

}
