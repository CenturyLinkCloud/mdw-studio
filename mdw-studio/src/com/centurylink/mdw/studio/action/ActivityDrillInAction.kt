package com.centurylink.mdw.studio.action

import com.intellij.openapi.actionSystem.AnActionEvent

class ActivityDrillInAction : AssetAction() {

    override fun actionPerformed(e: AnActionEvent) {
        println("DRILL IN")
    }

    override fun update(event: AnActionEvent) {
        // dynamically determined
    }

    companion object {
        const val OPEN_SCRIPT = "Open Script"
        const val OPEN_SUBPROCESS = "Open Subprocess"
        const val OPEN_JAVA = "Open Java"
    }

}