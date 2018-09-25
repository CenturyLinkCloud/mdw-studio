package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.CheckBox
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import java.io.File
import javax.swing.*
import javax.swing.event.DocumentEvent

class NewSwaggerApi : AssetAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        val projectSetup = locator.getProjectSetup()
        if (projectSetup != null) {
            locator.getPackage()?.let { pkg ->
                val codegenDialog = CodegenDialog(projectSetup)
                if (codegenDialog.showAndGet()) {
                    println("INPUT: " + codegenDialog.inputSpec )
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val applicable = Locator(event).getPackage() != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }
}

class CodegenDialog(projectSetup: ProjectSetup) : DialogWrapper(projectSetup.project, true) {

    private val centerPanel = JPanel(BorderLayout())
    private val okButton: JButton?
        get() = getButton(okAction)
    private val inputSpecText = object: JTextField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(400, super.getPreferredSize().height)
        }
    }

    var inputSpec = ""
    var codegenConfig: File? = null
    var genApis = true
    var genModels = true
    var genWorkflows = false
    var genDocs = false

    init {
        init()
        title = "Swagger Service"
        okButton?.isEnabled = false

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        centerPanel.add(panel, BorderLayout.WEST)

        val inputLabel = JLabel("Input Spec URL:")
        inputLabel.alignmentX = Component.LEFT_ALIGNMENT
        inputLabel.border = BorderFactory.createEmptyBorder(0, 3, 0, 0)
        panel.add(inputLabel)
        val inputPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        inputPanel.alignmentX = Component.LEFT_ALIGNMENT
        inputPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        panel.add(inputPanel)
        inputSpecText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                inputSpec = e.document.getText(0, e.document.length)
                okButton?.isEnabled = !inputSpec.isBlank()
            }
        })
        inputPanel.add(inputSpecText)
        val inputSpecButton = JButton("File...")
        inputSpecButton.addActionListener { _ ->
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                    .withRoots(projectSetup.assetDir)
                    .withFileFilter { it.extension == "json" }
            FileChooser.chooseFile(descriptor, projectSetup.project, null) { file ->
                inputSpecText.text = file.path
            }
        }
        inputPanel.add(inputSpecButton)

        val configLabel = JLabel("Codegen Config (optional):")
        configLabel.alignmentX = Component.LEFT_ALIGNMENT
        configLabel.border = BorderFactory.createEmptyBorder(0, 3, 0, 0)
        panel.add(configLabel)
        val configPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        configPanel.alignmentX = Component.LEFT_ALIGNMENT
        configPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        panel.add(configPanel)
        val configFileText = object: JTextField() {
            override fun getPreferredSize(): Dimension {
                return Dimension(400, super.getPreferredSize().height)
            }
        }
        configFileText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                codegenConfig = File(e.document.getText(0, e.document.length))
            }
        })
        configPanel.add(configFileText)
        val configFileButton = JButton("File...")
        configFileButton.addActionListener { _ ->
            val descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor()
                    .withRoots(projectSetup.assetDir)
                    .withFileFilter { it.extension == "json" }
            FileChooser.chooseFile(descriptor, projectSetup.project, null) { file ->
                codegenConfig = File(file.path)
            }
        }
        configPanel.add(configFileButton)

        val genPanel = object: JPanel(BorderLayout()) {
            override fun getPreferredSize(): Dimension {
                return Dimension(400, super.getPreferredSize().height)
            }
        }
        panel.add(genPanel)
        genPanel.alignmentX = Component.LEFT_ALIGNMENT
        genPanel.border = BorderFactory.createTitledBorder("Generate")

        val cbPanel = JPanel()
        genPanel.add(cbPanel, BorderLayout.WEST)
        val cbBorder = BorderFactory.createEmptyBorder(0, 0, 0, 5)

        val apisCheckbox = CheckBox("APIs")
        apisCheckbox.border = cbBorder
        apisCheckbox.isSelected = genModels
        apisCheckbox.addActionListener {
            genApis = apisCheckbox.isSelected
        }
        cbPanel.add(apisCheckbox)

        val modelsCheckbox = CheckBox("Models")
        modelsCheckbox.border = cbBorder
        modelsCheckbox.isSelected = genModels
        modelsCheckbox.addActionListener {
            genModels = modelsCheckbox.isSelected
        }
        cbPanel.add(modelsCheckbox)

        val workflowsCheckbox = CheckBox("Workflows")
        workflowsCheckbox.border = cbBorder
        workflowsCheckbox.isSelected = genWorkflows
        workflowsCheckbox.addActionListener {
            genWorkflows = workflowsCheckbox.isSelected
        }
        cbPanel.add(workflowsCheckbox)

        val docsCheckbox = CheckBox("Documentation")
        docsCheckbox.isSelected = genWorkflows
        docsCheckbox.addActionListener {
            genDocs = docsCheckbox.isSelected
        }
        cbPanel.add(docsCheckbox)
    }

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return inputSpecText
    }
}