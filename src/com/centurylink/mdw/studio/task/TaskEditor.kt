package com.centurylink.mdw.studio.task

import com.centurylink.mdw.studio.config.HideShowListener
import com.centurylink.mdw.studio.file.TaskFileType
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import java.beans.PropertyChangeListener
import javax.swing.JComponent

class TaskEditorGeneralProvider : FileEditorProvider, DumbAware {

    override fun getEditorTypeId() = "task-editor"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == TaskFileType
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return return TaskEditorTab("General", project, file)
    }
}

class TaskEditorTab(project: Project, val taskFile: VirtualFile) : FileEditor {

    constructor(name: String, project: Project, taskFile: VirtualFile): this(project, taskFile)

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isModified(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getName(): String {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun setState(state: FileEditorState) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getComponent(): JComponent {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun selectNotify() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun deselectNotify() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isValid(): Boolean {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun dispose() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}