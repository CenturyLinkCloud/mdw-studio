package com.centurylink.mdw.studio.action

import com.centurylink.mdw.bpmn.BpmnProcessImporter
import com.centurylink.mdw.drawio.DrawIoProcessImporter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileEditor.FileEditorManager
import java.io.File
import java.io.IOException

class ImportProcess : PackageAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val ext = when (templatePresentation.text) {
            "From draw.io Diagram" -> "xml"
            else -> "bpmn"
        }

        val locator = Locator(event)
        val projectSetup = locator.projectSetup
        if (projectSetup != null) {
            locator.selectedPackage?.let { pkg ->
                val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                        .withFileFilter { it.extension == ext }
                FileChooser.chooseFile(descriptor, projectSetup.project, pkg.dir) { file ->
                    val importer = when(ext) {
                        "xml" -> DrawIoProcessImporter()
                        else -> BpmnProcessImporter()
                    }
                    val process = importer.importProcess(File(file.path))
                    WriteAction.run<IOException> {
                        val procFileName =  file.name.substring(0, file.name.lastIndexOf('.')) + ".proc"
                        val procFile = pkg.dir.findFileByRelativePath(procFileName) ?:
                                pkg.dir.createChildData(this, procFileName)
                        procFile.setBinaryContent(process.json.toString(2).toByteArray())
                        FileEditorManager.getInstance(projectSetup.project).openFile(procFile, true)
                    }
                }
            }
        }
    }
}