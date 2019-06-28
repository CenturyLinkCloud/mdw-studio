package com.centurylink.mdw.studio.file

import com.intellij.ide.highlighter.XmlLikeFileType
import com.intellij.json.JsonFileType
import com.intellij.lang.xml.XMLLanguage
import com.intellij.openapi.fileTypes.FileTypeConsumer
import com.intellij.openapi.fileTypes.INativeFileType
import com.intellij.openapi.fileTypes.NativeFileType.openAssociatedApplication
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.CharsetToolkit
import com.intellij.openapi.vfs.VirtualFile

class FileTypeFactory : com.intellij.openapi.fileTypes.FileTypeFactory() {

    override fun createFileTypes(consumer: FileTypeConsumer) {
        consumer.consume(ProcessFileType, "proc")
        consumer.consume(TaskFileType, "task")
        consumer.consume(ImplFileType, "impl")
        consumer.consume(SpringFileType, "spring")
        consumer.consume(ExcelFileType, "xlsx")
        consumer.consume(EventHandlerFileType, "evth")
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

object ImplFileType : JsonFileType() {
    override fun getDefaultExtension() = "impl"
    override fun getName() = "Implementor"
    override fun getDescription() = "Activity Implementor"
    override fun getIcon() = Icons.IMPL
    override fun isReadOnly() = false
    override fun getCharset(file: VirtualFile, content: ByteArray) = CharsetToolkit.UTF8
}

object SpringFileType : XmlLikeFileType(XMLLanguage.INSTANCE) {
    override fun getDefaultExtension() = "spring"
    override fun getName() = "Spring"
    override fun getDescription() = "Spring Config"
    override fun getIcon() = Icons.SPRING
    override fun isReadOnly() = false
    override fun getCharset(file: VirtualFile, content: ByteArray) = CharsetToolkit.UTF8
}

object ExcelFileType : INativeFileType {
    override fun getDefaultExtension() = "xlsx"
    override fun getName() = "Excel"
    override fun getDescription() = "Excel Decision Table"
    override fun getIcon() = Icons.EXCEL
    override fun isReadOnly() = false
    override fun getCharset(file: VirtualFile, content: ByteArray) : String? { return null }
    override fun isBinary(): Boolean = true
    override fun useNativeIcon() = false
    override fun openFileInAssociatedApplication(project: Project?, file: VirtualFile): Boolean {
        return openAssociatedApplication(file)
    }
}

object EventHandlerFileType : JsonFileType() {
    override fun getDefaultExtension() = "evth"
    override fun getName() = "Event"
    override fun getDescription() = "Event Handler"
    override fun getIcon() = Icons.EVENT
    override fun isReadOnly() = false
    override fun getCharset(file: VirtualFile, content: ByteArray) = CharsetToolkit.UTF8
}
