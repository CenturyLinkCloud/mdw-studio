package com.centurylink.mdw.studio.file

import com.intellij.openapi.vfs.newvfs.events.*

class AssetEvent(fileEvent: VFileEvent, val asset: Asset) {

    enum class EventType {
        Create,
        Copy,
        Update,
        Move,
        Delete,
        Unknown
    }


    val type: EventType
    init {
        type = when(fileEvent) {
            is VFileCreateEvent -> EventType.Create
            is VFileCopyEvent -> EventType.Copy
            is VFileContentChangeEvent -> EventType.Update
            is VFileMoveEvent -> EventType.Move
            is VFileDeleteEvent -> EventType.Delete
            else -> EventType.Unknown
        }
    }

    override fun toString(): String {
        return "$type: ${asset.path}"
    }

    override fun equals(other: Any?): Boolean {
        return other is AssetEvent && other.toString() == toString()
    }
}