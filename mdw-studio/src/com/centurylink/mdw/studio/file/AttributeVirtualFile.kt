package com.centurylink.mdw.studio.file

import com.centurylink.mdw.activity.types.GeneralActivity
import com.centurylink.mdw.activity.types.ScriptActivity
import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.draw.ext.JsonObject
import com.centurylink.mdw.draw.ext.rootName
import com.centurylink.mdw.draw.model.Data
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.script.ScriptNaming
import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.util.PropertiesComponent
import com.intellij.lang.Language
import com.intellij.lang.LanguageParserDefinitions
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.fileTypes.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.MessageDialogBuilder
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.*
import com.intellij.psi.*
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
class AttributeVirtualFile(var workflowObj: WorkflowObj, value: String? = null,
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
            val type = getFileType()
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

    var _psiFile: PsiFile? = null
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
        return !workflowObj.isReadOnly
    }

    override fun getInputStream(): InputStream {
        return ByteArrayInputStream(contentsToByteArray())
    }

    override fun getOutputStream(requestor: Any?, newModificationStamp: Long, newTimeStamp: Long): OutputStream {
        return ByteArrayOutputStream()
    }
    override fun getFileSystem(): VirtualFileSystem {
        return attrFileSystem
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
        val attrFileSystem = AttributeVirtualFileSystem()
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

class AttributeVirtualFileSystem() : DeprecatedVirtualFileSystem(), NonPhysicalFileSystem {

    private val virtualFiles = mutableMapOf<String,AttributeVirtualFile>()

    init {
        startEventPropagation()
    }

    override fun getProtocol(): String {
        return PROTOCOL
    }

    /**
     * Finds or creates a dynamic Java/Script file.
     */
    fun getJavaOrScriptFile(workflowObj: WorkflowObj, contents: String? = null, qualifier: String? = null): AttributeVirtualFile? {
        assert(workflowObj.type == WorkflowType.activity)
        val projectSetup = workflowObj.project as ProjectSetup
        val process = workflowObj.asset as Process
        val implClass = workflowObj.obj.getString("implementor")
        projectSetup.implementors[implClass]?.let { implementor ->
            if (implementor.category == GeneralActivity::class.qualifiedName &&
                    (implClass == Data.Implementors.DYNAMIC_JAVA || workflowObj.getAttribute("Java") != null)) {
                var name = workflowObj.getAttribute("ClassName")
                if (name == null) {
                    name = JavaNaming.getValidClassName(process.rootName + "_" + workflowObj.id)
                }
                val filePath = "${process.packageName}/$name.java"
                val virtualFile = virtualFiles[filePath]
                return if (virtualFile == null) {
                    createFile(filePath, workflowObj, contents, "java", qualifier)
                }
                else {
                    virtualFile.workflowObj = workflowObj
                    if (contents != null) {
                        virtualFile.contents = contents
                    }
                    virtualFile
                }
            }
            else if (implementor.category == ScriptActivity::class.qualifiedName) {
                var name = ScriptNaming.getValidName(process.rootName + "_" + workflowObj.id)
                qualifier?.let { name += "_$it" }
                var ext = AttributeVirtualFile.DEFAULT_SCRIPT_EXT
                workflowObj.getAttribute("SCRIPT")?.let { scriptAttr ->
                    ext = AttributeVirtualFile.getScriptExt(scriptAttr)
                    val virtualFile = virtualFiles["${process.packageName}/$name.$ext"]
                    if (virtualFile != null) {
                        virtualFile.workflowObj = workflowObj
                        if (contents != null) {
                            virtualFile.contents = contents
                        }
                        return virtualFile
                    }
                }
                return createFile("${process.packageName}/$name.$ext", workflowObj, contents, ext, qualifier)
            }
        }
        return null
    }

    override fun findFileByPath(path: String): VirtualFile? {
        val activeProject = ProjectSetup.activeProject
        return if (activeProject == null) {
            LOG.warn("Cannot find active project for: $path")
            null
        }
        else {
            findFileByPath(path, activeProject)
        }
    }

    /**
     * Caches found VirtualFiles to avoid this:
     * https://youtrack.jetbrains.com/issue/IDEA-203751
     */
    fun findFileByPath(path: String, project: Project): VirtualFile? {
        val virtualFile = virtualFiles[path]
        val withoutExt = path.substring(0, path.lastIndexOf("."))
        val pkg = withoutExt.substring(0, withoutExt.indexOf("/"))
        val underscore = withoutExt.lastIndexOf("_")
        val processName = withoutExt.substring(pkg.length + 1, underscore)
        val activityId = withoutExt.substring(underscore + 1)

        val projectSetup = project.getComponent(ProjectSetup::class.java)

        projectSetup.findAssetFromNormalizedName(pkg, processName, "proc")?.let { asset ->
            val process = Process(JSONObject(String(asset.contents)))
            process.name = processName
            process.packageName = pkg
            process.id = asset.id
            process.activities.find { it.logicalId == activityId }?.let { activity ->
                activity.getAttribute("Java")?.let { java ->
                    val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json)
                    val file = virtualFile
                    if (file == null) {
                        createFile(path, workflowObj, java)
                    }
                    else {
                        file.workflowObj = workflowObj
                        file.contents = java
                        file._psiFile = null
                    }
                }
                activity.getAttribute("Rule")?.let { rule ->
                    val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json)
                    val file = virtualFile
                    if (file == null) {
                        createFile(path, workflowObj, rule)
                    }
                    else {
                        file.workflowObj = workflowObj
                        file.contents = rule
                        file._psiFile = null
                    }
                }
            }
        }

        return virtualFile
    }

    private fun createFile(path: String, workflowObj: WorkflowObj, contents: String? = null, ext: String? = null,
            qualifier: String? = null): AttributeVirtualFile {
        val vFile = AttributeVirtualFile(workflowObj, contents, ext, qualifier)
        virtualFiles[path] = vFile
        return vFile
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return findFileByPath(path)
    }

    override fun refresh(asynchronous: Boolean) {
    }

    companion object {
        val LOG = Logger.getInstance(AttributeVirtualFileSystem::class.java)
        const val PROTOCOL = "mdw"
        val instance: AttributeVirtualFileSystem
            get() = VirtualFileManager.getInstance().getFileSystem(PROTOCOL) as AttributeVirtualFileSystem
    }
}