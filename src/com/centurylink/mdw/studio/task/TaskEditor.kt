package com.centurylink.mdw.studio.task

import com.centurylink.mdw.studio.file.TaskFileType
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.io.IOException
import javax.swing.JComponent
import javax.swing.JPanel

abstract class TaskEditorProvider : FileEditorProvider, DumbAware {
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == TaskFileType
    }
}

class TaskEditorGeneralProvider : TaskEditorProvider() {
    override fun getEditorTypeId() = "task-editor-general"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return TaskEditorTab("General", project, file)
    }
}

class TaskEditorWorkgroupsProvider : TaskEditorProvider() {
    override fun getEditorTypeId() = "task-editor-workgroups"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return TaskEditorTab("Workgroups", project, file)
    }
}

class TaskEditorSourceProvider : TaskEditorProvider() {
    override fun getEditorTypeId() = "task-editor-source"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val provider = FileEditorProviderManager.getInstance().getProvider("text-editor")
        provider ?: throw IOException("Cannot create text editor: " + file)
        return object : PsiAwareTextEditorImpl(project, file, provider as TextEditorProvider) {
            override fun getName(): String {
                return "Source"
            }
        }
    }
}

class TaskEditorTab(private val tabName: String, project: Project, val taskFile: VirtualFile) : FileEditor {

    @Suppress("unused")
    constructor(project: Project, taskFile: VirtualFile): this("", project, taskFile)

    override fun getName(): String {
        return tabName
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun setState(state: FileEditorState) {
    }

    override fun getComponent(): JComponent {
        return JPanel(BorderLayout())
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun selectNotify() {
    }

    override fun deselectNotify() {
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        return null
    }
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null
    }

    override fun dispose() {
    }
}