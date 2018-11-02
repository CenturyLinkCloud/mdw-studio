package com.centurylink.mdw.studio.action

import com.centurylink.mdw.bpmn.BpmnProcessExporter
import com.centurylink.mdw.cli.Download
import com.centurylink.mdw.export.ProcessExporter
import com.centurylink.mdw.html.HtmlProcessExporter
import com.centurylink.mdw.image.PngProcessExporter
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.pdf.PdfProcessExporter
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.fileChooser.FileChooserFactory
import com.intellij.openapi.fileChooser.FileSaverDescriptor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.util.lang.UrlClassLoader
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.URL

class ExportProcess : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        val projectSetup = locator.getProjectSetup()
        if (projectSetup != null) {
            val ext = when (templatePresentation.text) {
                "PNG Image" -> "png"
                "HTML Document" -> "html"
                "PDF Document" -> "pdf"
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
                            "pdf" -> {
                                val tempDir = File(projectSetup.project.basePath + "/.temp")
                                if (!tempDir.exists())
                                    tempDir.mkdirs()
                                val itextJar = File(tempDir.path + "/itextpdf-5.5.13.jar")
                                if (!itextJar.isFile) {
                                    Download(URL("http://repo.maven.apache.org/maven2/com/itextpdf/itextpdf/5.5.13/itextpdf-5.5.13.jar"),
                                            itextJar, 2320581L).run(com.centurylink.mdw.studio.ui.ProgressMonitor(ProgressIndicatorBase()))
                                }
                                val xmlWorkerJar = File(tempDir.path + "/xmlworker-5.5.13.jar")
                                if (!xmlWorkerJar.isFile) {
                                    Download(URL("http://repo.maven.apache.org/maven2/com/itextpdf/tool/xmlworker/5.5.13/xmlworker-5.5.13.jar"),
                                            xmlWorkerJar, 2320581L).run(com.centurylink.mdw.studio.ui.ProgressMonitor(ProgressIndicatorBase()))
                                }
                                val classLoader = this.javaClass.classLoader as UrlClassLoader
                                val method = UrlClassLoader::class.java.getDeclaredMethod("addURL", URL::class.java)
                                method.isAccessible = true
                                method.invoke(classLoader, itextJar.toURI().toURL())
                                method.invoke(classLoader, xmlWorkerJar.toURI().toURL())
                                val exp = PdfProcessExporter(projectSetup)
                                exp.setOutputDir(fileWrapper.file)
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