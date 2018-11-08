package com.centurylink.mdw.studio

import com.intellij.ide.util.PropertiesComponent
import java.io.File
import java.nio.file.Files

class MdwSettings {

    var mdwHome: String
        get() = PropertiesComponent.getInstance().getValue(MDW_HOME, System.getenv("MDW_HOME") ?: "")
        set(value) {
            PropertiesComponent.getInstance().setValue(MDW_HOME, value)
            System.setProperty("mdw.home", value)
        }

    fun getOrMakeMdwHome(): File {
        var mdwHome: String? = System.getenv("MDW_HOME")
        if (mdwHome == null) {
            mdwHome = System.getProperty("mdw.home")
        }
        if (mdwHome == null) {
            // create under temp loc
            mdwHome = Files.createTempDirectory("mdw.studio").toString()
            System.setProperty("mdw.home", mdwHome)
        }
        return File(mdwHome)
    }

    var isHideCanvasGridLines: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(HIDE_CANVAS_GRIDLINES, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(HIDE_CANVAS_GRIDLINES, value)
        }

    var canvasZoom: Int
        get() {
            return PropertiesComponent.getInstance().getInt(CANVAS_ZOOM, 100)
        }
        set(value) {
            PropertiesComponent.getInstance().setValue(CANVAS_ZOOM, value.toString())
        }

    var isSyncDynamicJavaClassName: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(SYNC_DYNAMIC_JAVA_CLASS_NAME, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(SYNC_DYNAMIC_JAVA_CLASS_NAME, value)
        }

    var isOpenAttributeContentInEditorTab: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(OPEN_ATTRIBUTE_CONTENT_IN_EDITOR_TAB, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(OPEN_ATTRIBUTE_CONTENT_IN_EDITOR_TAB, value)
        }

    var isCreateAndAssociateTaskTemplate: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(CREATE_AND_ASSOCIATE_TASK_TEMPLATE, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(CREATE_AND_ASSOCIATE_TASK_TEMPLATE, value)
        }


    companion object {
        val instance = MdwSettings()
        const val ID = "com.centurylink.mdw.studio"
        private const val MDW_HOME = "$ID.mdwHome"

        // canvas
        private const val CANVAS_ZOOM = "$ID.canvasZoom"
        private const val HIDE_CANVAS_GRIDLINES = "$ID.isCanvasGridLines"

        // editing
        private const val SYNC_DYNAMIC_JAVA_CLASS_NAME = "$ID.isSyncDynamicJavaClassName"
        private const val OPEN_ATTRIBUTE_CONTENT_IN_EDITOR_TAB = "$ID.isOpenAttributeContentInContentInEditorTab"
        private const val CREATE_AND_ASSOCIATE_TASK_TEMPLATE = "$ID.createAndAssociateTaskTemplate"
    }
}