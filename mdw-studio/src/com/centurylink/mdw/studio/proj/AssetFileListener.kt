package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.annotations.Activity
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetEvent
import com.centurylink.mdw.studio.file.AssetEvent.EventType
import com.centurylink.mdw.studio.file.AssetPackage
import com.centurylink.mdw.studio.file.AttributeVirtualFileSystem
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.io.FileUtilRt
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vcs.impl.ProjectLevelVcsManagerImpl
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCopyEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFileMoveEvent
import com.intellij.testFramework.LightVirtualFile
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*

class AssetFileListener(private val projectSetup: ProjectSetup) : BulkFileListener {

    override fun before(events: MutableList<out VFileEvent>) {
    }

    override fun after(events: MutableList<out VFileEvent>) {
        if (!isVcsOperationInProgress(ProjectLevelVcsManager.getInstance(projectSetup.project))) {
            for (event in events) {
                getAssetEvent(event)?.let { assetEvent ->
                    LOG.debug("Asset event: $assetEvent")
                    val asset = assetEvent.asset
                    when (assetEvent.type) {
                        EventType.Create -> {
                            if (asset.ext == "proc") {
                                AttributeVirtualFileSystem.instance.loadAttributeVirtualFiles(projectSetup, asset)
                            }
                            projectSetup.setVersion(asset, 1)
                        }
                        EventType.Update, EventType.Copy, EventType.Move -> {
                            if (asset.ext == "proc") {
                                AttributeVirtualFileSystem.instance.loadAttributeVirtualFiles(projectSetup, asset)
                            }
                            projectSetup.git?.let { git ->
                                if (asset.name.endsWith(".impl")) {
                                    projectSetup.reloadImplementors()
                                } else if (asset.name.endsWith(".evth")) {
                                    // not asset
                                } else if (asset.version == 0) {
                                    projectSetup.setVersion(asset, 1)
                                } else if (FileUtilRt.isTooLarge(asset.file.length)) {
                                    LOG.info("Skip vercheck for large asset: $asset")
                                } else {
                                    LOG.debug("Performing vercheck: $asset")
                                    val gitPkgPath = git.getRelativePath(File(asset.pkg.dir.path).toPath())
                                    val gitBytes = git.readFromHead("$gitPkgPath/${asset.name}")
                                    val assetBytes = asset.file.contentsToByteArray()
                                    var isGitDiff = false
                                    if (gitBytes != null && !Arrays.equals(gitBytes, assetBytes)) {
                                        isGitDiff = if (projectSetup.data.getBinaryAssetExts().contains(asset.ext)) {
                                            true
                                        } else {
                                            // ignore line ending diffs
                                            val gitString = String(gitBytes).replace("\r", "")
                                            val assetString = String(assetBytes).replace("\r", "")
                                            gitString != assetString
                                        }
                                    }
                                    if (isGitDiff) {
                                        val gitVerFileBytes = git.readFromHead("$gitPkgPath/${AssetPackage.VERSIONS_FILE}")
                                        gitVerFileBytes?.let {
                                            val gitVerProps = Properties()
                                            gitVerProps.load(ByteArrayInputStream(gitVerFileBytes))
                                            val gitVerProp = gitVerProps.getProperty(asset.name)
                                            if (gitVerProp != null) {
                                                val headVer = gitVerProp.split(" ")[0].toInt()
                                                if (headVer >= asset.version) {
                                                    projectSetup.setVersion(asset, headVer + 1)
                                                }
                                            }
                                        }
                                    }
                                    if (asset.name.endsWith(".java") || asset.name.endsWith(".kt")) {
                                        DumbService.getInstance(projectSetup.project).smartInvokeLater {
                                            projectSetup.findPsiAnnotation(asset, Activity::class)?.let { psiAnnotation ->
                                                Implementors.getImpl(projectSetup, asset, psiAnnotation)?.let { projectSetup.reloadImplementors() }
                                            }
                                        }
                                    }
                                }
                            }
                            if (event is VFileMoveEvent) {
                                projectSetup.getPackage(event.oldParent)?.let { oldPkg ->
                                    val oldAsset = Asset(oldPkg, LightVirtualFile(event.file.name))
                                    projectSetup.setVersion(oldAsset, 0)
                                }
                            }
                        }
                        EventType.Delete -> {
                            projectSetup.setVersion(asset, 0)
                            if (asset.ext == "proc") {
                                AttributeVirtualFileSystem.instance.removeAttributeVirtualFiles(projectSetup, asset)
                            }
                        }
                        EventType.Unknown -> {
                        }
                    }
                }
            }
        }
    }

    private fun isVcsOperationInProgress(vcsManager: ProjectLevelVcsManager) : Boolean {
        if (vcsManager is ProjectLevelVcsManagerImpl) {
            return vcsManager.isBackgroundVcsOperationRunning()
        }
        else {
            return false
        }
    }

    private fun getAssetEvent(event: VFileEvent): AssetEvent? {
        val eventFile = if (event is VFileCopyEvent) {
            event.newParent.findFileByRelativePath(event.newChildName)
        }
        else {
            event.file
        }
        eventFile?.let {
            if (!it.isDirectory) {
                if (projectSetup.isAssetSubdir(it.parent)) {
                    if (!Asset.isIgnore(it)) {
                        // we care about this file
                        val asset = projectSetup.getAsset(it) ?: projectSetup.createAsset(it)
                        return AssetEvent(event, asset)
                    }
                }
            }
        }
        return null
    }

    companion object {
        val LOG = Logger.getInstance(AssetFileListener::class.java)
    }
}