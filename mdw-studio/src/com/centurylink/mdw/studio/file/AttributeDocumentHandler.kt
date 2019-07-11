package com.centurylink.mdw.studio.file

import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.json.JSONObject


/**
 * Listens for attribute document updates due to refactoring, editing from found usages, etc.
 * (Anything not edited through process design canvas).
 */
class AttributeDocumentHandler(private val projectSetup: ProjectSetup) : DocumentListener {

    private val lockedFiles = mutableListOf<AttributeVirtualFile>()
    fun lock(file: AttributeVirtualFile) {
        lockedFiles.add(file)
    }
    fun unlock(file: AttributeVirtualFile) {
        lockedFiles.remove(file)
    }

    @Override
    override fun documentChanged(event: DocumentEvent) {
        val file = FileDocumentManager.getInstance().getFile(event.document)
        if (file is AttributeVirtualFile && !lockedFiles.contains(file)) {
            val workflowObj = file.workflowObj
            val packageName = workflowObj.asset.packageName
            var processName = workflowObj.asset.name
            if (!processName.endsWith(".proc")) {
                processName += ".proc";
            }
            projectSetup.getAsset("$packageName/$processName")?.let { processAsset ->
                synchronized(processAsset.file) {
                    val process = Process(JSONObject(String(processAsset.contents)))
                    process.activities.find { it.logicalId == workflowObj.id }?.let { activity ->
                        if (activity.getAttribute(file.attributeName) != event.document.text) {
                            activity.setAttribute(file.attributeName, event.document.text)
                            val json = process.json.toString(2).replace("\r", "")
                            WriteAction.run<Throwable> {
                                val procDoc = FileDocumentManager.getInstance().getDocument(processAsset.file)
                                if (procDoc != null) {
                                    procDoc.setText(json)
                                    FileDocumentManager.getInstance().saveDocument(procDoc)
                                }
                                else {
                                    processAsset.file.setBinaryContent(json.toByteArray())
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
