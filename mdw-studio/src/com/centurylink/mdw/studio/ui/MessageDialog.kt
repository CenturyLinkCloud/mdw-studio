package com.centurylink.mdw.studio.ui

import com.intellij.openapi.project.Project

class MessageDialog(project: Project, title: String, message: String)
    : com.intellij.openapi.ui.messages.MessageDialog(project, "\n$message\n", title,
        arrayOf<String>(), 0, null, false)


