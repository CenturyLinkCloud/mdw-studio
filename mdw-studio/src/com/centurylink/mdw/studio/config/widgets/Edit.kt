package com.centurylink.mdw.studio.config.widgets

import com.centurylink.mdw.draw.edit.apply.WidgetApplier
import com.centurylink.mdw.draw.edit.isReadonly
import com.centurylink.mdw.draw.edit.valueString
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.file.AttributeVirtualFile
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.ui.DialogBuilder
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiDocumentManager
import org.jetbrains.kotlin.idea.caches.project.moduleInfo
import org.jetbrains.kotlin.idea.util.findModule
import java.awt.Cursor
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.io.File
import java.io.IOException
import javax.swing.BorderFactory
import javax.swing.JLabel

/**
 * For in-place editing of dynamic java, scripts, etc.
 */
@Suppress("unused")
class Edit(widget: Pagelet.Widget) : SwingWidget(widget) {

    init {
        isOpaque = false
        border = BorderFactory.createEmptyBorder(1, 3, 1, 0)

        val text = if (widget.isReadonly) "View" else "Edit"
        val linkLabel = JLabel("<html><a href='.'>$text</a></html>")
        linkLabel.toolTipText = "Open ${widget.attributes["languages"]}"
        linkLabel.cursor = Cursor(Cursor.HAND_CURSOR)
        linkLabel.addMouseListener(object: MouseAdapter() {
            override fun mouseReleased(e: MouseEvent?) {
                showEditDialog()
            }
        })
        add(linkLabel)
    }

    fun showEditDialog() {
        val applier = widget.adapter as WidgetApplier
        val workflowObj = applier.workflowObj

        println("HELLO HELLO HELLO")
        val fsVirtualFile = LocalFileSystem.getInstance().findFileByIoFile(File("c:/mdw/workspaces/mdw-demo/assets/myscript.kts"))
        fsVirtualFile ?: throw IOException("no file: " + fsVirtualFile)
        val fsDocument = FileDocumentManager.getInstance().getDocument(fsVirtualFile)
        fsDocument ?: throw IOException("no doc: " + fsDocument)

        val virtualFile = AttributeVirtualFile(workflowObj, widget.valueString)
        if (virtualFile.contents != widget.valueString) {
            // might have been set from template
            widget.value = virtualFile.contents
            applyUpdate()
        }
        val project = (workflowObj.project as ProjectSetup).project
        val document = FileDocumentManager.getInstance().getDocument(virtualFile)
        document ?: throw IOException("No document: " + virtualFile.path)

//        document.addDocumentListener(object: DocumentListener {
//            override fun beforeDocumentChange(e: DocumentEvent) {
//            }
//            override fun documentChanged(e: DocumentEvent) {
//                widget.value = e.document.text
//                applyUpdate()
//            }
//        })


        fsVirtualFile.findModule(project)
        virtualFile.findModule(project)

        val fsEditor = EditorFactory.getInstance().createEditor(fsDocument, project, fsVirtualFile, false)
        val editor = EditorFactory.getInstance().createEditor(document, project, virtualFile, false)
        fsEditor.component.preferredSize = Dimension(800, 600)


        val fsPsiFile = PsiDocumentManager.getInstance(project).getPsiFile(fsDocument)
        fsPsiFile ?: throw IOException("No psiFile: " + fsPsiFile)
        // val fsScope = fsPsiFile.resolveScope
        val psiFile = PsiDocumentManager.getInstance(project).getPsiFile(document)
        psiFile ?: throw IOException("No psiFile: " + psiFile)
        //val scope = psiFile.resolveScope


        val dialogBuilder = DialogBuilder(this)
        dialogBuilder.setTitle(applier.workflowObj.titlePath)
        dialogBuilder.setActionDescriptors(DialogBuilder.CloseDialogAction())

        dialogBuilder.setCenterPanel(fsEditor.component)
        dialogBuilder.setDimensionServiceKey("mdw.AttributeSourceDialog")
        dialogBuilder.showNotModal()
    }
}