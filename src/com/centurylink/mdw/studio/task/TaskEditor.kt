package com.centurylink.mdw.studio.task

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.model.task.TaskTemplate
import com.centurylink.mdw.studio.config.ConfigTab
import com.centurylink.mdw.studio.edit.Template
import com.centurylink.mdw.studio.edit.WorkflowObj
import com.centurylink.mdw.studio.edit.WorkflowType
import com.centurylink.mdw.studio.ext.toGson
import com.centurylink.mdw.studio.file.TaskFileType
import com.centurylink.mdw.studio.proj.Implementors
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.json.JSONObject
import java.beans.PropertyChangeListener
import java.io.IOException
import javax.swing.BorderFactory
import javax.swing.JComponent

abstract class TaskEditorProvider : FileEditorProvider, DumbAware {
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
    override fun accept(project: Project, file: VirtualFile): Boolean {
        val projectSetup = project.getComponent(ProjectSetup::class.java)
        return file.fileType == TaskFileType && projectSetup.isAssetSubdir(file.parent)
    }
    protected fun isAutoform(file: VirtualFile): Boolean {
        val content = String(file.contentsToByteArray())
        if (content.isEmpty()) {
            return true
        }
        val attrJson = JSONObject(content).optJSONObject("attributes")
        return attrJson != null && "Autoform" == attrJson.optString("FormName")
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

class TaskEditorNoticesProvider : TaskEditorProvider() {
    override fun getEditorTypeId() = "task-editor-notices"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return TaskEditorTab("Notices", project, file)
    }
}

class TaskEditorRecipientsProvider : TaskEditorProvider() {
    override fun getEditorTypeId() = "task-editor-recipients"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return TaskEditorTab("Recipients", project, file)
    }
}

class TaskEditorIndexesProvider : TaskEditorProvider() {
    override fun getEditorTypeId() = "task-editor-indexes"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return TaskEditorTab("Indexes", project, file)
    }
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return super.accept(project, file) && !isAutoform(file)
    }
}

class TaskEditorVariablesProvider : TaskEditorProvider() {
    override fun getEditorTypeId() = "task-editor-variables"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return TaskEditorTab("Variables", project, file)
    }
    override fun accept(project: Project, file: VirtualFile): Boolean {
        return super.accept(project, file) && isAutoform(file)
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
            override fun setState(state: FileEditorState, exactState: Boolean) {
            }
        }
    }
}

class TaskEditorTab(private val tabName: String, project: Project, val taskFile: VirtualFile) : FileEditor {

    @Suppress("unused")
    constructor(project: Project, taskFile: VirtualFile): this("", project, taskFile)

    private val projectSetup = project.getComponent(ProjectSetup::class.java)
    private var taskDoc = FileDocumentManager.getInstance().getDocument(taskFile)!!
    private var taskTemplate: TaskTemplate
    private val configTab: ConfigTab


    init {
        if (taskDoc.textLength == 0) {
            // load from template
            val content = Templates.get("assets/autoform.task")
            val taskJson = JSONObject(content)
            // populate name and logical id (TODO: remove name property from task json)
            val name = taskFile.name.substring(0, taskFile.name.length - ".task".length)
            taskJson.put("name", name)
            taskJson.put("logicalId", name)
            taskJson.put("version", "0")
            taskDoc.setText(taskJson.toString(2))
        }
        taskTemplate = TaskTemplate(JSONObject(taskDoc.text))
        val taskPagelet = if (taskTemplate.isAutoformTask) {
            Implementors.BASE_PKG + "/AutoFormManualTask.pagelet"
        } else {
            Implementors.BASE_PKG + "/CustomManualTask.pagelet"
        }
        val pageletAsset = projectSetup.getAssetFile(taskPagelet)
        pageletAsset ?: throw IOException("Missing task pagelet: " + taskPagelet)
        val defJson = JSONObject()
        defJson.put("category", "task")
        defJson.put("pagelet", String(pageletAsset.contentsToByteArray()))
        val definition = Template(defJson.toGson())
        val workflowObj = WorkflowObj(projectSetup, taskTemplate, WorkflowType.task, taskTemplate.json)
        configTab = ConfigTab(tabName, definition, workflowObj)
        configTab.addUpdateListener { obj ->
            obj.updateAsset()
        }
        configTab.border = BorderFactory.createEmptyBorder(7, 0, 3, 0)
    }

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
        return configTab
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