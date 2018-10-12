package com.centurylink.mdw.studio.file

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import javax.swing.ImageIcon

class Icons {
    companion object {
        val MDW = IconLoader.getIcon("/icons/mdw.png")
        val MDWDLG = IconLoader.getIcon("/icons/mdwdlg.png")
        val PROCESS = IconLoader.getIcon("/icons/process.gif")
        val TASK = IconLoader.getIcon("/icons/task.gif")
        val IMPL = IconLoader.getIcon("/icons/impl.gif")
        val SPRING = IconLoader.getIcon("/icons/spring.png")
        val EXCEL = IconLoader.getIcon("/icons/excel.gif")
        val KOTLIN = IconLoader.getIcon("/icons/kotlin_file.png")
        val JAVA = AllIcons.FileTypes.Java

        fun readIcon(path: String): ImageIcon {
            val classLoader = this::class.java.classLoader
            classLoader.getResourceAsStream(path).use {
                val bytes = ByteArray(it.available())
                it.read(bytes)
                return ImageIcon(bytes)
            }
        }
    }
}