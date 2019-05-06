package com.centurylink.mdw.studio.vcs

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.components.CheckBox
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.FlowLayout
import javax.swing.*
import javax.swing.event.DocumentEvent

class CredentialsDialog(private val projectSetup: ProjectSetup) : DialogWrapper(projectSetup.project) {

    private val centerPanel = object: JPanel(BorderLayout()) {
        override fun getMinimumSize(): Dimension {
            return Dimension(260, super.getMinimumSize().width)
        }
    }

    private val okButton: JButton?
        get() = getButton(okAction)

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    private val userText = object: JTextField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(250, super.getPreferredSize().height)
        }
    }
    private val passwordText = object: JPasswordField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(250, super.getPreferredSize().height)
        }
    }
    private val saveCheckbox = CheckBox("Remember")

    override fun getPreferredFocusedComponent(): JComponent {
        return userText
    }

    internal var user = projectSetup.settings.gitUser
    internal var password = projectSetup.settings.gitPassword
    internal var saveToProject = true

    override fun doOKAction() {
        if (saveCheckbox.isSelected) {
            projectSetup.settings.gitUser = user
            projectSetup.settings.gitPassword = password
        }
        super.doOKAction()
    }

    init {
        init()
        title = "Enter Git Credentials"
        okButton?.isEnabled = false

        val formPanel = JPanel()
        formPanel.layout = BoxLayout(formPanel, BoxLayout.Y_AXIS)
        centerPanel.add(formPanel, BorderLayout.WEST)

        val userLabel = JLabel("User:")
        userLabel.alignmentX = Component.LEFT_ALIGNMENT
        userLabel.border = BorderFactory.createEmptyBorder(0, 3, 0, 0)
        formPanel.add(userLabel)
        val userPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        userPanel.alignmentX = Component.LEFT_ALIGNMENT
        userPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        formPanel.add(userPanel)
        userText.text = user
        userText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                user = e.document.getText(0, e.document.length)
                okButton?.isEnabled = !user.isBlank() && !password.isBlank()
            }
        })
        userPanel.add(userText)

        val passwordLabel = JLabel("Password:")
        passwordLabel.alignmentX = Component.LEFT_ALIGNMENT
        passwordLabel.border = BorderFactory.createEmptyBorder(0, 3, 0, 0)
        formPanel.add(passwordLabel)
        val passwordPanel = JPanel(FlowLayout(FlowLayout.LEFT, 0, 0))
        passwordPanel.alignmentX = Component.LEFT_ALIGNMENT
        passwordPanel.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        formPanel.add(passwordPanel)
        passwordText.text = password
        passwordText.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                password = e.document.getText(0, e.document.length)
                okButton?.isEnabled = !user.isBlank() && !password.isBlank()
            }
        })
        passwordPanel.add(passwordText)

        saveCheckbox.isSelected = saveToProject
        saveCheckbox.addActionListener {
            saveToProject = saveCheckbox.isSelected
        }
        formPanel.add(saveCheckbox)
    }
}