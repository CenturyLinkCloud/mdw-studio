package com.centurylink.mdw.studio

import com.centurylink.mdw.studio.file.Icons
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.fileChooser.PathChooserDialog
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.IdeBorderFactory
import com.intellij.ui.JBColor
import com.intellij.ui.JBIntSpinner
import com.intellij.ui.components.CheckBox
import com.intellij.ui.components.JBList
import com.intellij.util.ui.UIUtil
import java.awt.*
import java.net.MalformedURLException
import java.net.URL
import javax.swing.*
import javax.swing.event.DocumentEvent

class MdwConfig : SearchableConfigurable {

    var modified = false

    private val settingsPanel = JPanel(BorderLayout())
    private val mdwHomeText = object: TextFieldWithBrowseButton() {
        override fun getPreferredSize(): Dimension {
            return Dimension(500, super.getPreferredSize().height)
        }
    }

    private val serverPollingCheckbox = CheckBox("Poll to detect running server (requires restart)")

    private val gridLinesCheckbox = CheckBox("Show grid lines when editable")
    private val zoomSlider = object : JSlider(20, 200, 100) {
        override fun getPreferredSize(): Dimension {
            return Dimension(500, super.getPreferredSize().height)
        }
    }

    private val syncDynamicJavaCheckbox = CheckBox("Sync dynamic Java class name")
    private val createAndAssociateTaskCheckbox = CheckBox("Create and associate task template")

    private val vercheckAutofixCheckbox = CheckBox("Autofix asset version conflicts")

    private val discoveryRepoUrlsList = object: JBList<String>() {
        override fun getPreferredSize(): Dimension {
            val height = super.getPreferredSize().height
            return Dimension(460, if (height < 60) 60 else height)
        }
    }
    private val discoveryRepoUrls: MutableList<String> = MdwSettings.instance.discoveryRepoUrls.toMutableList()
    private val maxBranchesTagsSpinner = JBIntSpinner(MdwSettings.instance.discoveryMaxBranchesTags, 1, 100)

