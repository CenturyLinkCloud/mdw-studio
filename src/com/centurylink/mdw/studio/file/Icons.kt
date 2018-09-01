package com.centurylink.mdw.studio.file

import com.intellij.openapi.util.IconLoader
import java.io.File
import java.nio.file.Files
import javax.swing.ImageIcon

class Icons {
    companion object {
        val MDW = IconLoader.getIcon("/icons/mdw.png")
        val PROCESS = IconLoader.getIcon("/icons/process.gif")
        val TASK = IconLoader.getIcon("/icons/task.gif")
        val IMPL = IconLoader.getIcon("/icons/impl.gif")
        val SPRING = IconLoader.getIcon("/icons/spring.png")
        val EXCEL = IconLoader.getIcon("/icons/excel.gif")

        fun readIcon(path: String): ImageIcon {
            val classLoader = this::class.java.classLoader
            val file = File(classLoader.getResource(path).file)
            val bytes = Files.readAllBytes(file.toPath())
            return ImageIcon(bytes)
        }
    }
}