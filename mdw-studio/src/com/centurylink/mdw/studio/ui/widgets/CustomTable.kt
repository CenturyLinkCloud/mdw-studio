package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.default
import com.centurylink.mdw.model.asset.Pagelet

/**
 * Table with custom row entry dialog.
 */
@Suppress("unused")
class CustomTable(widget: Pagelet.Widget, customWidget: Pagelet.Widget? = null) :
        Table(widget, true, true) {

    init {
        customWidget?.let { custom ->
            custom.attributes["searchUrl"]?.let { _ ->
                rowCreationHandler = { _ ->
                    val searchDialog = SearchDialog(projectSetup, custom)
                    if (searchDialog.showAndGet()) {
                        searchDialog.selectedResult?.let { searchResult ->
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
                    else {
                        null
                    }
                }
            }
        }
    }
}