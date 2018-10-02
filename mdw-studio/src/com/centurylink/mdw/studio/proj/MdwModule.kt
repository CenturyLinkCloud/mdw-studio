package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.cli.Init
import com.centurylink.mdw.cli.Props
import com.centurylink.mdw.studio.MdwHelp
import com.centurylink.mdw.studio.file.Icons
import com.intellij.ide.util.projectWizard.JavaModuleBuilder
import com.intellij.ide.util.projectWizard.ModuleWizardStep
import com.intellij.ide.util.projectWizard.WizardContext
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.ModuleTypeManager
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.ui.configuration.ModulesProvider
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.JBTextField
import java.awt.*
import java.io.File
import javax.swing.*

class MdwModuleType : JavaModuleType(ID) {

    override fun getName() = "MDW"
    override fun getDescription() = "MDW Project"

    override fun createModuleBuilder(): MdwModuleBuilder {
        return MdwModuleBuilder()
    }

    companion object {
        const val ID = "mdwModuleType"
        val instance: MdwModuleType
            get() = ModuleTypeManager.getInstance().findByID(ID) as MdwModuleType
    }
}

class MdwModuleBuilder : JavaModuleBuilder() {

    enum class BuildType {
        Gradle,
        Maven
    }

    override fun getNodeIcon() = Icons.MDW

    var wizardContext: WizardContext? = null
    var initialUserId: String = System.getProperty("user.name")
    var buildType = BuildType.Gradle
    var groupId = "com.example"
    var isSpringBoot = true

    override fun getModuleType() = MdwModuleType.instance

    override fun setupRootModel(modifiableRootModel: ModifiableRootModel) {
        super.setupRootModel(modifiableRootModel)
        wizardContext?.let { context ->
            val init = Init(File(context.projectFileDirectory))
            Props.init("mdw.yaml")
            // init.isRunUpdate = false
            init.user = initialUserId
            init.isMaven = buildType == BuildType.Maven
            init.sourceGroup = groupId
            init.isSpringBoot = isSpringBoot
            init.run()
        }
    }

    override fun createWizardSteps(wizardContext: WizardContext, modulesProvider: ModulesProvider): Array<ModuleWizardStep> {
        this.wizardContext = wizardContext
        val steps = super.createWizardSteps(wizardContext, modulesProvider).toMutableList()
        steps.add(MdwModuleWizardStep(this))
        return steps.toTypedArray()
    }
}

class MdwModuleWizardStep(private val moduleBuilder: MdwModuleBuilder) : ModuleWizardStep() {

    private val mainPanel = JPanel(BorderLayout())

    private val initialUserText = object : JBTextField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(500, super.getPreferredSize().height)
        }
    }
    private val groupIdText = object : JBTextField() {
        override fun getPreferredSize(): Dimension {
            return Dimension(500, super.getPreferredSize().height)
        }
    }
    private val buildTypeButtonGroup = ButtonGroup()
    private val springBootCheckbox = JBCheckBox("Generate Spring Boot artifacts")

    override fun getHelpId(): String {
        return MdwHelp.CREATE_PROJECT
    }

    init {
        mainPanel.layout = GridBagLayout()

        val gridConstraints = GridBagConstraints()
        gridConstraints.anchor = GridBagConstraints.NORTH
        gridConstraints.gridx = 0
        gridConstraints.gridy = 0
        gridConstraints.fill = GridBagConstraints.HORIZONTAL
        gridConstraints.weightx = 1.0
        gridConstraints.weighty = 1.0

        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        mainPanel.add(panel, gridConstraints)

        val initialUserPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        initialUserPanel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(initialUserPanel)
        val initialUserLabel = object : JLabel("Initial User ID") {
            override fun getPreferredSize(): Dimension {
                return Dimension(150, super.getPreferredSize().height)
            }
        }
        initialUserPanel.add(initialUserLabel)
        initialUserText.text = moduleBuilder.initialUserId
        initialUserPanel.add(initialUserText)

        val buildTypePanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        buildTypePanel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(buildTypePanel)
        val buildTypeLabel = object : JLabel("Build Type") {
            override fun getPreferredSize(): Dimension {
                return Dimension(153, super.getPreferredSize().height)
            }
        }
        buildTypePanel.add(buildTypeLabel)

        val buildTypeButtonPanel = object: JPanel(FlowLayout(FlowLayout.LEFT, 10, 5)) {
            override fun getPreferredSize(): Dimension {
                return Dimension(493, super.getPreferredSize().height)
            }
            override fun getWidth(): Int {
                return 493
            }
        }
        buildTypePanel.add(buildTypeButtonPanel)
        buildTypeButtonPanel.border = BorderFactory.createLineBorder(JBColor.border())
        val gradleButton = JBRadioButton("Gradle")
        gradleButton.actionCommand = MdwModuleBuilder.BuildType.Gradle.toString()
        gradleButton.isSelected = moduleBuilder.buildType == MdwModuleBuilder.BuildType.Gradle
        buildTypeButtonPanel.add(gradleButton)
        buildTypeButtonGroup.add(gradleButton)
        val mavenButton = JBRadioButton("Maven")
        mavenButton.actionCommand = MdwModuleBuilder.BuildType.Gradle.toString()
        mavenButton.isSelected = moduleBuilder.buildType == MdwModuleBuilder.BuildType.Maven
        buildTypeButtonPanel.add(mavenButton)
        buildTypeButtonGroup.add(mavenButton)

        val groupIdPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        groupIdPanel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(groupIdPanel)
        val groupIdLabel = object : JLabel("Group ID") {
            override fun getPreferredSize(): Dimension {
                return Dimension(150, super.getPreferredSize().height)
            }
        }
        groupIdPanel.add(groupIdLabel)
        groupIdText.text = moduleBuilder.groupId
        groupIdPanel.add(groupIdText)

        val springBootPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        springBootPanel.alignmentX = Component.LEFT_ALIGNMENT
        panel.add(springBootPanel)
        val springBootLabel = object : JLabel("") {
            override fun getPreferredSize(): Dimension {
                return Dimension(150, super.getPreferredSize().height)
            }
        }
        springBootPanel.add(springBootLabel)
        springBootCheckbox.isSelected = moduleBuilder.isSpringBoot
        springBootPanel.add(springBootCheckbox)
    }

    override fun updateDataModel() {
        moduleBuilder.initialUserId = initialUserText.text
        moduleBuilder.buildType = MdwModuleBuilder.BuildType.valueOf(buildTypeButtonGroup.selection.actionCommand)
        moduleBuilder.groupId = groupIdText.text
        moduleBuilder.isSpringBoot = springBootCheckbox.isSelected
    }

    override fun validate(): Boolean {
        return !initialUserText.text.isBlank() && !groupIdText.text.isBlank()
    }

    override fun getComponent(): JComponent {
        return mainPanel
    }
}
