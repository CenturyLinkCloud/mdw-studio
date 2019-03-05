package com.centurylink.mdw.studio.ui.widgets

interface Hoverable {
    /**
     * Coords are relative to cell origin.
     * Returns whether the pointer cursor should be displayed.
     */
    fun isHover(x: Int, y: Int): Boolean
}