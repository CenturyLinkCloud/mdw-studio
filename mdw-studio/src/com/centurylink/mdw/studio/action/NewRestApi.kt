package com.centurylink.mdw.studio.action

import com.centurylink.mdw.java.JavaNaming
import com.centurylink.mdw.studio.file.Icons
import com.intellij.icons.AllIcons
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import java.io.IOException
import javax.swing.Icon

abstract class NewRestApi(title: String, description: String, icon: Icon) : NewAssetAction(title, description, icon) {
    override val fileExtension = "java"
    override val fileType = JavaFileType.INSTANCE

    /**
     * Uses MDW templates.  Does not do swagger codegen.
     */
    override fun createFile(name: String, templatePath: String, dir: PsiDirectory): PsiFile? {
        val assetName = getAssetName(name)
        val baseName = getBaseName(name)
        if (baseName != JavaNaming.getValidClassName(baseName)) {
            throw IOException("Bad asset name")
        }

        val pkg = assetPackage ?: return null
        val values = mutableMapOf<String,Any?>(
                "className" to baseName,
                "packageName" to pkg.name
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
    protected fun getAnnotations(path: String): Pair<String,String> {
        return Pair<String,String>("@Path(\"$path\")\n", "import javax.ws.rs.Path;\n")
    }

}

class NewRestApiSkeletal() : NewRestApi("From Scratch", "Create a REST API implementation", Icons.JAVA) {

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("Java Skeleton", Icons.JAVA, "assets/code/rest/skeletal_java")
    }
}

class NewRestApiSwagger() : NewRestApi("From Swagger", "Create a REST API implementation from Swagger", AllIcons.FileTypes.JsonSchema) {
    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("Swagger Codegen", Icons.JAVA, "assets/code/rest/skeletal_java")
    }

}