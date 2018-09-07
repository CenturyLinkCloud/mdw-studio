package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.file.ProcessFileType
import com.intellij.ide.actions.CreateFileFromTemplateDialog
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDirectory

class NewProcess : NewAssetAction("New MDW Process", "Create a workflow process", Icons.PROCESS) {

    override val fileExtension = "proc"
    override val fileType = ProcessFileType

    override fun buildDialog(project: Project, directory: PsiDirectory, builder: CreateFileFromTemplateDialog.Builder) {
        builder
            .setTitle(title)
            .addKind("Workflow Process", Icons.PROCESS, "assets/new.proc")
            .addKind("Service Process", Icons.PROCESS, "assets/service.proc")
    }

}