package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetOpener
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.UnknownFileType

class ActivityAssetAction(private val asset: Asset) :
        AnAction("Open " + (FileTypeManager.getInstance().getFileTypeByExtension(asset.ext).let { if (it is UnknownFileType) asset.ext + " Asset" else it.name }),
                null, FileTypeManager.getInstance().getFileTypeByExtension(asset.ext).let { if (it is UnknownFileType) null else it.icon }) {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            AssetOpener(projectSetup, asset).doOpen()
        }
    }

    override fun update(event: AnActionEvent) {
        // dynamically determined in ProcessCanvas
        event.presentation.isVisible = true
        event.presentation.isEnabled = true
    }

}