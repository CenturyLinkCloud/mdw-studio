package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.annotations.Activity
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetEvent
import com.centurylink.mdw.studio.file.AssetEvent.EventType
import com.centurylink.mdw.studio.file.AssetPackage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.io.FileUtilRt
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
        for (event in events) {
            getAssetEvent(event)?.let { assetEvent ->
                LOG.debug("Asset event: $assetEvent")
                val asset = assetEvent.asset
                when (assetEvent.type) {
                    EventType.Create -> {
                        projectSetup.setVersion(asset, 1)
                    }
                    EventType.Update, EventType.Copy, EventType.Move -> {
                        // TODO handle large files
                        val increment = projectSetup.git?.let { git ->
                            if (asset.name.endsWith(".impl")) {
                                projectSetup.reloadImplementors()
                            }
                            else if (asset.version == 0) {
                                projectSetup.setVersion(asset, 1)
                            }
                            else if (FileUtilRt.isTooLarge(asset.file.length)) {
                                LOG.info("Skip vercheck for large asset: $asset")
                            }
                            else {
                                LOG.debug("Performing vercheck: $asset")
                                val gitPkgPath = git.getRelativePath(File(asset.pkg.dir.path).toPath())
                                val gitAssetBytes = git.readFromHead("$gitPkgPath/${asset.name}")
                                if (gitAssetBytes != null && !Arrays.equals(gitAssetBytes, asset.file.contentsToByteArray())) {
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
                                            Implementors.getImpl(asset, psiAnnotation)?.let { projectSetup.reloadImplementors() }
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
                    }
                    EventType.Unknown -> {
                    }
                }
            }
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