    init {
        settingsPanel.layout = GridBagLayout()

        val gridConstraints = GridBagConstraints()
        gridConstraints.anchor = GridBagConstraints.NORTH
        gridConstraints.gridx = 0
        gridConstraints.gridy = 0
        gridConstraints.fill = GridBagConstraints.HORIZONTAL
        gridConstraints.weightx = 1.0
        gridConstraints.weighty = 1.0

        // environment
        val envPanel = JPanel()
        envPanel.layout = BoxLayout(envPanel, BoxLayout.Y_AXIS)
        envPanel.border = IdeBorderFactory.createTitledBorder("Environment")
        settingsPanel.add(envPanel, gridConstraints)

        // mdw home
        val mdwHomePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        mdwHomePanel.alignmentX = Component.LEFT_ALIGNMENT
        envPanel.add(mdwHomePanel)
        val mdwHomeLabel = JLabel("MDW Home:")
        mdwHomePanel.add(mdwHomeLabel)
        mdwHomeText.text = MdwSettings.instance.mdwHome
        val descriptor = FileChooserDescriptor(false, true, false, false, false, false)
        descriptor.putUserData(PathChooserDialog.PREFER_LAST_OVER_EXPLICIT, false)
        mdwHomeText.addBrowseFolderListener(null, null, null, descriptor)
        mdwHomeText.textField.document.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                modified = true
            }
        })
        mdwHomePanel.add(mdwHomeText)
        val mdwHomeHelp = JLabel("MDW CLI installation directory (requires restart)")
        mdwHomeHelp.alignmentX = Component.LEFT_ALIGNMENT
        mdwHomeHelp.foreground = Color.GRAY
        mdwHomeHelp.border = BorderFactory.createEmptyBorder(0, 90, 0, 0)
        envPanel.add(mdwHomeHelp)

        // server polling
        serverPollingCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        serverPollingCheckbox.border = BorderFactory.createEmptyBorder(10, 0, 5, 0)
        serverPollingCheckbox.isSelected = !MdwSettings.instance.isSuppressServerPolling
        serverPollingCheckbox.addActionListener {
            modified = true
        }
        envPanel.add(serverPollingCheckbox)

        // canvas
        gridConstraints.gridy = 1
        val canvasPanel = JPanel()
        canvasPanel.layout = BoxLayout(canvasPanel, BoxLayout.Y_AXIS)
        canvasPanel.border = IdeBorderFactory.createTitledBorder("Canvas")
        settingsPanel.add(canvasPanel, gridConstraints)

        // grid lines
        gridLinesCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        gridLinesCheckbox.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        gridLinesCheckbox.isSelected = !MdwSettings.instance.isHideCanvasGridLines
        gridLinesCheckbox.addActionListener {
            modified = true
        }
        canvasPanel.add(gridLinesCheckbox)

        // canvas zoom
        val zoomPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        zoomPanel.alignmentX = Component.LEFT_ALIGNMENT
        canvasPanel.add(zoomPanel)
        val zoomLabel = JLabel("Canvas Zoom:")
        zoomPanel.add(zoomLabel)

        zoomSlider.alignmentX = Component.LEFT_ALIGNMENT
        zoomSlider.value = MdwSettings.instance.canvasZoom
        zoomSlider.minorTickSpacing = 10
        zoomSlider.majorTickSpacing = 20
        zoomSlider.paintTicks = true
        zoomSlider.paintLabels = true
        zoomSlider.addChangeListener {
            modified = true
        }
        zoomPanel.add(zoomSlider)

        // editing
        gridConstraints.gridy = 2
        val editPanel = JPanel()
        editPanel.layout = BoxLayout(editPanel, BoxLayout.Y_AXIS)
        editPanel.border = IdeBorderFactory.createTitledBorder("Editing")
        settingsPanel.add(editPanel, gridConstraints)

        // sync dynamic java classname
        syncDynamicJavaCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        syncDynamicJavaCheckbox.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        syncDynamicJavaCheckbox.isSelected = MdwSettings.instance.isSyncDynamicJavaClassName
        syncDynamicJavaCheckbox.addActionListener {
            modified = true
        }
        editPanel.add(syncDynamicJavaCheckbox)

        // create and associate task template
        createAndAssociateTaskCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        createAndAssociateTaskCheckbox.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        createAndAssociateTaskCheckbox.isSelected = MdwSettings.instance.isCreateAndAssociateTaskTemplate
        createAndAssociateTaskCheckbox.addActionListener {
            modified = true
        }
        editPanel.add(createAndAssociateTaskCheckbox)

        // assets
        gridConstraints.gridy = 3
        val assetsPanel = JPanel()
        assetsPanel.layout = BoxLayout(assetsPanel, BoxLayout.Y_AXIS)
        assetsPanel.border = IdeBorderFactory.createTitledBorder("Assets")
        settingsPanel.add(assetsPanel, gridConstraints)

        // vercheck autofix
        vercheckAutofixCheckbox.alignmentX = Component.LEFT_ALIGNMENT
        vercheckAutofixCheckbox.border = BorderFactory.createEmptyBorder(0, 0, 5, 0)
        vercheckAutofixCheckbox.isSelected = MdwSettings.instance.isAssetVercheckAutofix
        vercheckAutofixCheckbox.addActionListener {
            modified = true
        }
        assetsPanel.add(vercheckAutofixCheckbox)

        // discovery
        gridConstraints.gridy = 4
        val discoveryPanel = JPanel()
        discoveryPanel.layout = BoxLayout(discoveryPanel, BoxLayout.Y_AXIS)
        discoveryPanel.border = IdeBorderFactory.createTitledBorder("Discovery")
        settingsPanel.add(discoveryPanel, gridConstraints)

        // repos panel
        val reposLayout = FlowLayout(FlowLayout.LEFT, 7, 5)
        reposLayout.alignOnBaseline = true
        val reposPanel = JPanel(reposLayout)
        reposPanel.alignmentX = Component.LEFT_ALIGNMENT
        discoveryPanel.add(reposPanel)
        val reposLabel = JLabel("Repositories:")
        reposPanel.add(reposLabel)

        discoveryRepoUrlsList.alignmentX = Component.LEFT_ALIGNMENT
        val borderColor = if (UIUtil.isUnderDarcula()) Color.GRAY else JBColor.border()
        discoveryRepoUrlsList.border = BorderFactory.createLineBorder(borderColor)
        discoveryRepoUrlsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        discoveryRepoUrlsList.setListData(discoveryRepoUrls.toTypedArray())
        reposPanel.add(discoveryRepoUrlsList)

        val buttonPanel = JPanel()
        buttonPanel.layout = BoxLayout(buttonPanel, BoxLayout.Y_AXIS)
        val addButton = JButton(AllIcons.General.Add)
        addButton.preferredSize = Dimension(35, 30)
        addButton.addActionListener {
            val value = JOptionPane.showInputDialog(discoveryRepoUrlsList, "Repo URL", "Git Repository URL",
                    JOptionPane.PLAIN_MESSAGE, Icons.MDWDLG, null,  "")
            value?.let {
                if (!discoveryRepoUrls.contains(value)) {
                    var msg: String? = null
                    try {
                        val url = URL(value as String)
                        if (!url.path.endsWith(".git")) {
                            msg = "Invalid Git repository URL:\n$url"
                        }
                    } catch (ex: MalformedURLException) {
                        msg = "Invalid repository URL:\n$value"
                    }
                    if (msg != null) {
                        JOptionPane.showMessageDialog(discoveryRepoUrlsList, msg,
                                "Bad Value", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                    }
                    else {
                        discoveryRepoUrls.add(value as String)
                        discoveryRepoUrlsList.setListData(discoveryRepoUrls.toTypedArray())
                        modified = true
                    }
                }
            }
        }
        buttonPanel.add(addButton)
        val removeButton = JButton(AllIcons.General.Remove)
        removeButton.preferredSize = Dimension(35, 30)
        removeButton.isEnabled = false
        removeButton.addActionListener {
            discoveryRepoUrlsList.selectedValue?.let { value ->
                if (discoveryRepoUrls.contains(value)) {
                    discoveryRepoUrls.remove(value)
                    discoveryRepoUrlsList.setListData(discoveryRepoUrls.toTypedArray())
                    modified = true
                }
            }
        }
        buttonPanel.add(removeButton)
        reposPanel.add(buttonPanel)

        discoveryRepoUrlsList.addListSelectionListener {
            removeButton.isEnabled = discoveryRepoUrlsList.selectedIndex >= 0
        }

        val maxPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        maxPanel.alignmentX = Component.LEFT_ALIGNMENT
        discoveryPanel.add(maxPanel)
        maxPanel.add(JLabel("Max Branches/Tags:"))
        maxPanel.add(maxBranchesTagsSpinner)
        maxBranchesTagsSpinner.addChangeListener {
            modified = true
        }


        // leftover vertical space
        gridConstraints.gridy = 4
        gridConstraints.fill = GridBagConstraints.VERTICAL
        gridConstraints.gridheight = GridBagConstraints.REMAINDER
        gridConstraints.weighty = 100.0
        val glue = Box.createVerticalGlue()
        settingsPanel.add(glue, gridConstraints)
    }

    override fun getId(): String {
        return "com.centurylink.mdw.studio"
    }

    override fun getDisplayName(): String {
        return "MDW"
    }

    override fun createComponent(): JComponent {
        return settingsPanel
    }
    override fun isModified(): Boolean {
        return modified
    }

    override fun apply() {
        val mdwSettings = MdwSettings.instance

        mdwSettings.isSuppressServerPolling = !serverPollingCheckbox.isSelected

        mdwSettings.isHideCanvasGridLines = !gridLinesCheckbox.isSelected
        mdwSettings.canvasZoom = zoomSlider.value

        mdwSettings.isSyncDynamicJavaClassName = syncDynamicJavaCheckbox.isSelected
        mdwSettings.isCreateAndAssociateTaskTemplate = createAndAssociateTaskCheckbox.isSelected

        mdwSettings.isAssetVercheckAutofix = vercheckAutofixCheckbox.isSelected
        if (!mdwSettings.isAssetVercheckAutofix) {
            // enable the prompt again
            PropertiesComponent.getInstance().setValue(MdwSettings.SUPPRESS_PROMPT_VERCHECK_AUTOFIX, false)
        }

        mdwSettings.discoveryRepoUrls = discoveryRepoUrls
        mdwSettings.discoveryMaxBranchesTags = maxBranchesTagsSpinner.number
    }
}