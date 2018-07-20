package com.centurylink.mdw.studio.file

import java.io.File
import java.nio.file.Files
import javax.swing.ImageIcon

class Icons {
    companion object {
        fun readIcon(path: String): ImageIcon {
            val classLoader = this::class.java.classLoader
            val file = File(classLoader.getResource(path).file)
            val bytes = Files.readAllBytes(file.toPath())
            return ImageIcon(bytes)
        }

    }
}