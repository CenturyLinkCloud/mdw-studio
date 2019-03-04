package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.model.asset.Pagelet.Widget
import org.json.JSONObject
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class DialogCell(label: String, val widget: Widget) : LinkCell(label) {

    init {

    }
}

class DialogCellRenderer(private val widget: Widget) : TableCellRenderer, Hoverable {

    var dialogCell: DialogCell? = null

    override fun getTableCellRendererComponent(table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        var link = widget.attributes["link"] ?: ""
        if (JsonValue.isPath(link)) {
            link = JsonValue(JSONObject(value?.toString() ?: "{}"), widget.name).evalPath(link)
        }
        val dialogCell = DialogCell(link, widget)
        dialogCell.init(table, isSelected, hasFocus)
        this.dialogCell = dialogCell
        return dialogCell
    }

    override fun isHover(x: Int, y: Int): Boolean {
        return dialogCell?.isHover(x, y) == true
    }

}

class DialogCellEditor(private val widget: Widget) :  AbstractCellEditor(), TableCellEditor {

    private var link = ""
    private var value = "{}"

    override fun getTableCellEditorComponent(table: JTable, value: Any?, isSelected: Boolean, row: Int, column: Int):
            Component {
        this.value = value?.toString() ?: "{}"
        link = widget.attributes["link"] ?: ""
        if (JsonValue.isPath(link)) {
            link = JsonValue(JSONObject(this.value), widget.name).evalPath(link)
        }
        return DialogCell(link, widget)
    }

    override fun getCellEditorValue(): Any? {
        return value
    }
}
