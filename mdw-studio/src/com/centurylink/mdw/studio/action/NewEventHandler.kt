package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.event.ExternalEvent
import com.centurylink.mdw.studio.file.EventHandlerFileType
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.psi.*
import com.intellij.ui.DocumentAdapter
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.DocumentEvent

class NewEventHandler : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        val projectSetup: ProjectSetup? = locator.projectSetup
        if (projectSetup != null) {
            var pkg = locator.selectedPackage
            if (pkg == null) {
                locator.potentialPackageDir?.let { pkgDir ->
                    pkg = projectSetup.createPackage(pkgDir)
                }
            }
            pkg?.let { eventHandlerPkg ->
                val eventHandlerDialog = EventHandlerDialog(projectSetup)
                if (eventHandlerDialog.showAndGet() ) {
                    val externalEvent = ExternalEvent()
                    var handler = ""
                    externalEvent.eventName = eventHandlerDialog.eventHandlerName
                    externalEvent.messagePattern = eventHandlerDialog.msgPattern
                    if (eventHandlerDialog.handlerType == "custom") {
                        val javaPackage= eventHandlerDialog.pkgName
                        handler = javaPackage + "." + eventHandlerDialog.className
                    }
                    else {
                        if (eventHandlerDialog.handlerActionType == "launchProcess")
                            handler = "START_PROCESS?ProcessName=" + eventHandlerDialog.processName
                        else if (eventHandlerDialog.handlerActionType == "notifyProcess")
                            handler = "NOTIFY_PROCESS?EventName=" + eventHandlerDialog.eventName
                    }
                    externalEvent.eventHandler = handler
                    externalEvent.packageName = eventHandlerPkg.name
                    val dir = PsiManager.getInstance(projectSetup.project).findDirectory(eventHandlerPkg.dir)
                    if (dir != null)
                        createAndOpen(dir, eventHandlerDialog.eventHandlerName + ".evth", externalEvent.json.toString(2))
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val applicable = Locator(event).potentialPackageDir != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    private fun createAndOpen(dir: PsiDirectory, fileName: String, content: String): PsiFile? {
        val project = dir.project
        var psiFile = PsiFileFactory.getInstance(dir.project).createFileFromText(fileName, EventHandlerFileType, content)
        WriteAction.run<Exception> {
            psiFile = dir.add(psiFile) as PsiFile
        }
        val virtualFile = psiFile.virtualFile
        return if (virtualFile != null) {
            FileEditorManager.getInstance(project).openFile(virtualFile, true)
            val pointer = SmartPointerManager.getInstance(project).createSmartPsiElementPointer(psiFile)
            pointer.element
        }
        else {
            null
        }
    }

    companion object {
        val LOG = Logger.getInstance(NewEventHandler::class.java)
    }
}

class EventHandlerDialog(projectSetup: ProjectSetup) : DialogWrapper(projectSetup.project, true) {
    private val centerPanel = JPanel(BorderLayout())
    private val okButton: JButton?
        get() = getButton(okAction)
    private val eventNameText = object: JTextField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(400, super.getPreferredSize().height)
        }
    }

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return eventNameText
    }

    private val builtinHandlerPanel = JPanel()
    private val customHandlerPanel = JPanel()

    private val pkgNameLabel = JLabel("Package Name:")
    private val pkgNameText = object: JTextField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(400, super.getPreferredSize().height)
        }
    }
    private val classNameLabel = JLabel("Class Name:")
    private val classNameText = object: JTextField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(400, super.getPreferredSize().height)
        }
    }
    private val launchProcess = JRadioButton("Process Launch:")
    private val eventNotify = JRadioButton("Event Notify:")
    private val eventNotifyText = object: JTextField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(400, super.getPreferredSize().height)
        }
    }

    private val combo = object : JComboBox<String>(emptyArray<String>()) {
        override fun getPreferredSize(): Dimension {
            val size = super.getPreferredSize()
            return Dimension(400, size.height)
        }
    }

    var eventHandlerName = ""
    var msgPattern: String = ""
    var pkgName = ""
    var className = ""
    var processName = ""
    var eventName= ""
    var handlerType = ""
    var handlerActionType = ""

    init {
        init()
        title = "Event Handler"
        okButton?.isEnabled = false

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        centerPanel.add(panel, BorderLayout.WEST)

        val eventNamePanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        eventNamePanel.alignmentX = Component.LEFT_ALIGNMENT
        eventNamePanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        panel.add(eventNamePanel)
        val eventNameLabel = JLabel("Event Handler Name:")
        eventNameLabel.alignmentX = Component.LEFT_ALIGNMENT
        eventNameLabel.border = BorderFactory.createEmptyBorder(0, 3, 0, 3)
        eventNamePanel.add(eventNameLabel)
        eventNameText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                eventHandlerName = e.document.getText(0, e.document.length)
                okButton?.isEnabled = isInputValid()
            }
        })
        eventNamePanel.add(eventNameText)
        panel.add(eventNamePanel)

        val msgPatternPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        msgPatternPanel.alignmentX = Component.LEFT_ALIGNMENT
        msgPatternPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        panel.add(msgPatternPanel)
        val msgPatternLabel = JLabel("Message Pattern:")
        msgPatternLabel.alignmentX = Component.LEFT_ALIGNMENT
        msgPatternLabel.border = BorderFactory.createEmptyBorder(0, 3, 0, 25)
        msgPatternPanel.add(msgPatternLabel)

        val msgPatternText = object: JTextField() {
            override fun getPreferredSize(): Dimension {
                return Dimension(400, super.getPreferredSize().height)
            }
        }
        msgPatternText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                msgPattern = e.document.getText(0, e.document.length)
                okButton?.isEnabled = isInputValid()
            }
        })
        msgPatternPanel.add(msgPatternText)

        val handlerTypePanel = object: JPanel(BorderLayout()) {
            override fun getPreferredSize(): Dimension {
                return Dimension(600, super.getPreferredSize().height)
            }
        }
        handlerTypePanel.layout = BoxLayout(handlerTypePanel, BoxLayout.Y_AXIS)

        panel.add(handlerTypePanel)
        handlerTypePanel.alignmentX = Component.LEFT_ALIGNMENT
        handlerTypePanel.border = BorderFactory.createTitledBorder("Handler Type")

        val rbPanel = JPanel()
        rbPanel.layout = BoxLayout(rbPanel, BoxLayout.Y_AXIS)

        handlerTypePanel.add(rbPanel, BorderLayout.WEST)

        builtinHandlerPanel.layout = BoxLayout(builtinHandlerPanel, BoxLayout.Y_AXIS)
        rbPanel.add(builtinHandlerPanel, BorderLayout.WEST)
        val rbBorder = BorderFactory.createEmptyBorder(3, 5, 3, 5)

        val handlerGroup = ButtonGroup()
        val builtInHandlerButton = JRadioButton("Built-in Handler")
        builtInHandlerButton.isOpaque = true
        builtInHandlerButton.border = rbBorder
        builtInHandlerButton.actionCommand = "built-in"
        builtInHandlerButton.addActionListener {
            pkgNameLabel.isEnabled = false
            pkgNameText.isEnabled = false
            classNameLabel.isEnabled = false
            classNameText.isEnabled = false
            launchProcess.isEnabled = true
            combo.isEnabled = true
            eventNotify.isEnabled = true
            eventNotifyText.isEnabled = true
            handlerType = builtInHandlerButton.actionCommand
            okButton?.isEnabled = isInputValid()
        }
        handlerGroup.add(builtInHandlerButton)
        builtinHandlerPanel.add(builtInHandlerButton)

        val actionPanel = JPanel()
        actionPanel.layout = BoxLayout(actionPanel, BoxLayout.Y_AXIS)
        actionPanel.border = BorderFactory.createEmptyBorder(0, 30, 0, 5)
        builtinHandlerPanel.add(actionPanel)

        val handlerActionPanel = object: JPanel(BorderLayout()) {
            override fun getPreferredSize(): Dimension {
                return Dimension(600, super.getPreferredSize().height)
            }
        }
        handlerActionPanel.layout = BoxLayout(handlerActionPanel, BoxLayout.Y_AXIS)
        actionPanel.add(handlerActionPanel)
        handlerActionPanel.alignmentX = Component.LEFT_ALIGNMENT
        handlerActionPanel.border = BorderFactory.createTitledBorder("Handler Action")

        val launchProcessPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        launchProcessPanel.alignmentX = Component.LEFT_ALIGNMENT
        launchProcessPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        handlerActionPanel.add(launchProcessPanel)

        val actionGroup = ButtonGroup()
        launchProcess.isOpaque = false
        launchProcess.border = rbBorder
        launchProcess.actionCommand = "launchProcess"
        launchProcess.addActionListener {
            handlerActionType = launchProcess.actionCommand
            okButton?.isEnabled = isInputValid()
        }
        actionGroup.add(launchProcess)
        launchProcessPanel.add(launchProcess)

        val assets = projectSetup.findAssetsOfType("proc")
        assets.let {
            combo.addItem("")
            assets.forEach { asset ->
                combo.addItem(asset.pkg.name + "/" + asset.name)
            }
        }

        combo.isOpaque = false
        combo.addItemListener { ie ->
            processName = ie.item.toString()
            okButton?.isEnabled = isInputValid()
        }
        launchProcessPanel.add(combo)

        val eventNotifyPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        eventNotifyPanel.alignmentX = Component.LEFT_ALIGNMENT
        eventNotifyPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        handlerActionPanel.add(eventNotifyPanel)

        eventNotify.isOpaque = false
        eventNotify.border = BorderFactory.createEmptyBorder(0, 5, 5, 22)
        eventNotify.actionCommand = "notifyProcess"
        eventNotify.addActionListener {
            handlerActionType = eventNotify.actionCommand
            okButton?.isEnabled = isInputValid()
        }
        actionGroup.add(eventNotify)
        eventNotifyPanel.add(eventNotify)

        eventNotifyText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                eventName = e.document.getText(0, e.document.length)
                okButton?.isEnabled = isInputValid()
            }
        })
        eventNotifyText.isOpaque = false
        eventNotifyPanel.add(eventNotifyText)

        customHandlerPanel.layout = BoxLayout(customHandlerPanel, BoxLayout.Y_AXIS)

        rbPanel.add(customHandlerPanel, BorderLayout.WEST)

        val customHandlerButton = JRadioButton("Custom Handler")
        customHandlerButton.isOpaque = false
        customHandlerButton.border = rbBorder
        customHandlerButton.actionCommand = "custom"
        customHandlerButton.addActionListener {
            launchProcess.isEnabled = false
            combo.isEnabled = false
            eventNotify.isEnabled = false
            eventNotifyText.isEnabled = false
            pkgNameLabel.isEnabled = true
            pkgNameText.isEnabled = true
            classNameLabel.isEnabled = true
            classNameText.isEnabled = true
            handlerType = customHandlerButton.actionCommand
            okButton?.isEnabled = isInputValid()
        }
        handlerGroup.add(customHandlerButton)
        customHandlerPanel.add(customHandlerButton)

        val codeGenPanel = JPanel()
        codeGenPanel.layout = BoxLayout(codeGenPanel, BoxLayout.Y_AXIS)
        codeGenPanel.border = BorderFactory.createEmptyBorder(0, 30, 0, 5)
        customHandlerPanel.add(codeGenPanel)

        val codeGenerationPanel = object: JPanel(BorderLayout()) {
            override fun getPreferredSize(): Dimension {
                return Dimension(600, super.getPreferredSize().height)
            }
        }
        codeGenPanel.add(codeGenerationPanel)
        codeGenerationPanel.alignmentX = Component.LEFT_ALIGNMENT
        codeGenerationPanel.border = BorderFactory.createTitledBorder("Code Registration")
        codeGenerationPanel.layout = BoxLayout(codeGenerationPanel, BoxLayout.Y_AXIS)

        val customPkgPanel = JPanel()
        customPkgPanel.layout = BoxLayout(customPkgPanel, BoxLayout.X_AXIS)
        customPkgPanel.border = rbBorder
        codeGenerationPanel.add(customPkgPanel)

        pkgNameLabel.border = BorderFactory.createEmptyBorder(0, 5, 5, 5)
        pkgNameLabel.alignmentX = Component.LEFT_ALIGNMENT
        pkgNameLabel.isOpaque = false
        customPkgPanel.add(pkgNameLabel)


        pkgNameText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                pkgName = e.document.getText(0, e.document.length)
                okButton?.isEnabled = isInputValid()
            }
        })
        pkgNameText.isOpaque = false
        customPkgPanel.add(pkgNameText)

        val customClassPanel = JPanel()
        customClassPanel.layout = BoxLayout(customClassPanel, BoxLayout.X_AXIS)
        customClassPanel.border = rbBorder
        codeGenerationPanel.add(customClassPanel)

        classNameLabel.border = BorderFactory.createEmptyBorder(0, 5, 5, 22)
        classNameLabel.alignmentX = Component.LEFT_ALIGNMENT
        classNameLabel.isOpaque = false
        customClassPanel.add(classNameLabel)

        classNameText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                className = e.document.getText(0, e.document.length)
                okButton?.isEnabled = isInputValid()            }
        })
        classNameText.isOpaque = false
        customClassPanel.add(classNameText)
    }

    fun isInputValid() : Boolean {
        return eventHandlerName.isNotBlank() && msgPattern.isNotBlank()
                && ((handlerType == "built-in" && (handlerActionType == "launchProcess" && processName.isNotBlank() && processName.isNotEmpty()
                || ( handlerActionType == "notifyProcess" && eventName.isNotBlank() && eventName.isNotEmpty())))
                || (handlerType == "custom" &&  pkgName.isNotBlank() && pkgName.isNotEmpty() && className.isNotBlank()
                && className.isNotEmpty()))
    }
}