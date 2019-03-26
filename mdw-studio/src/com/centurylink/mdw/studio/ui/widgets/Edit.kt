package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.isReadonly
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.file.AttributeVirtualFile
import com.centurylink.mdw.studio.file.AttributeVirtualFileSystem
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import java.io.IOException
import javax.swing.BorderFactory

/**
 * For in-place editing of dynamic java, scripts, etc.
 */
@Suppress("unused")
class Edit(widget: Pagelet.Widget) : SwingWidget(widget) {

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(1, 3, 1, 0)

        val linkLabel = LinkLabel(if (widget.isReadonly) "View" else "Edit")
        linkLabel.toolTipText = "Open ${widget.attributes["languages"]}"
        linkLabel.clickListener = {
            openEditor()
        }
        add(linkLabel)
    }

    private fun openEditor() {
        val qualifier = when(widget.name) {
            "PreScript" -> "Pre"
            "PostScript" -> "Post"
            else -> null
        }

        val fileSystem = AttributeVirtualFileSystem.instance
        val javaOrScriptFile = fileSystem.getJavaOrScriptFile(workflowObj, widget.valueString, qualifier)
        // TODO: reuse non Java or Script files
        val virtualFile = javaOrScriptFile ?: AttributeVirtualFile(workflowObj, widget.valueString, qualifier = qualifier)

        if (virtualFile.contents != widget.valueString) {
            // might have been set from template
            widget.value = virtualFile.contents
            applyUpdate()
        }
        val project = (workflowObj.project as ProjectSetup).project
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: throw IOException("No document: " + virtualFile.path)

        document.addDocumentListener(object: DocumentListener {
            override fun beforeDocumentChange(e: DocumentEvent) {
            }
            override fun documentChanged(e: DocumentEvent) {
                widget.value = e.document.text
                applyUpdate()
            }
        })

        if (virtualFile.extension == "java") {
            ApplicationManager.getApplication().invokeLater {
                val className = virtualFile.syncDynamicJavaClassName()
                workflowObj.setAttribute("ClassName", className)
                notifyUpdateListeners(workflowObj)
            }
        }

        val descriptor = OpenFileDescriptor(project, virtualFile)
        FileEditorManager.getInstance(project).openTextEditor(descriptor, true)
    }
}