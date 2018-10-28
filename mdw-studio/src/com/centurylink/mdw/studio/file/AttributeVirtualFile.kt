package com.centurylink.mdw.studio.file

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.draw.ext.JsonObject
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.script.ScriptNaming
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtil
import com.intellij.testFramework.LightVirtualFileBase
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * name and file type are determined based on workflowObj
 */
class AttributeVirtualFile(private val workflowObj: WorkflowObj, value: String?, private val ext: String? = null) :
        LightVirtualFileBase(workflowObj.name, FileTypes.PLAIN_TEXT, System.currentTimeMillis()) {

    val contents = value ?: getTemplateContents() ?: ""

    private fun getTemplateContents(): String? {
        val ext = getExt()
        return when (ext) {
            "java" -> {
                var templateContents = Templates.get("assets/code/dynamic_java")
                templateContents = templateContents.replace("{{assetPackage}}", getJavaPackage())
                templateContents = templateContents.replace("{{className}}", getDynamicJavaClassName())
                return templateContents
            }
            else -> {
                Templates.get("assets/code/script_$ext")
            }
        }
    }

    private fun getJavaPackage(): String {
        return JavaNaming.getValidPackageName(workflowObj.asset.packageName)
    }

    override fun getFileSystem(): VirtualFileSystem {
        return attrFileSystem
    }

    override fun getName(): String {
        // eg: "PerformCriticalBusinessFunction_A5.java"
        val ext = getExt()
        return when (ext) {
            "java" -> {
                getDynamicJavaClassName() + "." + ext
            }
            else -> {
                getScriptName() + "." + ext
            }
        }
    }

    fun getExt(): String {
        if (ext != null) {
            return ext // documentation, etc
        }

        if (workflowObj.getAttribute("Java") != null || (workflowObj.obj.has("implementor") &&
                workflowObj.obj.get("implementor") == "com.centurylink.mdw.workflow.activity.java.DynamicJavaActivity")) {
            return "java"
        }
        for (langAttr in attrEditsJson.get("languageAttributes").asJsonArray) {
            val lang = workflowObj.getAttribute(langAttr.asString)
            if (lang != null) {
                return attrEditsJson.get("languages").asJsonObject.get(lang).asString
            }
        }
        if (workflowObj.obj.has("implementor") &&
                workflowObj.obj.get("implementor") == "com.centurylink.mdw.kotlin.ScriptActivity") {
            return "kts"
        }
        return "txt"
    }

    private fun getDynamicJavaClassName(): String {
        val process = workflowObj.asset as Process
        return JavaNaming.getValidClassName(process.name + "_" + workflowObj.id)
    }

    private fun getScriptName(): String {
        val process = workflowObj.asset as Process
        return ScriptNaming.getValidName(process.name + "_" + workflowObj.id)
    }

    /**
     * Returns the resulting class name (not qualified).
     */
    fun syncDynamicJavaClassName(): String? {
        val project = (workflowObj.project as ProjectSetup).project
        PsiManager.getInstance(project).findFile(this)?.let { psiFile ->
            if (psiFile is PsiClassOwner) {
                for (psiClass in psiFile.classes) {
                    psiClass.modifierList?.let { modifierList ->
                        if (PsiUtil.getAccessLevel(modifierList) == PsiUtil.ACCESS_LEVEL_PUBLIC) {
                            val dynamicJavaClassName = getDynamicJavaClassName()
                            if (psiClass.name != dynamicJavaClassName) {
                                var sync = false
                                val setting = "${MdwSettings.ID}.suppressPromptSyncDynamicJava"
                                if (PropertiesComponent.getInstance().getBoolean(setting, false)) {
                                    sync = MdwSettings.instance.isSyncDynamicJavaClassName
                                }
                                else {
                                    val res = MessageDialogBuilder
                                            .yesNo("Class Name Mismatch",
                                                    "Dynamic Java class name does not match expected: $dynamicJavaClassName.  Fix?")
                                            .doNotAsk(object : DialogWrapper.DoNotAskOption.Adapter() {
                                                override fun rememberChoice(isSelected: Boolean, res: Int) {
                                                    if (isSelected) {
                                                        sync = res == Messages.YES
                                                        MdwSettings.instance.isSyncDynamicJavaClassName = sync
                                                        PropertiesComponent.getInstance().setValue(setting, true)
                                                    }
                                                }
                                            })
                                            .show()
                                    sync = res == Messages.YES
                                }
                                return if (sync) {
                                    val fixedClass = PsiElementFactory.SERVICE.getInstance(project).createClass(dynamicJavaClassName)
                                    WriteCommandAction.writeCommandAction(project, psiFile).run<Exception> {
                                        psiClass.nameIdentifier?.replace(fixedClass.nameIdentifier!!)
                                    }
                                    dynamicJavaClassName
                                }
                                else {
                                    psiClass.name
                                }
                            }
                            return dynamicJavaClassName
                        }
                    }
                }
            }
        }
        return null
    }

    override fun getFileType(): FileType {
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension(getExt())
        if (fileType is UnknownFileType) {
            return FileTypes.PLAIN_TEXT
        }
        else {
            return fileType
        }
    }

    override fun contentsToByteArray(): ByteArray {
        return contents.toByteArray()
    }

    override fun getLength(): Long {
        return contentsToByteArray().size.toLong()
    }

    override fun getPath(): String {
        return workflowObj.asset.packageName + "/" + name
    }

    override fun isWritable(): Boolean {
        return !workflowObj.isReadonly
    }

    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(contentsToByteArray())
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        return ByteArrayOutputStream()
    }

    companion object {
        val attrEditsJson = JsonObject(Templates.get("configurator/attribute-edits.json"))
        val attrFileSystem = AttributeVirtualFileSystem()
    }
}

class AttributeVirtualFileSystem() : DeprecatedVirtualFileSystem(), NonPhysicalFileSystem {

    init {
        startEventPropagation()
    }

    override fun getProtocol(): String {
        return PROTOCOL
    }

    override fun findFileByPath(path: String): VirtualFile? {
        val withoutExt = path.substring(0, path.lastIndexOf("."))
        val pkg = withoutExt.substring(0, withoutExt.indexOf("/"))
        val underscore = withoutExt.lastIndexOf("_")
        val processName = withoutExt.substring(pkg.length + 1, underscore)
        val activityId = withoutExt.substring(underscore + 1)
        val assetPath = "$pkg/$processName.proc"

        ProjectSetup.activeProject?.let { project ->
            val projectSetup = project.getComponent(ProjectSetup::class.java)
            projectSetup.getAsset(assetPath)?.let{ asset ->
                val process = Process(JSONObject(String(asset.contents)))
                process.name = "$processName.proc"
                process.packageName = pkg
                process.id = asset.id
                process.activities.find{ it.logicalId == activityId }?.let { activity ->
                    activity.getAttribute("Java")?.let { java ->
                        val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json)
                        return AttributeVirtualFile(workflowObj, java)
                    }
                    activity.getAttribute("Rule")?.let { rule ->
                        val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json)
                        return AttributeVirtualFile(workflowObj, rule)
                    }
                }
            }
        }

        return null
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return findFileByPath(path)
    }

    override fun refresh(asynchronous: Boolean) {
    }

    companion object {
        const val PROTOCOL = "mdw"
    }
}