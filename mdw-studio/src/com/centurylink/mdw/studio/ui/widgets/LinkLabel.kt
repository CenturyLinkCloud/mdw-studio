package com.centurylink.mdw.studio.ui.widgets

import java.awt.Color
import javax.swing.Icon

typealias LinkClickListener = () -> Unit

open class LinkLabel(label: String, icon: Icon? = null) :
        com.intellij.ui.components.labels.LinkLabel<Any?>(label, icon) {

    var clickListener: LinkClickListener? = null
    var linkColor: Color? = null

    init {
        setListener({ _, _ ->
            clickListener?.invoke()
        }, null)
    }

    override fun getNormal(): Color {
        return linkColor ?: super.getNormal()
    }
}

