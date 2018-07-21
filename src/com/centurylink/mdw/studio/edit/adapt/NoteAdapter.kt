package com.centurylink.mdw.studio.edit.adapt

import com.centurylink.mdw.model.asset.Pagelet.Widget
import com.centurylink.mdw.studio.edit.apply.WidgetApplier
import com.centurylink.mdw.studio.edit.label

/**
 * Note widgets only have a label
 */
class NoteAdapter(applier: WidgetApplier) : WidgetAdapter(applier) {
    override fun didInit(widget: Widget) {
        widget.value = widget.label
        widget.label = ""
    }
}