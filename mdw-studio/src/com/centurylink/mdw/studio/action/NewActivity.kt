package com.centurylink.mdw.studio.action

import com.centurylink.mdw.activity.types.GeneralActivity
import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.file.Icons
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import org.json.JSONObject
import java.io.IOException
import javax.swing.Icon

abstract class NewActivity(title: String, icon: Icon) : NewAssetAction(title, DESCRIPTION, icon) {

    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
        val assetName = getAssetName(name)
        val baseName = getBaseName(name)
        if (baseName != JavaNaming.getValidClassName(baseName)) {
            throw IOException("Bad asset name")
        }

        val implFileName = "$baseName.impl"
        val implTemplatePath = "assets/" + templatePath.substring(21, templatePath.lastIndexOf("_")) + ".impl"
        val values = mutableMapOf<String,Any?>(
                "className" to baseName,
                "packageName" to assetPackageName
        )
        val implContent = substitute(loadTemplate(implFileName, implTemplatePath), values)

        val is6109 = projectSetup?.mdwVersion?.checkRequiredVersion(6, 1, 9)
        if (is6109 == true) {
            val annotations = getAnnotations(JSONObject(implContent))
            values["annotations"] = annotations.first
            values["annotationsImports"] = annotations.second
        }
        else {
            createAndOpen(dir, implFileName, implContent)
        }

        val content = substitute(loadTemplate(assetName, templatePath), values)
        return createAndOpen(dir, assetName, content)
    }

    /**
     * Returns a pair.  First element is annotations content, and second is any needed imports.
     */
    private fun getAnnotations(implJson: JSONObject): Pair<String,String> {
        val name = implJson.getString("label")
        val category = implJson.getString("category")
        val icon = implJson.getString("icon")
        val pageletXml = implJson.getString("pagelet")

        val defaultCategory = GeneralActivity::class.java.name
        val defaultIcon = "shape:activity"
        val defaultPagelet = "<PAGELET></PAGELET>"

        var imports = "import com.centurylink.mdw.annotations.Activity${if (fileExtension == "kt") "" else ";"}\n"
        var annotations = "@Activity("
        if (category == defaultCategory && icon == defaultIcon && pageletXml == defaultPagelet) {
            annotations += "\"$name\")\n"
        }
        else {
            annotations += "value=\"$name\""
            if (category != defaultCategory) {
                imports += "import $category;\n"
                var categoryClass = category.substring(category.lastIndexOf('.') + 1)
                categoryClass += if (fileExtension == "kt") "::class" else ".class"
                annotations += ", category=$categoryClass"
            }
            if (icon != defaultIcon) {
                annotations += ", icon=\"$icon\""
            }
            if (pageletXml != defaultPagelet) {
                val pagelet = Pagelet(pageletXml).json.toString().replace("\"","\\\"")
                annotations += ",\n                pagelet=\"$pagelet\""
            }
            annotations += ")\n"
        }
        return Pair(annotations, imports)
    }

    companion object {
        const val DESCRIPTION = "Create an activity implementor"
    }
}

class NewActivityJava : NewActivity("Java Activity", Icons.JAVA) {

    override val fileExtension = "java"
    override val fileType = JavaFileType.INSTANCE

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("General Activity", Icons.JAVA, "assets/code/activity/general_java")
            .addKind("Adapter Activity", Icons.JAVA, "assets/code/activity/adapter_java")
            .addKind("Evaluator Activity", Icons.JAVA, "assets/code/activity/evaluator_java")
    }
}

class NewActivityKotlin : NewActivity("Kotlin Activity", Icons.KOTLIN) {

    override val fileExtension = "kt"
    override val fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("General Activity", Icons.KOTLIN, "assets/code/activity/general_kt")
            .addKind("Adapter Activity", Icons.KOTLIN, "assets/code/activity/adapter_kt")
            .addKind("Evaluator Activity", Icons.KOTLIN, "assets/code/activity/evaluator_kt")
    }
}
