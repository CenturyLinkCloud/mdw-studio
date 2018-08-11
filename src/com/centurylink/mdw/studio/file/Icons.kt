package com.centurylink.mdw.studio.file

import com.intellij.openapi.util.IconLoader
import java.io.File
import java.nio.file.Files
import javax.swing.ImageIcon

class Icons {
    companion object {
        val PROCESS = IconLoader.getIcon("/icons/process.gif")
        val TASK = IconLoader.getIcon("/icons/task.gif")

        fun readIcon(path: String): ImageIcon {
            val classLoader = this::class.java.classLoader
            val file = File(classLoader.getResource(path).file)
            val bytes = Files.readAllBytes(file.toPath())
            return ImageIcon(bytes)
        }
    }
}