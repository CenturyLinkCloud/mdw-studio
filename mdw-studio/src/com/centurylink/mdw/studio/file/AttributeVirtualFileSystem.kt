package com.centurylink.mdw.studio.file

import com.centurylink.mdw.activity.types.AdapterActivity
import com.centurylink.mdw.activity.types.GeneralActivity
import com.centurylink.mdw.activity.types.ScriptActivity
import com.centurylink.mdw.draw.ext.rootName
import com.centurylink.mdw.draw.model.WorkflowObj
import com.centurylink.mdw.draw.model.WorkflowType
import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.script.ScriptNaming
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.DeprecatedVirtualFileSystem
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import org.json.JSONObject

class AttributeVirtualFileSystem : DeprecatedVirtualFileSystem(), NonPhysicalFileSystem {

    internal val virtualFiles = mutableMapOf<String,AttributeVirtualFile>()

    init {
        AttributeVirtualFile.attrFileSystem = this
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
    fun findFileByPath(path: String, project: Project): AttributeVirtualFile? {
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
                    val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json, false)
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
                    val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json, false)
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

        ApplicationManager.getApplication().runReadAction {
            val document = FileDocumentManager.getInstance().getDocument(vFile)
            document?.addDocumentListener(
                object : DocumentListener {
                    override fun beforeDocumentChange(e: DocumentEvent) {
                    }
                    override fun documentChanged(e: DocumentEvent) {
                        println("CHANGED: \n${e.document.text}")
                    }
                }
            )
        }
        virtualFiles[path] = vFile
        return vFile
    }

    override fun refreshAndFindFileByPath(path: String): VirtualFile? {
        return findFileByPath(path)
    }

    fun refresh(projectSetup: ProjectSetup) {
        virtualFiles.clear()
        for (asset in projectSetup.findAssetsOfType("proc")) {
            val process = Process(JSONObject(String(asset.contents)))
            process.name = asset.rootName
            process.packageName = asset.pkg.name
            process.id = asset.id
            for (activity in process.activities) {
                activity.getAttribute("Java")?.let { java ->
                    val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json, false)
                    var name = workflowObj.getAttribute("ClassName")
                    if (name == null) {
                        name = JavaNaming.getValidClassName(process.rootName + "_" + workflowObj.id)
                    }
                    createFile("${process.packageName}/$name.java", workflowObj, java)
                    return
                }
                activity.getAttribute("Rule")?.let { rule ->
                    val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json, false)
                    projectSetup.implementors[workflowObj.obj.getString("implementor")]?.let { implementor ->
                        if (implementor.category == ScriptActivity::class.qualifiedName) {
                            val name = ScriptNaming.getValidName(process.rootName + "_" + workflowObj.id)
                            var ext = AttributeVirtualFile.DEFAULT_SCRIPT_EXT
                            workflowObj.getAttribute("SCRIPT")?.let { scriptAttr ->
                                ext = AttributeVirtualFile.getScriptExt(scriptAttr)
                            }
                            createFile("${process.packageName}/$name.$ext", workflowObj, rule, ext)
                        }
                    }
                    return
                }
                activity.getAttribute("PreScript")?.let { script ->
                    val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json, false)
                    projectSetup.implementors[workflowObj.obj.getString("implementor")]?.let { implementor ->
                        if (implementor.category == AdapterActivity::class.qualifiedName) {
                            val name = ScriptNaming.getValidName(process.rootName + "_" + workflowObj.id)
                            var ext = AttributeVirtualFile.DEFAULT_SCRIPT_EXT
                            workflowObj.getAttribute("PreScriptLang")?.let { langAttr ->
                                ext = AttributeVirtualFile.getScriptExt(langAttr)
                            }
                            createFile("${process.packageName}/$name.$ext", workflowObj, script, ext, "Pre")
                        }
                    }
                }
                activity.getAttribute("PostScript")?.let { script ->
                    val workflowObj = WorkflowObj(projectSetup, process, WorkflowType.activity, activity.json, false)
                    projectSetup.implementors[workflowObj.obj.getString("implementor")]?.let { implementor ->
                        if (implementor.category == AdapterActivity::class.qualifiedName) {
                            val name = ScriptNaming.getValidName(process.rootName + "_" + workflowObj.id)
                            var ext = AttributeVirtualFile.DEFAULT_SCRIPT_EXT
                            workflowObj.getAttribute("PostScriptLang")?.let { langAttr ->
                                ext = AttributeVirtualFile.getScriptExt(langAttr)
                            }
                            createFile("${process.packageName}/$name.$ext", workflowObj, script, ext, "Post")
                        }
                    }
                }
            }
        }
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