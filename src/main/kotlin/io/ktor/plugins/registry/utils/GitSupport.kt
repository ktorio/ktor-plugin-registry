/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package io.ktor.plugins.registry.utils

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.diff.DiffFormatter
import org.eclipse.jgit.lib.Repository
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
     * @param target target directory to look for plugin changes
     * @param defaultOrigin the remote for my fork
     */
    fun getChangedPluginIds(
        mainBranchName: String = "main",
        target: String = "server",
        defaultOrigin: String = "origin"
    ): List<String> {
        val repository = Git.open(File("")).repository
        val currentIndex = FileTreeIterator(repository)
        val remoteName = repository.config.getSubsections("remote").takeIf { it.size > 1 }?.last { it != defaultOrigin }
        val mainBranch = when(remoteName) {
            null -> getTreeIteratorForBranch(repository, mainBranchName)
            else -> getTreeIteratorForRemoteBranch(repository, mainBranchName, remoteName)
        }
        val diffEntries = DiffFormatter(DisabledOutputStream.INSTANCE).run {
            setRepository(repository)
            scan(currentIndex, mainBranch)
        }

        val extractPluginId = Regex("plugins/$target/[^/]+/([^/]+).*")
        val changedPluginIds = diffEntries.asSequence().mapNotNull {
            extractPluginId.matchEntire(it.newPath)?.let { pluginMatch ->
                    pluginMatch.groups[1]?.value
                }
        }.distinct()

        return changedPluginIds.toList()
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

}
