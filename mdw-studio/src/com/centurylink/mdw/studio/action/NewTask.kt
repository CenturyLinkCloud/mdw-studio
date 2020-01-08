package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.file.TaskFileType
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.json.JSONObject

class NewTask : NewAssetAction("MDW Task", "Create a task template", Icons.TASK) {

    override val fileExtension = "task"
    override val fileType = TaskFileType

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("Autoform Task", Icons.TASK, "assets/autoform.task")
            .addKind("Custom Task", Icons.TASK, "assets/custom.task")

        projectSetup?.let { projectSetup ->
            for (pageletAsset in projectSetup.findAssetsOfType("pagelet")) {
                if (pageletAsset.pkg.name != Data.BASE_PKG && !String(pageletAsset.file.contentsToByteArray()).startsWith('{')) {
                    // custom task pagelet XML
                    builder.addKind(pageletAsset.file.nameWithoutExtension, Icons.TASK, pageletAsset.path)
                }
            }
        }
    }

    override fun loadTemplate(fileName: String, path: String): String {
        // populate name and logical id
        val content = if (path == "assets/autoform.task" || path == "assets/custom.task") {
            super.loadTemplate(fileName, path)
        } else {
            super.loadTemplate(fileName, "assets/autoform.task")
        }

        val taskJson = JSONObject(content)
        val name = fileName.substring(0, fileName.length - ".task".length)
        taskJson.put("name", name)
        taskJson.put("logicalId", name)
        taskJson.put("version", "0")
        if (path != "assets/autoform.task" && path != "assets/custom.task") {
            taskJson.getJSONObject("attributes").put("FormName", path)
        }
        return taskJson.toString(2)
    }

}