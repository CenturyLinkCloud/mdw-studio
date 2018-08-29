package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.studio.file.AssetEvent
import com.centurylink.mdw.studio.file.AssetEvent.EventType
import com.centurylink.mdw.studio.file.AssetPackage
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.io.ByteArrayInputStream
import java.io.File
import java.util.*

class AssetFileListener(private val projectSetup: ProjectSetup) : BulkFileListener {

    override fun before(events: MutableList<out VFileEvent>) {
    }

    override fun after(events: MutableList<out VFileEvent>) {
        for (event in events) {
            getAssetEvent(event)?.let { assetEvent ->
                val asset = assetEvent.asset
                when (assetEvent.type) {
                    EventType.Create -> {
                        projectSetup.setVersion(asset, 1)
                    }
                    EventType.Update -> {
                        val increment = projectSetup.git?.let { git ->
                            // TODO: ignore vcs revert
                            val gitVerFileBytes = git.readFromHead(git.getRelativePath(
                                    File(asset.pkg.dir.path + "/" + AssetPackage.VERSIONS_FILE)))
                            gitVerFileBytes?.let {
                                val gitVerProps = Properties()
                                gitVerProps.load(ByteArrayInputStream(gitVerFileBytes))
                                val gitVerProp = gitVerProps.getProperty(asset.name)
                                gitVerProp?.let {
                                    val headVer = gitVerProp.split(" ")[0].toInt()
                                    if (headVer >= asset.version) {
                                        projectSetup.setVersion(asset, headVer + 1)
                                    }
                                }
                            }
                        }
                    }
                    EventType.Delete -> {
                        projectSetup.setVersion(asset, 0)
                    }
                }
            }
        }
    }

    private fun getAssetEvent(event: VFileEvent): AssetEvent? {
        event.file?.let {
            if (!it.isDirectory) {
                if (projectSetup.isAssetSubdir(it.parent)) {
                    if (!AssetPackage.isMeta(it)) {
                        // we care about this file
                        var asset = projectSetup.getAsset(it) ?: projectSetup.createAsset(it)
                        return AssetEvent(event, asset)
                    }
                }
            }
        }
        return null
    }
}