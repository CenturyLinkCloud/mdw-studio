package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.proj.Implementors
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.usages.UsageTarget
import com.intellij.usages.UsageViewManager
import com.intellij.usages.UsageViewPresentation
import javax.swing.Icon

class ActivityUsages : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).getProjectSetup()?.let { projectSetup ->
            getImplementor(event)?.let { implementor ->
                val presentation = UsageViewPresentation()
                val tabCaptionText = "Activity Usages"
                presentation.setTargetsNodeText("Usages of: ${implementor.implementorClass}")
                presentation.tabName = tabCaptionText
                presentation.tabText = tabCaptionText
                presentation.isOpenInNewTab = false

                val implDecl = "\"implementor\": \"${implementor.implementorClass}\""
                val processAssets = mutableListOf<Asset>()
                for (processAsset in projectSetup.findAssetsOfType("proc")) {
                    if (String(processAsset.file.contentsToByteArray()).contains(implDecl)) {
                        if (!processAssets.contains(processAsset)) {
                            processAssets.add(processAsset)
                        }
                    }
                }
                val targets = mutableListOf<UsageTarget>()
                for (processAsset in processAssets) {
                    targets.add(ActivityUsageTarget(projectSetup, processAsset))
                }

                UsageViewManager.getInstance(projectSetup.project).showUsages(targets.toTypedArray(), arrayOf(), presentation)
            }
        }
    }

    override fun update(event: AnActionEvent) {

        val applicable = getImplementor(event) != null && Locator(event).getProjectSetup() != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    private fun getImplementor(event: AnActionEvent): ActivityImplementor? {
        return Implementors.IMPLEMENTOR_DATA_KEY.getData(event.dataContext)
    }
}

class ActivityUsageTarget(private val projectSetup: ProjectSetup, private val processAsset: Asset) : UsageTarget {

    override fun isValid(): Boolean {
        return processAsset.file.isValid
    }

    override fun isReadOnly(): Boolean {
        return false
    }

    override fun getFiles(): Array<VirtualFile>? {
        return arrayOf(processAsset.file)
    }

    override fun canNavigate(): Boolean {
        return isValid
    }

    override fun canNavigateToSource(): Boolean {
        return canNavigate()
    }

    override fun getName(): String {
        return processAsset.path
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getLocationString(): String {
                return processAsset.pkg.name
            }
            override fun getIcon(unused: Boolean): Icon {
                return Icons.PROCESS
            }
            override fun getPresentableText(): String {
                return processAsset.path
            }
        }
    }

    override fun navigate(requestFocus: Boolean) {
        FileEditorManager.getInstance(projectSetup.project).openFile(processAsset.file, true)
    }

    override fun update() {
    }

    override fun highlightUsages(file: PsiFile, editor: Editor, clearHighlights: Boolean) {
    }

    override fun findUsages() {
    }

    override fun findUsagesInEditor(editor: FileEditor) {
    }

}