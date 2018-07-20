package com.centurylink.mdw.studio.file

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile

class FileTypeFactory : com.intellij.openapi.fileTypes.FileTypeFactory() {

    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(ProcessFileType, "proc")
    }
}

object ProcessFileType : FileType {

    val processIcon = IconLoader.getIcon("/icons/process.gif")

    override fun getDefaultExtension() = "proc"
    override fun getName() = "Process"
    override fun getDescription() = "Workflow Process"
    override fun getIcon() = processIcon
    override fun isBinary() = false
    override fun isReadOnly() = false
    override fun getCharset(file: VirtualFile, content: ByteArray) = CharsetToolkit.UTF8

}