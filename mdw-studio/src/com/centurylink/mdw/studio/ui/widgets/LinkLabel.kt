package com.centurylink.mdw.studio.ui.widgets

import javax.swing.Icon

typealias LinkClickListener = () -> Unit

open class LinkLabel(label: String, icon: Icon? = null) :
        com.intellij.ui.components.labels.LinkLabel<Any?>(label, icon) {

    var clickListener: LinkClickListener? = null

    init {
        setListener({ _, _ ->
            clickListener?.invoke()
        }, null)
    }
}

