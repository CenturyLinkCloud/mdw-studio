package com.centurylink.mdw.studio.proc

import com.intellij.ide.CopyProvider
import com.intellij.ide.CutProvider
import com.intellij.ide.DeleteProvider
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.DataContext

class CanvasActionProvider : DeleteProvider, CutProvider, CopyProvider, PasteProvider {

    override fun canDeleteElement(dataContext: DataContext): Boolean {
        return true
    }
    override fun deleteElement(dataContext: DataContext) {
        println("DELETE")
    }

    override fun isCutVisible(dataContext: DataContext): Boolean {
        return true
    }
    override fun isCutEnabled(dataContext: DataContext): Boolean {
        return true
    }
    override fun performCut(dataContext: DataContext) {
        println("CUT")
    }

    override fun isCopyVisible(dataContext: DataContext): Boolean {
        return true
    }
    override fun isCopyEnabled(dataContext: DataContext): Boolean {
        return true
    }
    override fun performCopy(dataContext: DataContext) {
        println("COPY")
    }

    override fun isPastePossible(dataContext: DataContext): Boolean {
        return true
    }
    override fun isPasteEnabled(dataContext: DataContext): Boolean {
        return true
    }
    override fun performPaste(dataContext: DataContext) {
        println("PASTE")
    }
}