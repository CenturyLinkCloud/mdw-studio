package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.file.Asset
import com.centurylink.mdw.studio.file.AssetUsage
import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.proc.ProcessEditor
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.navigation.ItemPresentation
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorLocation
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.ui.SimpleTextAttributes
import com.intellij.usages.*
import com.intellij.usages.impl.rules.UsageType
import org.json.JSONObject
import javax.swing.Icon

class ProcessUsages : AnAction() {

    override fun actionPerformed(event: AnActionEvent) {
        val locator = Locator(event)
        Locator(event).projectSetup?.let { projectSetup ->
            locator.asset?.let { asset ->
                val presentation = UsageViewPresentation()
                val tabCaptionText = "Process Usages"
                presentation.tabName = tabCaptionText
                presentation.tabText = tabCaptionText
                presentation.isOpenInNewTab = false

                presentation.targetsNodeText = "Process"
                val process = Process(JSONObject(String(asset.file.contentsToByteArray())))
                process.name = asset.name.substring(0, asset.name.indexOf('.'))
                process.packageName = asset.pkg.name
                process.version = asset.version
                val targets = arrayOf(ProcessUsageTarget(projectSetup, process))

                val usages = mutableListOf<ProcessUsage>()
                for (processAsset in projectSetup.findAssetsOfType("proc")) {
                    val parent = Process(JSONObject(String(processAsset.file.contentsToByteArray())))
                    if (parent.invokesSubprocess(process)) {
                        usages.add(ProcessUsage(projectSetup, processAsset, process))
                    }
                }
                UsageViewManager.getInstance(projectSetup.project).showUsages(targets, usages.toTypedArray(), presentation)
            }
        }
    }

    override fun update(event: AnActionEvent) {
        Locator(event).asset?.let { asset ->
            val applicable = asset.ext == "proc"
            event.presentation.isVisible = applicable
            event.presentation.isEnabled = applicable
        }
    }
}

class ProcessUsageTarget(private val projectSetup: ProjectSetup, private val process: Process)
    : UsageTarget {

    override fun isValid(): Boolean {
        return true
    }

    override fun isReadOnly(): Boolean {
        return false
    }

    override fun getFiles(): Array<VirtualFile>? {
        val file = projectSetup.getAssetFile(process.qualifiedName + ".proc" )
        return if (file == null) null else arrayOf(file)
    }

    override fun canNavigate(): Boolean {
        return isValid
    }

    override fun canNavigateToSource(): Boolean {
        return canNavigate()
    }

    override fun getName(): String {
        return process.name
    }

    override fun getPresentation(): ItemPresentation {
        return object : ItemPresentation {
            override fun getLocationString(): String {
                return ""
            }
            override fun getIcon(unused: Boolean): Icon {
                return Icons.PROCESS
            }
            override fun getPresentableText(): String {
                return name
            }
        }
    }

    override fun navigate(requestFocus: Boolean) {
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

class ProcessUsage(private val projectSetup: ProjectSetup, private val processAsset: Asset,
                    val process: Process) : AssetUsage {

    override val asset = processAsset
    override val targetName: String = process.name
    val procName: String
        get() = process.name.replace("\\r", "").replace('\n', ' ')

    override fun isValid(): Boolean {
        val parent = Process(JSONObject(String(processAsset.file.contentsToByteArray())))
        return processAsset.file.isValid && parent.invokesSubprocess(process)
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
                val parent = Process(JSONObject(String(processAsset.file.contentsToByteArray())))
                if (parent.activities != null) {
                    for (activity in parent.activities) {
                        if (activity.invokesSubprocess(process)) {
                            ed.canvas.preSelect(activity.logicalId)
                            break
                        }
                    }
                }
            }
        }
    }

    override fun getPresentation(): UsagePresentation {
        return object : UsagePresentation {
            override fun getIcon(): Icon {
                return Icons.PROCESS
            }
            override fun getPlainText(): String {
                return process.name
            }
            override fun getText(): Array<TextChunk> {
                return arrayOf(TextChunk(SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES.toTextAttributes(), procName, PROCESS_USAGE_TYPE))
            }
            override fun getTooltipText(): String? {
                return null
            }
        }
    }

    override fun getLocation(): FileEditorLocation? {
        val editor = FileEditorManager.getInstance(projectSetup.project).getSelectedEditor(processAsset.file)
        return if (editor is ProcessEditor) {
            return ProcessEditorLocation(editor, process.name)
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

    class ProcessEditorLocation(private val processEditor: ProcessEditor, private val name: String)
        : FileEditorLocation {
        override fun getEditor(): FileEditor {
            return processEditor
        }
        override fun compareTo(other: FileEditorLocation): Int {
            return if (other is ProcessEditorLocation) {
                name.substring(1).toInt() - other.name.substring(1).toInt()
            }
            else {
                0
            }
        }
    }

    companion object {
        val PROCESS_USAGE_TYPE = UsageType("Prcoess")
    }
}