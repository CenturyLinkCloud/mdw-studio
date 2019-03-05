package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.draw.edit.JsonValue
import com.centurylink.mdw.model.asset.Pagelet.Widget
import org.json.JSONObject
import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellEditor
import javax.swing.table.TableCellRenderer

class DialogCell(label: String, val widget: Widget, val jsonValue: JsonValue) : LinkCell(label) {

    init {
        linkLabel.clickListener = {
            widget.value = jsonValue.json.toString()
            val newValue = Dialog(widget).showAndGet(jsonValue)
            // TODO handle dialog updates
        }
    }
}

class DialogCellRenderer(private val widget: Widget) : TableCellRenderer, Hoverable {

    var dialogCell: DialogCell? = null

    override fun getTableCellRendererComponent(table: JTable, value: Any?,
            isSelected: Boolean, hasFocus: Boolean, row: Int, column: Int): Component {
        var link = widget.attributes["link"] ?: ""
        val jsonValue = JsonValue(JSONObject(value?.toString() ?: "{}"), widget.name)
        if (JsonValue.isPath(link)) {
            link = jsonValue.evalPath(link)
        }
        val dialogCell = DialogCell(link, widget, jsonValue)
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
        val jsonValue = JsonValue(JSONObject(this.value), widget.name)
        if (JsonValue.isPath(link)) {
            link = jsonValue.evalPath(link)
        }
        return DialogCell(link, widget, jsonValue)
    }

    override fun getCellEditorValue(): Any? {
        return value
    }
}
