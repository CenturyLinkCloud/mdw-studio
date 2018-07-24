package com.centurylink.mdw.studio.config.widgets

import java.awt.Component
import javax.swing.AbstractCellEditor
import javax.swing.JTable
import javax.swing.table.TableCellEditor

class AssetCellEditor : AbstractCellEditor(), TableCellEditor {


    override fun getTableCellEditorComponent(table: JTable?, value: Any?, isSelected: Boolean, row: Int, column: Int): Component {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCellEditorValue(): Any {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }


}