package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.default
import com.centurylink.mdw.model.asset.Pagelet.Widget

/**
 * Table with custom row entry dialog.
 */
@Suppress("unused")
class CustomTable(widget: Widget, customWidget: Widget? = null) :
        Table(widget, true, true) {

    init {
        customWidget?.let { custom ->
            custom.attributes["searchUrl"]?.let { _ ->
                rowCreationHandler = { _ ->
                    val searchResult = SearchDialog(custom).showAndGet()
                    if (searchResult == null) {
                        null
                    }
                    else {
                        val row = mutableListOf<String>()
                        for (tableWidget in widget.widgets) {
                            val path = tableWidget.attributes["path"]
                            row.add(when {
                                path != null -> searchResult.evalPath(path)
                                tableWidget.default != null -> tableWidget.default!!
                                else -> ""
                            })
                        }
                        row.toTypedArray()
                    }
                }
            }
        }
    }
}