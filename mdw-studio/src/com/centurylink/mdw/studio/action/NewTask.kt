package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.file.TaskFileType
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory
import org.json.JSONObject

class NewTask : NewAssetAction("New MDW Task", "Create a task template", Icons.TASK) {

    override val fileExtension = "task"
    override val fileType = TaskFileType

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
                .setTitle(title)
                .addKind("Autoform Task", Icons.TASK, "assets/autoform.task")
                .addKind("Custom Task", Icons.TASK, "assets/custom.task")
    }

    override fun loadTemplate(fileName: String, path: String): String {
        // populate name and logical id (TODO: remove name and version properties from task json)
        val content = super.loadTemplate(fileName, path)
        val taskJson = JSONObject(content)
        val name = fileName.substring(0, fileName.length - ".task".length)
        taskJson.put("name", name)
        taskJson.put("logicalId", name)
        taskJson.put("version", "0")
        return taskJson.toString(2)
    }

}