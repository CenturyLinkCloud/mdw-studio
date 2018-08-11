package com.centurylink.mdw.studio.proc

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.config.ConfigPanel
import com.centurylink.mdw.studio.config.HideShowListener
import com.centurylink.mdw.studio.config.PanelBar
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.ProcessFileType
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.AppTopics
import com.intellij.codeHighlighting.BackgroundEditorHighlighter
import com.intellij.ide.GeneralSettings
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Divider
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.JBSplitter
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.tabs.TabsUtil
import com.intellij.util.ui.JBUI
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Cursor
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel

class ProcessEditorProvider : FileEditorProvider, DumbAware {

    override fun getEditorTypeId() = "process-editor"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR

    override fun accept(project: Project, file: VirtualFile): Boolean {
        return file.fileType == ProcessFileType
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return ProcessEditor(project, file)
    }
}

class ProcessEditor(project: Project, val procFile: VirtualFile) : FileEditor, HideShowListener {

    private val projectSetup = project.getComponent(ProjectSetup::class.java)
    private var procDoc = FileDocumentManager.getInstance().getDocument(procFile)!!
    private val editPanel: JPanel
    private val canvasScrollPane: JBScrollPane
    private val splitter: JBSplitter
    private val configPanel: ConfigPanel
    private val panelBar: PanelBar
    private val canvas: ProcessCanvas
    private var _process: Process
    private var process: Process
        get() = _process
        set(value) {
            _process = value
            _process.name = procFile.nameWithoutExtension
            asset = projectSetup.getAsset(procFile)!! // asset must be found
            _process.id = asset.id
            _process.version = asset.version
            _process.packageName = asset.packageName
        }
    private lateinit var asset: Asset
    private var modified: Boolean = false
    // listeners installed by FileEditorManagerImpl
    private val propChangeListeners = mutableListOf<PropertyChangeListener>()
    private val generalSettings = GeneralSettings.getInstance()

    init {
        if (procDoc.textLength == 0) {
            procDoc.setText(Templates.get("assets/new.proc"))
        }

        // initialize backing property and then invoke setter
        _process = Process(JSONObject(procDoc.text))
        process = _process
        canvas = ProcessCanvas(projectSetup, process)
        canvasScrollPane = JBScrollPane(canvas)

        editPanel = JPanel(BorderLayout())
        configPanel = ConfigPanel(projectSetup)
        configPanel.titleBar.hideShowListener = this
        panelBar = PanelBar()

        splitter = object : JBSplitter(true, 0.75f) {

            override fun createDivider(): Divider {
                return object : DividerImpl() {
                    init {
                        layout = BorderLayout()
                        background = JBUI.CurrentTheme.ToolWindow.headerBackground(true)
                        add(configPanel.titleBar)
                    }

                    override fun setOrientation(isVerticalSplit: Boolean) {
                        cursor = if (isVertical) {
                            Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR)
                        }
                        else {
                            Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR)
                        }
                    }
                }
            }

            override fun getDividerWidth(): Int {
                return TabsUtil.getTabsHeight(JBUI.CurrentTheme.ToolWindow.tabVerticalPadding())
            }
        }

        splitter.splitterProportionKey = "processEditor.splitter"
        splitter.firstComponent = canvasScrollPane
        splitter.secondComponent = configPanel
        splitter.isShowDividerControls = false
        splitter.isShowDividerIcon = false

        canvas.selectListeners.add(configPanel)
        canvas.addUpdateListener {
            // update already reflected in process
            handleChange()
        }

        configPanel.hideShowListener = this
        panelBar.hideShowListener = this

        configPanel.addUpdateListener { obj ->
            obj.updateAsset()
            handleChange()
            canvas.revalidate()
            canvas.repaint()
        }

        val connection = ApplicationManager.getApplication().messageBus.connect(this)
        connection.subscribe(AppTopics.FILE_DOCUMENT_SYNC, object : FileDocumentManagerAdapter() {
            override fun fileContentReloaded(file: VirtualFile, document: Document) {
                if (file.equals(procFile)) {
                    procDoc = document
                    process = Process(JSONObject(procDoc.text))
                    canvas.process = process
                    canvas.revalidate()
                    canvas.repaint()
                }
            }
            override fun beforeAllDocumentsSaving() {
                // react to Save All and build events
                saveToFile()
            }
        })

        editPanel.add(splitter)
    }

    private fun handleChange() {
        updateModifiedProperty(true)
    }

    private fun saveToFile() {
        // invokeLater is used to avoid non-ui thread error on startup with multiple processes open
        ApplicationManager.getApplication().invokeLater( {
            WriteAction.run<Throwable> {
                procDoc.setText(process.json.toString(2))
                updateModifiedProperty(false)
            }
        }, ModalityState.NON_MODAL)
    }

    fun updateModifiedProperty(newValue: Boolean) {
        val wasModified = modified
        modified = newValue
        if (wasModified != modified) {
            for (propChangeListener in propChangeListeners) {
                propChangeListener.propertyChange(PropertyChangeEvent(this@ProcessEditor,
                        FileEditor.PROP_MODIFIED, wasModified, modified))
            }
        }
    }

    override fun onHideShow(show: Boolean) {
        configPanel.hideShowListener = null
        panelBar.hideShowListener = null
        if (show) {
            editPanel.remove(panelBar)
            editPanel.add(splitter)
        }
        else {
            editPanel.remove(splitter)
            editPanel.add(canvasScrollPane)
            editPanel.add(panelBar, BorderLayout.SOUTH)
        }
        editPanel.revalidate()
        editPanel.repaint()
        configPanel.hideShowListener = this
        panelBar.hideShowListener = this

        if (generalSettings.isSaveOnFrameDeactivation) {
            saveToFile()
        }
    }

    override fun getComponent() = editPanel

    /**
     * Editor name (not process name)
     */
    override fun getName(): String {
        return "Flow Designer"
    }

    override fun isModified(): Boolean {
        return modified
    }

    override fun isValid(): Boolean {
        return true
    }

    override fun setState(state: FileEditorState) {
    }

    override fun getPreferredFocusedComponent(): JComponent? {
        return null
    }

    override fun getCurrentLocation(): FileEditorLocation? {
        return null
    }

    override fun selectNotify() {
    }

    override fun deselectNotify() {
        if (generalSettings.isSaveOnFrameDeactivation) {
            saveToFile()
        }
    }

    override fun getBackgroundHighlighter(): BackgroundEditorHighlighter? {
        return null
    }

    override fun addPropertyChangeListener(listener: PropertyChangeListener) {
        propChangeListeners.add(listener)
    }

    override fun removePropertyChangeListener(listener: PropertyChangeListener) {
        propChangeListeners.remove(listener)
    }

    override fun dispose() {
    }

    override fun <T : Any?> getUserData(key: Key<T>): T? {
        return null
    }

    override fun <T : Any?> putUserData(key: Key<T>, value: T?) {
    }

}