package com.centurylink.mdw.studio.file

import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import org.json.JSONObject

/**
 * TODO need listeners for updates to in-memory processes
 */
class AttributeDocumentListener(private val projectSetup: ProjectSetup) : DocumentListener {

    @Override
    override fun documentChanged(event: DocumentEvent) {
        val file = FileDocumentManager.getInstance().getFile(event.document)
        if (file is AttributeVirtualFile) {
            val workflowObj = file.workflowObj
            val packageName = workflowObj.asset.packageName
            var processName = workflowObj.asset.name
            if (!processName.endsWith(".proc")) {
                processName += ".proc";
            }
            projectSetup.getAsset("$packageName/$processName")?.let { processAsset ->
                val process = Process(JSONObject(String(processAsset.contents)))
                process.activities.find { it.logicalId == workflowObj.id }?.let { activity ->
                    activity.setAttribute(file.attributeName, event.document.text)
                    WriteAction.run<Throwable> {
                        processAsset.file.setBinaryContent(process.json.toString(2).toByteArray())
                    }
                }
            }
        }
    }
}
