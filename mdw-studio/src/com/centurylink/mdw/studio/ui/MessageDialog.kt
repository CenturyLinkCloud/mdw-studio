package com.centurylink.mdw.studio.ui

import com.centurylink.mdw.studio.file.Icons
import com.intellij.CommonBundle
import com.intellij.openapi.project.Project

class MessageDialog(project: Project, title: String, message: String)
    : com.intellij.openapi.ui.messages.MessageDialog(project, "\n$message\n", title,
        arrayOf<String>(CommonBundle.getOkButtonText()), 0, Icons.MDWDLG, false) {


}


