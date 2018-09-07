package com.centurylink.mdw.studio.file

import com.centurylink.mdw.app.Templates
import com.centurylink.mdw.draw.edit.WorkflowObj
import com.centurylink.mdw.draw.ext.JsonObject
import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.script.ScriptNaming
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.fileTypes.FileTypes
import com.intellij.openapi.fileTypes.UnknownFileType
import com.intellij.testFramework.LightVirtualFileBase
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.io.OutputStream

/**
 * name and file type are determined based on workflowObj
 */
class AttributeVirtualFile(private val workflowObj: WorkflowObj, private val value: String?, private val ext: String? = null) :
        LightVirtualFileBase(workflowObj.name, FileTypes.PLAIN_TEXT, System.currentTimeMillis()) {

    val contents: String

    init {
        if (value == null) {
            contents = getTemplateContents() ?: ""
        }
        else {
            contents = value
        }

    }

    fun getTemplateContents(): String? {
        val ext = getExt()
        return when (ext) {
            "java" -> {
                var templateContents = Templates.get("assets/code/dynamic_java")
                templateContents = templateContents.replace("{{assetPackage}}", getJavaPackage())
                templateContents = templateContents.replace("{{className}}", getJavaClassName())
                return templateContents
            }
            else -> {
                Templates.get("assets/code/script_$ext")
            }
        }
    }

    fun getJavaPackage(): String {
        return JavaNaming.getValidPackageName(workflowObj.asset.packageName)
    }

    fun getJavaClassName(): String {
        return JavaNaming.getValidClassName(workflowObj.name + "_" + workflowObj.id)
    }

    override fun getName(): String {
        // eg: "PerformCriticalBusinessFunction_A5.java"
        val ext = getExt()
        return when (ext) {
            "java" -> {
                getJavaClassName() + "." + ext
            }
            else -> {
                ScriptNaming.getValidName(workflowObj.name + "_" + workflowObj.id) + "." + ext
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