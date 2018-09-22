package com.centurylink.mdw.studio

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ui.components.CheckBox
import javax.swing.JPanel

class MdwSettings {

    var isSyncDynamicJavaClassName: Boolean
        get() = PropertiesComponent.getInstance().getBoolean(SYNC_DYNAMIC_JAVA_CLASS_NAME, false)
        set(value) {
            PropertiesComponent.getInstance().setValue(SYNC_DYNAMIC_JAVA_CLASS_NAME, value)
        }

    companion object {
        val instance = MdwSettings()
        const val ID = "com.centurylink.mdw.studio"
        private const val SYNC_DYNAMIC_JAVA_CLASS_NAME = "$ID.isSyncDynamicJavaClassName"
    }
}