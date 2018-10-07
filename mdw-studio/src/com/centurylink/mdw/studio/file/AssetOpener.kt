package com.centurylink.mdw.studio.file

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.INativeFileType
import com.intellij.openapi.project.Project

class AssetOpener(private val projectSetup: ProjectSetup, private val asset: Asset) {

    fun doOpen() {
        val editors = FileEditorManager.getInstance(projectSetup.project).openFile(asset.file, true)
        if (editors.isEmpty()) {
            asset.file.extension?.let { ext ->
                FileTypeManager.getInstance().getFileTypeByExtension(ext).let { fileType ->
                    if (fileType is INativeFileType) {
                        fileType.openFileInAssociatedApplication(projectSetup.project, asset.file)
                    }
                }
            }
        }
    }
}