package com.centurylink.mdw.studio.action

import com.centurylink.mdw.studio.MdwSettings
import com.centurylink.mdw.studio.proc.ProcessEditor
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.fileEditor.FileEditorManager

interface CanvasAction {

    fun update(event: AnActionEvent) {

        Locator(event).projectSetup?.let { projectSetup ->
            event.presentation.isVisible = true
            event.presentation.isEnabled = false
            for (editor in FileEditorManager.getInstance(projectSetup.project).selectedEditors) {
                if (editor is ProcessEditor) {
                    event.presentation.isEnabled = true
                }
            }
            return
        }

        event.presentation.isVisible = false
        event.presentation.isEnabled = false
    }
}

class GridLines : ToggleAction(), CanvasAction {

    override fun isSelected(event: AnActionEvent): Boolean {
        return !MdwSettings.instance.isHideCanvasGridLines
    }

    override fun setSelected(event: AnActionEvent, state: Boolean) {
        Locator(event).projectSetup?.let { projectSetup ->
            MdwSettings.instance.isHideCanvasGridLines = !state
            for (editor in FileEditorManager.getInstance(projectSetup.project).selectedEditors) {
                if (editor is ProcessEditor) {
                    editor.canvas.isShowGrid = !state
                }
            }
            return
        }
    }

    override fun update(event: AnActionEvent) {
        super<CanvasAction>.update(event)
    }
}

class ZoomIn : AnAction(), CanvasAction {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            var zoom = MdwSettings.instance.canvasZoom + ZOOM_INT
            if (zoom > ZOOM_MAX)
                zoom = ZOOM_MAX
            MdwSettings.instance.canvasZoom = zoom
            for (editor in FileEditorManager.getInstance(projectSetup.project).selectedEditors) {
                if (editor is ProcessEditor) {
                    editor.canvas.zoom = zoom
                }
            }
            return
        }
    }

    override fun update(event: AnActionEvent) {
        super<CanvasAction>.update(event)
    }

    companion object {
        const val ZOOM_INT = 20
        const val ZOOM_MAX = 200
    }
}

class ZoomOut : AnAction(), CanvasAction {

    override fun actionPerformed(event: AnActionEvent) {
        Locator(event).projectSetup?.let { projectSetup ->
            var zoom = MdwSettings.instance.canvasZoom - ZOOM_INT
            if (zoom < ZOOM_MIN)
                zoom = ZOOM_MIN
            MdwSettings.instance.canvasZoom = zoom
            for (editor in FileEditorManager.getInstance(projectSetup.project).selectedEditors) {
                if (editor is ProcessEditor) {
                    editor.canvas.zoom = zoom
                }
            }
            return
        }
    }

    override fun update(event: AnActionEvent) {
        super<CanvasAction>.update(event)
    }

    companion object {
        const val ZOOM_INT = 20
        const val ZOOM_MIN = 20
    }
}