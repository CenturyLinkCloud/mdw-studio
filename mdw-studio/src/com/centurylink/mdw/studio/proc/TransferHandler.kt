package com.centurylink.mdw.studio.proc

import com.centurylink.mdw.activity.types.TaskActivity
import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.constant.ProcessVisibilityConstant.*
import com.centurylink.mdw.constant.WorkAttributeConstant.EMBEDDED_PROCESS_TYPE
import com.centurylink.mdw.draw.Diagram
import com.centurylink.mdw.draw.DiagramEvent
import com.centurylink.mdw.draw.Step
import com.centurylink.mdw.draw.edit.UpdateListeners
import com.centurylink.mdw.draw.edit.UpdateListenersDelegate
import com.centurylink.mdw.model.task.TaskTemplate
import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.studio.prefs.MdwSettings
import com.centurylink.mdw.studio.file.TaskFileType
import com.centurylink.mdw.studio.proc.CanvasActions.Companion.DATA_FLAVOR_JSON
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import org.json.JSONObject

/**
 * Drag/drop handler for activity implementors from toolbox.
 */
class TransferHandler(private val diagram: Diagram) : javax.swing.TransferHandler(),
        UpdateListeners by UpdateListenersDelegate() {

    override fun canImport(support: TransferSupport): Boolean {
        if (!diagram.props.isReadonly && support.transferable.isDataFlavorSupported(DATA_FLAVOR_JSON)) {
            val data = JSONObject(support.transferable.getTransferData(DATA_FLAVOR_JSON).toString())
            val impl = getImplementor(data)
            if (impl != null) {
                if (impl.category == "subflow") {
                    diagram.process.subprocesses?.let {
                        for (subproc in it) {
                            val type = subproc.getAttribute(EMBEDDED_PROCESS_TYPE)
                            if (type == EMBEDDED_DELAY_PROCESS && impl.label.startsWith("Delay")) {
                                return false
                            }
                            else if (type == EMBEDDED_ERROR_PROCESS && impl.label.startsWith("Exception")) {
                                return false
                            }
                            else if (type == EMBEDDED_ABORT_PROCESS && impl.label.startsWith("Cancel")) {
                                return false
                            }
                        }
                    }
                }
                return true
            }
        }
        return false
    }

    override fun importData(support: TransferSupport): Boolean {
        if (support.transferable.isDataFlavorSupported(DATA_FLAVOR_JSON)) {
            val data = JSONObject(support.transferable.getTransferData(DATA_FLAVOR_JSON).toString())
            val impl = getImplementor(data)
            if (impl != null) {
                val dropPoint = support.dropLocation.dropPoint
                val x = maxOf(dropPoint.x - 60, 0)
                val y = maxOf(dropPoint.y - 30, 0)
                val drawable = diagram.onDrop(DiagramEvent(x, y), impl)
                if (impl.category == TaskActivity::class.qualifiedName) {
                    // create a task asset
                    val projectSetup = diagram.project as ProjectSetup
                    projectSetup.getPackage(diagram.process.packageName)?.let { pkg ->
                        val step = drawable as Step
                        var name = step.activity.name
                        var idx = 1
                        while (pkg.dir.findChild("$name.task") != null) {
                            name = "${step.activity.name} ${idx++}"
                        }
                        var create = false
                        val setting = "${MdwSettings.ID}.suppressCreateAndAssociateTaskTemplate"
                        if (PropertiesComponent.getInstance().getBoolean(setting, false)) {
                            create = MdwSettings.instance.isCreateAndAssociateTaskTemplate
                        }
                        else {
                            val res = MessageDialogBuilder
                                    .yesNo("Task Activity",
                                            "Create and associate task template:\n\"$name.task\"?")
                                    .doNotAsk(object : DialogWrapper.DoNotAskOption.Adapter() {
                                        override fun rememberChoice(isSelected: Boolean, res: Int) {
                                            if (isSelected) {
                                                create = res == Messages.YES
                                                MdwSettings.instance.isCreateAndAssociateTaskTemplate = create
                                                PropertiesComponent.getInstance().setValue(setting, true)
                                            }
                                        }
                                    })
                                    .show()
                            create = res == Messages.YES
                        }
                        if (create) {
                            val isAutoform = impl.implementorClass.contains("AutoForm") || impl.implementorClass.contains("Autoform")
                            val content = Templates.get(if (isAutoform) "assets/autoform.task" else "assets/custom.task")
                            val taskJson = JSONObject(content)
                            taskJson.put("name", name)
                            taskJson.put("logicalId", name)
                            taskJson.put("version", "0")
                            val task = TaskTemplate(taskJson)
                            val fileName = "$name.task"
                            val psiFile = PsiFileFactory.getInstance(projectSetup.project).createFileFromText(fileName, TaskFileType, task.json.toString(2))
                            WriteAction.run<Exception> {
                                PsiManager.getInstance(projectSetup.project).findDirectory(pkg.dir)?.add(psiFile)
                            }
                            step.activity.setAttribute(TaskActivity.ATTRIBUTE_TASK_TEMPLATE, "${pkg.name}/$fileName")
                        }
                    }
                }
                notifyUpdateListeners(diagram.workflowObj)
                return true
            }
        }
        return false
    }

    private fun getImplementor(json: JSONObject): ActivityImplementor? {
        json.optString("mdw.implementor")?.let { impl ->
            return diagram.implementors[impl]
        }
        return null
    }
}