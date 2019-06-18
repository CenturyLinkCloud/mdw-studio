package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.studio.file.Icons
import com.centurylink.mdw.studio.ui.IconButton
import com.intellij.icons.AllIcons
import java.awt.FlowLayout
import javax.swing.JLabel
import javax.swing.JOptionPane
import javax.swing.JPanel

/**
 * The updateListener should return whether the value is an expression
 */
class ExpressionEntry(label: String, private var value: String?,
          private val updateListener: (value: String?) -> Boolean) :
        JPanel(FlowLayout(FlowLayout.CENTER, 0, 5)) {

    private fun indicatorText(isExpression: Boolean) = if (isExpression) "*" else ""

    init {
        isOpaque = false
        val expressionIndicator = JLabel(indicatorText(updateListener(value)))

        val iconBtn = IconButton(AllIcons.Debugger.EvaluateExpression, "Expression...") {
            val result = JOptionPane.showInputDialog(this@ExpressionEntry,
                    "Enter an expression for $label:", "Expression",
                    JOptionPane.PLAIN_MESSAGE, Icons.MDWDLG, null,  value)
            result?.let {
                value = result.toString()
                val isExpression = updateListener.invoke(result.toString())
                expressionIndicator.text = indicatorText(isExpression)
            }
        }
        add(iconBtn)
        add(expressionIndicator)
    }
}