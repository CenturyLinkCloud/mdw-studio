package com.centurylink.mdw.studio.task

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.draw.edit.*
import com.centurylink.mdw.draw.edit.apply.ObjectApplier
import com.centurylink.mdw.draw.ext.toGson
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.model.task.TaskTemplate
import com.centurylink.mdw.studio.config.ConfigPanel
import com.centurylink.mdw.studio.config.ConfigTab
import com.centurylink.mdw.studio.file.TaskFileType
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.AppTopics
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.fileEditor.impl.text.PsiAwareTextEditorImpl
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Cursor
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import java.io.IOException
import javax.swing.*

abstract class TaskEditorProvider : FileEditorProvider, DumbAware {
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
    override fun accept(project: Project, file: VirtualFile): Boolean {
        val projectSetup = project.getComponent(ProjectSetup::class.java)
        return file.fileType == TaskFileType && projectSetup.isMdwProject && projectSetup.isAssetSubdir(file.parent)
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
    override fun getEditorTypeId() = "task-editor-DEFAULT_WORKGROUPS"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return TaskEditorTab("Workgroups", project, file)
    }
}

class TaskEditorNoticesProvider : TaskEditorProvider() {
    override fun getEditorTypeId() = "task-editor-notices"
    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        val content = String(file.contentsToByteArray())
        if (content.isNotEmpty()) {
            val json = JSONObject(content)
            json.optJSONObject("attributes")?.let { attrJson ->
                val notices = attrJson.optString("Notices")
                if (notices.isNullOrBlank() || notices == "\$DefaultNotices") {
                    attrJson.put("Notices", Data.DEFAULT_TASK_NOTICES.toString())
                    WriteAction.run<Throwable> {
                        file.setBinaryContent(json.toString(2).toByteArray())
                    }
                }
            }
        }
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
            override fun isModified(): Boolean {
                return false
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
    private var workflowObj: WorkflowObj
    private var editorPane = JPanel(BorderLayout())
    private var configTab: ConfigTab
    private val propChangeListeners = mutableListOf<PropertyChangeListener>()
    private var modified: Boolean = false

    init {
        editorPane.border = BorderFactory.createEmptyBorder()
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
            Data.BASE_PKG + "/AutoFormManualTask.pagelet"
        } else {
            Data.BASE_PKG + "/CustomManualTask.pagelet"
        }
        val pageletAsset = projectSetup.getAssetFile(taskPagelet)
        pageletAsset ?: throw IOException("Missing task pagelet: " + taskPagelet)
        val defJson = JSONObject()
        defJson.put("category", "task")
        defJson.put("pagelet", String(pageletAsset.contentsToByteArray()))
        val definition = Template(defJson.toGson())
        definition.filterWidgets("General").forEach {
            if (it.name == "name" || it.name == "logicalId" || it.name == "category" || it.name == "version" ||
                    it.name == "description") {
                it.attributes["applier"] = ObjectApplier::class.qualifiedName
            }
        }

        workflowObj = WorkflowObj(projectSetup, taskTemplate, WorkflowType.task, taskTemplate.json, false)
        configTab = ConfigTab(tabName, definition, workflowObj)
        configTab.border = BorderFactory.createEmptyBorder(7, 0, 3, 0)
        configTab.addUpdateListener { obj ->
            obj.updateAsset()
            handleChange()
        }
        editorPane.add(configTab)

        // help link
        definition.pagelet.widgets.find {
            it.isHelpLink && (it.section == tabName || (it.section == null && tabName == "General"))
        }?.let {
            addHelpLink(it)
        }

        val connection = ApplicationManager.getApplication().messageBus.connect(this)
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, object : FileDocumentManagerListener {
            override fun fileContentReloaded(file: VirtualFile, document: Document) {
                if (file.equals(taskFile)) {
                    syncFromDoc(document)
                }
            }
            override fun beforeAllDocumentsSaving() {
                // react to Save All and build events
                saveToFile()
            }
        })

    }

    private fun addHelpLink(widget: Pagelet.Widget) {
        val helpLabel = JLabel(widget.label, ConfigPanel.ICON_HELP, SwingConstants.LEADING)
        helpLabel.border = BorderFactory.createEmptyBorder(0, 0, 1, 0)
        helpLabel.addMouseListener(object : MouseAdapter() {
            override fun mouseEntered(e: MouseEvent?) {
                helpLabel.cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)
                helpLabel.border = BorderFactory.createMatteBorder(0, 0, 1, 0,
                        UIManager.getColor("EditorPane.foreground"))
            }
            override fun mouseExited(e: MouseEvent?) {
                helpLabel.cursor = Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR)
                helpLabel.border = BorderFactory.createEmptyBorder(0, 0, 1, 0)
            }
            override fun mouseReleased(e: MouseEvent) {
                BrowserUtil.browse(if (widget.url?.startsWith("help/http") == true) {
                    // full-fledged url for help link
                    widget.url?.substring(5) ?: Data.DOCS_URL
                }
                else {
                    // relative to help docs
                    Data.DOCS_URL + "/" + widget.url
                })
            }
        })
        val helpPane = JPanel(BorderLayout())
        helpPane.add(helpLabel, BorderLayout.EAST)
        helpPane.border = BorderFactory.createEmptyBorder(1, 1, 5, 10)
        helpPane.background = UIManager.getColor("EditorPane.background")
        editorPane.add(helpPane, BorderLayout.SOUTH)
    }

    private fun syncFromDoc(document: Document) {
        taskDoc = document
        val json = JSONObject(taskDoc.text)
        taskTemplate = TaskTemplate(json)
        workflowObj.asset = taskTemplate
        workflowObj.obj = json
    }

    private fun handleChange() {
        updateModifiedProperty(true)
    }

    private fun saveToFile() {
         ApplicationManager.getApplication().invokeAndWait( {
             WriteAction.run<Throwable> {
                 if (modified) {
                     taskDoc.setText(taskTemplate.json.toString(2))
                     updateModifiedProperty(false)
                 }
             }
         }, ModalityState.NON_MODAL)
     }

    fun updateModifiedProperty(newValue: Boolean) {
         val wasModified = modified
         modified = newValue
         if (wasModified != modified) {
             for (propChangeListener in propChangeListeners) {
                 propChangeListener.propertyChange(PropertyChangeEvent(this@TaskEditorTab,
                         FileEditor.PROP_MODIFIED, wasModified, modified))
             }
         }
    }

    override fun getName(): String {
        return tabName
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun isModified(): Boolean {
        return modified
    }

    override fun setState(state: FileEditorState) {
    }

    override fun getComponent(): JComponent {
        return editorPane
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun selectNotify() {
        taskFile.refresh(true, false) {
            syncFromDoc(FileDocumentManager.getInstance().getDocument(taskFile)!!)
        }
    }

    override fun deselectNotify() {
        saveToFile()
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propChangeListeners.add(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propChangeListeners.remove(listener)
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