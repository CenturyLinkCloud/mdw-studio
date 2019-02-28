package com.centurylink.mdw.studio.action

import com.centurylink.mdw.cli.Delete
import com.centurylink.mdw.discovery.GitDiscoverer
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.notification.Notifications
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.progress.impl.BackgroundableProcessIndicator
import com.intellij.openapi.vfs.VfsUtil
import git4idea.commands.GitCommand
import git4idea.commands.GitImpl
import git4idea.commands.GitLineHandler
import git4idea.commands.GitStandardProgressAnalyzer
import java.io.File
import java.io.IOException
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/**
 * TODO: Server side filtering via Git Protocol v2.
 * GitHub support: https://github.blog/changelog/2018-11-08-git-protocol-v2-support/
 * GitLab support: https://gitlab.com/gitlab-org/gitlab-ce/issues/46555 (v11.4)
 * However, client must be configured with protocol.version 2 (unlikely).
 */
class GitImport(private val projectSetup: ProjectSetup, private val discoverer: GitDiscoverer) :
            Task.Backgroundable(projectSetup.project, "Import MDW Assets") {

    private var tempDir = Files.createTempDirectory("mdw-studio-")
    private var packages = listOf<String>()

    fun doImport(packages: List<String>) {
        this.packages = packages
        ProgressManager.getInstance().runProcessWithProgressAsynchronously(this,
                BackgroundableProcessIndicator(this))
    }

    override fun run(indicator: ProgressIndicator) {
        try {
            clone(indicator)
            move(indicator)
        }
        catch (ex: Exception) {
            LOG.warn(ex)
            Notifications.Bus.notify(Notification("MDW", "Asset Import Error", ex.toString(),
                    NotificationType.ERROR), projectSetup.project)
        }
        finally {
            try {
                Delete(tempDir.toFile(), true).run();
            }
            catch (ex: Exception) {
                LOG.warn(ex)
                Notifications.Bus.notify(Notification("MDW", "Failed to delete Temp directory after asset import: ${tempDir}", ex.toString(),
                        NotificationType.WARNING), projectSetup.project)
            }
        }
    }

    private fun clone(indicator: ProgressIndicator) {
        val git = GitImpl()
        val projectLocation = File(projectSetup.project.presentableUrl)
        // If system temp location is on different drive (i.e in WindowsOS) then we cannot move directory due to
        // bug in JDK, so create temp location for cloning on same drive as the project
        if (projectLocation.toString().length > 3 && ":\\".equals(projectLocation.toString().substring(1,3)) && !projectLocation.toString().get(0).equals(tempDir.toString().get(0))) {
            Delete(tempDir.toFile(), true).run()
            if (projectLocation.parent == null)
                tempDir = Files.createDirectories(File("${projectLocation}/${tempDir.fileName}").toPath())
            else
                tempDir = Files.createDirectories(File("${projectLocation.parent}/${tempDir.fileName}").toPath())
        }
        LOG.info("Cloning $discoverer to: $tempDir")
        indicator.isIndeterminate = false
        indicator.text2 = "Retrieving project..."
        val progressListener = GitStandardProgressAnalyzer.createListener(indicator)
        git.runCommand {
            val url = discoverer.repoUrl.toString()
            val handler = GitLineHandler(projectSetup.project, tempDir.toFile(), GitCommand.CLONE)
            handler.setSilent(false)
            handler.setStderrSuppressed(false)
            handler.setUrl(url)
            handler.addParameters("--progress")
            handler.addParameters("-b", discoverer.ref)
            handler.addParameters("--single-branch")
            handler.addParameters("--depth", "1")
            handler.addParameters(url)
            handler.endOptions()
            // handler.addParameters(clonedDirectoryName)
            handler.addLineListener(progressListener)
            handler
        }
    }

    private fun move(indicator: ProgressIndicator) {
        indicator.fraction = 0.0
        indicator.text2 = "Moving packages..."
        indicator.isIndeterminate = false

        for ((i, pkg) in packages.withIndex()) {
            val pkgPath = pkg.replace('.','/')
            val src = File("$tempDir/${discoverer.repoName}/${discoverer.assetPath}/$pkgPath").toPath()
            val dest = File("${projectSetup.assetRoot}/$pkgPath").toPath()

            try {
                Files.createDirectories(dest)
                Delete(dest.toFile(), true).run()
                Files.move(src, dest, StandardCopyOption.REPLACE_EXISTING)
            } catch (ex: IOException) {
                LOG.warn(ex)
                Notifications.Bus.notify(Notification("MDW", "Asset Import Error", ex.toString(),
                        NotificationType.ERROR), projectSetup.project)
            }
            indicator.fraction = i.toDouble() / packages.size
        }
        VfsUtil.markDirtyAndRefresh(true, true, true, projectSetup.assetDir)
        projectSetup.reloadImplementors()
        indicator.stop()
    }

    companion object {
        val LOG = Logger.getInstance(GitImport::class.java)
    }
}

