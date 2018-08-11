package com.centurylink.mdw.studio.file

import com.intellij.json.JsonFileType
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.util.IconLoader
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile

class FileTypeFactory : com.intellij.openapi.fileTypes.FileTypeFactory() {

    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(ProcessFileType, "proc")
        consumer.consume(TaskFileType, "task")
    }
}

object ProcessFileType : JsonFileType() {

    override fun getDefaultExtension() = "proc"
    override fun getName() = "Process"
    override fun getDescription() = "Workflow Process"
    override fun getIcon() = Icons.PROCESS
    override fun isReadOnly() = false
    override fun getCharset(file: VirtualFile, content: ByteArray) = CharsetToolkit.UTF8

}

object TaskFileType : JsonFileType() {

    override fun getDefaultExtension() = "task"
    override fun getName() = "Task"
    override fun getDescription() = "Workflow Task"
    override fun getIcon() = Icons.TASK
    override fun isReadOnly() = false
    override fun getCharset(file: VirtualFile, content: ByteArray) = CharsetToolkit.UTF8
}