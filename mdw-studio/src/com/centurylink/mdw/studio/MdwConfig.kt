package com.centurylink.mdw.studio

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.ui.components.CheckBox
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class MdwConfig : SearchableConfigurable {

    var modified = false

    private val settingsPanel = JPanel(BorderLayout())
    private val syncDynamicJavaCheckbox = CheckBox("Sync dynamic Java class name on edit")

    init {
        val panel = JPanel()
        settingsPanel.add(panel, BorderLayout.WEST)

        syncDynamicJavaCheckbox.isSelected = MdwSettings.instance.isSyncDynamicJavaClassName
        syncDynamicJavaCheckbox.addActionListener {
            modified = true
        }
        panel.add(syncDynamicJavaCheckbox)
    }

    override fun getId(): String {
        return "com.centurylink.mdw.studio"
    }

    override fun getDisplayName(): String {
        return "MDW Options"
    }

    override fun createComponent(): JComponent {
        return settingsPanel
    }
    override fun isModified(): Boolean {
        return modified
    }

    override fun apply() {
        val mdwSettings = MdwSettings.instance
        mdwSettings.isSyncDynamicJavaClassName = syncDynamicJavaCheckbox.isSelected
    }
}