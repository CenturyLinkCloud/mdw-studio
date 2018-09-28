package com.centurylink.mdw.studio.proc

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.listeners.RefactoringElementListener
import com.intellij.refactoring.listeners.RefactoringElementListenerProvider

class RefactorListenerProvider : RefactoringElementListenerProvider {
    override fun getListener(element: PsiElement): RefactoringElementListener? {
        return if (element is PsiFile && element.virtualFile.extension == "proc") {
            val processEditor = FileEditorManager.getInstance(element.project).allEditors.find { editor ->
                editor is ProcessEditor && editor.procFile == element.virtualFile
            }
            processEditor?.let { RefactorListener(it as ProcessEditor) }
        }
        else {
            null
        }
    }
}

class RefactorListener(val processEditor: ProcessEditor) : RefactoringElementListener {

    override fun elementRenamed(newElement: PsiElement) {
        val newName = (newElement as PsiFile).virtualFile.nameWithoutExtension
        processEditor.canvas.rename(newName)
    }

    override fun elementMoved(newElement: PsiElement) {
    }
}