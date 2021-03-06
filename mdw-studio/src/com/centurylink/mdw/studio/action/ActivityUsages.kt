package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.workflow.Activity
import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetUsage
import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.proc.ProcessEditor
import com.centurylink.mdw.studio.proj.Implementors
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.ui.SimpleTextAttributes
import com.intellij.usages.*
import com.intellij.usages.impl.rules.UsageType
import com.intellij.util.OpenSourceUtil
import javax.swing.Icon

class ActivityUsages : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            getImplementor(event)?.let { implementor ->
                val presentation = UsageViewPresentation()
                val tabCaptionText = "Activity Usages"
                presentation.tabName = tabCaptionText
                presentation.tabText = tabCaptionText
                presentation.isOpenInNewTab = false

                presentation.targetsNodeText = "Activity Implementor"
                val targets = arrayOf(ActivityUsageTarget(projectSetup, implementor))

                val usages = mutableListOf<ActivityUsage>()
                for (processAsset in projectSetup.findAssetsOfType("proc")) {
                    val contents = String(processAsset.file.contentsToByteArray())
                    val process = Process.fromString(contents)
                    for (activity in process.activities) {
                        if (activity.implementor == implementor.implementorClass) {
                            usages.add(ActivityUsage(projectSetup, processAsset, activity))
                        }
                    }
                }
                UsageViewManager.getInstance(projectSetup.project).showUsages(targets, usages.toTypedArray(), presentation)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val applicable = getImplementor(event) != null && Locator(event).project != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    private fun getImplementor(event: AnActionEvent): ActivityImplementor? {
        return Implementors.IMPLEMENTOR_DATA_KEY.getData(event.dataContext)
    }
}

class ActivityUsageTarget(private val projectSetup: ProjectSetup, private val implementor: ActivityImplementor)
    : UsageTarget {

    private val psiClass : PsiClass? by lazy {
        val scope = GlobalSearchScope.allScope(projectSetup.project)
        val psiFacade = JavaPsiFacade.getInstance(projectSetup.project)
        psiFacade.findClass(implementor.implementorClass, scope)
    }

    override fun isValid(): Boolean {
        return psiClass != null
    }

    override fun isReadOnly(): Boolean {
        return false
    }

    override fun getFiles(): Array<VirtualFile>? {
        val file = psiClass?.containingFile?.virtualFile
        return if (file == null) null else arrayOf(file)
    }

    override fun canNavigate(): Boolean {
        return isValid
    }

    override fun canNavigateToSource(): Boolean {
        return canNavigate()
    }

    override fun getName(): String {
        return implementor.implementorClass
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getLocationString(): String {
                return ""
            }
            override fun getIcon(unused: Boolean): Icon {
                return Icons.IMPL
            }
            override fun getPresentableText(): String {
                return name
            }
        }
    }

    override fun navigate(requestFocus: Boolean) {
        OpenSourceUtil.navigate(psiClass)
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

class ActivityUsage(private val projectSetup: ProjectSetup, private val processAsset: Asset,
        val activity: Activity) : AssetUsage {

    override val asset = processAsset
    override val targetName: String = activity.implementor
    val activityName: String
        get() = activity.name.replace("\\r", "").replace('\n', ' ')

    override fun isValid(): Boolean {
        return processAsset.file.isValid
    }

    override fun isReadOnly(): Boolean {
        return false
    }

    override fun canNavigate(): Boolean {
        return isValid
    }

    override fun canNavigateToSource(): Boolean {
        return canNavigate()
    }

    override fun navigate(requestFocus: Boolean) {
        for (ed in FileEditorManager.getInstance(projectSetup.project).openFile(processAsset.file, requestFocus)) {
            if (ed is ProcessEditor) {
                ed.canvas.preSelect(activity.logicalId)
            }
        }
    }

    override fun getPresentation(): UsagePresentation {
        return object : UsagePresentation {
            override fun getIcon(): Icon {
                return Icons.IMPL
            }
            override fun getPlainText(): String {
                return "${activity.logicalId} $activityName"
            }
            override fun getText(): Array<TextChunk> {
                return arrayOf(TextChunk(SimpleTextAttributes.GRAYED_ATTRIBUTES.toTextAttributes(), activity.logicalId, ACTIVITY_USAGE_TYPE),
                        TextChunk(SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES.toTextAttributes(), "$activityName", ACTIVITY_USAGE_TYPE))
            }
            override fun getTooltipText(): String? {
                return null
            }
        }
    }

    override fun getLocation(): FileEditorLocation? {
        val editor = FileEditorManager.getInstance(projectSetup.project).getSelectedEditor(processAsset.file)
        return if (editor is ProcessEditor) {
            return ActivityEditorLocation(editor, activity.logicalId)
        }
        else {
            null
        }
    }

    override fun selectInEditor() {
        // not called
    }

    override fun highlightInEditor() {
        // not called
    }

    class ActivityEditorLocation(private val processEditor: ProcessEditor, private val logicalId: String)
        : FileEditorLocation {
        override fun getEditor(): FileEditor {
            return processEditor
        }
        override fun compareTo(other: FileEditorLocation): Int {
            return if (other is ActivityEditorLocation) {
                logicalId.substring(1).toInt() - other.logicalId.substring(1).toInt()
            }
            else {
                0
            }
        }
    }

    companion object {
        val ACTIVITY_USAGE_TYPE = UsageType("Activity")
    }
}