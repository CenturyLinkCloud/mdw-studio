package com.centurylink.mdw.studio.file

import com.intellij.openapi.vfs.newvfs.events.VFileContentChangeEvent
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent

class AssetEvent(fileEvent: VFileEvent, val asset: Asset) {

    enum class EventType {
        Create,
        Update,
        Delete,
        Unknown
    }


    val type: EventType
    init {
        type = when(fileEvent) {
            is VFileCreateEvent -> EventType.Create
            is VFileContentChangeEvent -> EventType.Update
            is VFileDeleteEvent -> EventType.Delete
            else -> EventType.Unknown
        }
    }

    override fun toString(): String {
        return "$type: ${asset.path}"
    }

    override fun equals(other: Any?): Boolean {
        return other is AssetEvent && (other as AssetEvent).toString() == toString()
    }
}