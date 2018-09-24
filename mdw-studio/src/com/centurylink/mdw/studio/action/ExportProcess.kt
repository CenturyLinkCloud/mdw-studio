package com.centurylink.mdw.studio.action

import com.centurylink.mdw.bpmn.BpmnProcessExporter
import com.centurylink.mdw.export.ProcessExporter
import com.centurylink.mdw.html.HtmlProcessExporter
import com.centurylink.mdw.image.PngProcessExporter
import com.centurylink.mdw.model.workflow.Process
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import org.json.JSONObject
import java.io.IOException

class ExportProcess : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        val projectSetup = locator.getProjectSetup()
        if (projectSetup != null) {
            val ext = when (templatePresentation.text) {
                "PNG Image" -> "png"
                "HTML Document" -> "html"
                else -> "bpmn"
            }
            locator.getAsset()?.let { asset ->
                if (asset.ext == "proc") {
                    val process = Process(JSONObject(String(asset.file.contentsToByteArray())))
                    process.name = asset.rootName

                    val descriptor = FileSaverDescriptor("Export " + templatePresentation.text, "Export process to " + ext, ext)
                    val saveFileDialog = FileChooserFactory.getInstance().createSaveFileDialog(descriptor, projectSetup.project)
                    saveFileDialog.save(asset.file.parent, asset.rootName + "." + ext)?.let { fileWrapper ->
                        val exporter: ProcessExporter = when (ext) {
                            "png" -> {
                                PngProcessExporter(projectSetup)
                            }
                            "html" -> {
                                val exp = HtmlProcessExporter(projectSetup)
                                exp.setOutputDir(fileWrapper.file.parentFile)
                                exp
                            }
                            else -> {
                                BpmnProcessExporter()
                            }
                        }

                        WriteAction.run<IOException> {
                            fileWrapper.getVirtualFile(true)?.let {
                                val theExporter = exporter
                                val originalLoader = Thread.currentThread().contextClassLoader
                                try {
                                    if (theExporter is BpmnProcessExporter) {
                                        Thread.currentThread().contextClassLoader = this.javaClass.classLoader
                                    }
                                    it.setBinaryContent(theExporter.export(process))
                                    FileEditorManager.getInstance(projectSetup.project).openFile(it, true)
                                }
                                finally {
                                    Thread.currentThread().contextClassLoader = originalLoader
                                }
                            }
                        }

                    }
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        super.update(event)
//        val applicable = getAsset(event)?.ext == "proc"
//        event.presentation.isVisible = applicable
//        event.presentation.isEnabled = applicable
    }
}