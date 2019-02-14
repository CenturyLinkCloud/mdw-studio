package com.centurylink.mdw.studio.action

import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.studio.file.Icons
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import java.io.IOException
import javax.swing.Icon

abstract class NewRestApi(title: String, icon: Icon) : NewAssetAction(title, DESCRIPTION, icon) {

    /**
     * Uses MDW templates.  Does not do swagger codegen.
     */
    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
        val assetName = getAssetName(name)
        val baseName = getBaseName(name)
        if (baseName != JavaNaming.getValidClassName(baseName)) {
            throw IOException("Bad asset name")
        }

        val values = mutableMapOf<String,Any?>(
                "className" to baseName,
                "packageName" to assetPackageName
        )
        var path = "/"
        if (baseName != null) {
            path += baseName[0].toLowerCase() + baseName.substring(1)
        }
        val annotations = getAnnotations(path)
        values["annotations"] = annotations.first
        values["annotationsImports"] = annotations.second
        val content = substitute(loadTemplate(assetName, templatePath), values)
        return createAndOpen(dir, assetName, content)
    }

    /**
     * Returns a pair.  First element is annotations content, and second is any needed imports.
     */
    abstract fun getAnnotations(path: String): Pair<String,String>

    companion object {
        const val DESCRIPTION = "Create a REST API implementation"
    }
}

class NewRestApiJava : NewRestApi("Java Service", Icons.JAVA) {

    override val fileExtension = "java"
    override val fileType = JavaFileType.INSTANCE

    override fun getAnnotations(path: String): Pair<String,String> {
        return Pair<String,String>("@Path(\"$path\")\n@Api()\n", "import javax.ws.rs.Path;\nimport io.swagger.annotations.Api;\n")
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("Java Skeleton", Icons.JAVA, "assets/code/rest/skeletal_java")
    }
}

class NewRestApiKotlin : NewRestApi("Kotlin Service", Icons.KOTLIN) {

    override val fileExtension = "kt"
    override val fileType = FileTypeManager.getInstance().getFileTypeByExtension("kt")

    override fun getAnnotations(path: String): Pair<String,String> {
        return Pair<String,String>("@Path(\"$path\")\n@Api()\n", "import javax.ws.rs.Path\nimport io.swagger.annotations.Api\n")
    }

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("Kotlin Skeleton", Icons.KOTLIN, "assets/code/rest/skeletal_kt")
    }
}
