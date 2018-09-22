package com.centurylink.mdw.studio.file

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.draw.ext.JsonObject
import com.centurylink.mdw.draw.model.WorkflowObj
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
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElementFactory
import com.intellij.psi.PsiJavaParserFacade
import com.intellij.psi.PsiManager
import com.intellij.psi.util.PsiUtil
import com.intellij.testFramework.LightVirtualFileBase
import java.io.*

/**
 * name and file type are determined based on workflowObj
 */
class AttributeVirtualFile(private val workflowObj: WorkflowObj, private val value: String?, private val ext: String? = null) :
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

    fun syncDynamicJavaClassName() {
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
                                if (sync) {
                                    val fixedClass = PsiElementFactory.SERVICE.getInstance(project).createClass(dynamicJavaClassName)
                                    WriteCommandAction.writeCommandAction(project, psiFile).run<Exception> {
                                        psiClass.nameIdentifier?.replace(fixedClass.nameIdentifier!!)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
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
    }
}