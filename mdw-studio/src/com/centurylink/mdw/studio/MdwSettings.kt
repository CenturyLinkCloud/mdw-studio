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

    var isSyncDynamicJavaClassName: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(SYNC_DYNAMIC_JAVA_CLASS_NAME, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(SYNC_DYNAMIC_JAVA_CLASS_NAME, value)
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
        private const val SYNC_DYNAMIC_JAVA_CLASS_NAME = "$ID.isSyncDynamicJavaClassName"
        private const val CREATE_AND_ASSOCIATE_TASK_TEMPLATE = "$ID.createAndAssociateTaskTemplate"
    }
}