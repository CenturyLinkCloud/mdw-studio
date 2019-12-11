package com.centurylink.mdw.studio.file

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.draw.ext.JsonObject
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.script.ScriptNaming
import com.centurylink.mdw.studio.prefs.MdwSettings
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.util.PropertiesComponent
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.VirtualFileSystem
import com.intellij.psi.*
import com.intellij.psi.util.PsiUtil
import com.intellij.testFramework.LightVirtualFileBase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * name and file type are determined based on workflowObj
 */
class AttributeVirtualFile(var workflowObj: WorkflowObj, val attributeName: String, value: String? = null,
        private val fileExtension: String? = null, private val qualifier: String? = null) :
        LightVirtualFileBase(workflowObj.name, FileTypes.PLAIN_TEXT, System.currentTimeMillis()) {

    val projectSetup: ProjectSetup
        get() = workflowObj.project as ProjectSetup

    val project: Project
        get() = projectSetup.project

    var contents = value ?: templateContents ?: ""

    val ext: String
        get() {
            if (fileExtension != null) {
                return fileExtension // documentation, etc
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

    val language: Language?
        get() {
            val type = fileType
            return if (type is LanguageFileType) {
                type.language
            }
            else {
                null
            }
        }

    val templateContents: String?
        get() {
            return when (ext) {
                "java" -> {
                    var templateContents = Templates.get("assets/code/dynamic_java")
                    templateContents = templateContents.replace("{{assetPackage}}", javaPackage)
                    templateContents = templateContents.replace("{{className}}", dynamicJavaClassName)
                    return templateContents
                }
                else -> {
                    Templates.get("assets/code/script_$ext")
                }
            }
        }

    private val javaPackage: String
      get() = JavaNaming.getValidPackageName(workflowObj.asset.packageName)

    private val dynamicJavaClassName: String
        get() {
            val process = workflowObj.asset as Process
            return JavaNaming.getValidClassName(process.name + "_" + workflowObj.id)
        }

    private val scriptName: String
        get() {
            val process = workflowObj.asset as Process
            var name = process.name + "_" + workflowObj.id
            qualifier?.let { name += "_$it" }
            return ScriptNaming.getValidName(name)
        }

    private var _psiFile: PsiFile? = null
    val psiFile: PsiFile?
        get() {
            if (_psiFile == null) {
                language?.let { lang ->
                    val factory = LanguageFileViewProviders.INSTANCE.forLanguage(lang)
                    val psiManager = PsiManager.getInstance(project)
                    var viewProvider: FileViewProvider? = factory?.createFileViewProvider(this, language, psiManager, true)
                    if (viewProvider == null) {
                        viewProvider = SingleRootFileViewProvider(psiManager, this, true)
                    }
                    val baseLang = viewProvider.baseLanguage
                    val parserDefinition = LanguageParserDefinitions.INSTANCE.forLanguage(baseLang)
                    if (parserDefinition != null) {
                        _psiFile = viewProvider.getPsi(baseLang)
                    }
                }
            }
            return _psiFile
        }
    fun refreshPsi() {
        _psiFile = null
    }

    /**
     * Returns the resulting class name (not qualified).
     */
    fun syncDynamicJavaClassName(): String? {
        PsiManager.getInstance(project).findFile(this)?.let { psiFile ->
            if (psiFile is PsiClassOwner) {
                for (psiClass in psiFile.classes) {
                    psiClass.modifierList?.let { modifierList ->
                        if (PsiUtil.getAccessLevel(modifierList) == PsiUtil.ACCESS_LEVEL_PUBLIC) {
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
        val fileType = FileTypeManager.getInstance().getFileTypeByExtension(ext)
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
        return !workflowObj.props.isReadonly
    }

    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(contentsToByteArray())
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        return ByteArrayOutputStream()
    }
    override fun getFileSystem(): VirtualFileSystem {
        return AttributeVirtualFileSystem.instance
    }

    override fun getName(): String {
        // eg: "MyProcess_A5.java"
        return when (ext) {
            "java" -> {
                "$dynamicJavaClassName.$ext"
            }
            else -> {
                "$scriptName.$ext"
            }
        }
    }

    override fun equals(other: Any?): Boolean {
        return other is AttributeVirtualFile && other.project == project && other.path == path
    }

    override fun hashCode(): Int {
        return "${project.name}~$path".hashCode()
    }

    companion object {
        val attrEditsJson = JsonObject(Templates.get("configurator/attribute-edits.json"))
        const val DEFAULT_SCRIPT_EXT = "groovy"
        fun getScriptExt(scriptAttr: String): String {
            return when(scriptAttr) {
                "Kotlin Script" -> "kts"
                "Groovy" -> "groovy"
                "JavaScript" -> "js"
                else -> DEFAULT_SCRIPT_EXT
            }
        }
    }
}