package com.centurylink.mdw.studio.file

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.util.Processor

class AttributeReferencesSearcher : QueryExecutorBase<PsiReference,ReferencesSearch.SearchParameters>(true) {

    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val scope = queryParameters.scopeDeterminedByUser
        val element = queryParameters.elementToSearch
        val containingFile = element.containingFile
        if (scope is GlobalSearchScope || containingFile.virtualFile is AttributeVirtualFile) {
            val currentScope: LocalSearchScope?
            if (scope is LocalSearchScope) {
                if (queryParameters.isIgnoreAccessScope) {
                    return
                }
                currentScope = scope
            } else {
                currentScope = null
            }
            println("ELEMENT: " + element)
            element.project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                AttributeVirtualFileSystem.instance.virtualFiles.values.forEach { file ->
                    file.psiFile?.let { psiFile ->
                        val fileScope = LocalSearchScope(psiFile)
                        val searchScope = if (currentScope == null) fileScope else fileScope.intersectWith(currentScope)
                        ReferencesSearch.searchOptimized(element, searchScope, true, queryParameters.optimizer, consumer)
                    }
                }
            }
        }
    }
}

class AttributeMethodUsagesSearch : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val scope = queryParameters.scopeDeterminedByUser
        val method = queryParameters.method
        val containingFile = method.containingFile
        if (scope is GlobalSearchScope || containingFile.virtualFile is AttributeVirtualFile) {
            val currentScope: LocalSearchScope?
            if (scope is LocalSearchScope) {
//                if (queryParameters.isIgnoreAccessScope) {
//                    return
//                }
                currentScope = scope
            } else {
                currentScope = null
            }
            println("METHOD: " + method)
            method.project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                AttributeVirtualFileSystem.instance.virtualFiles.values.forEach { file ->
                    file.psiFile?.let { psiFile ->
                        val fileScope = LocalSearchScope(psiFile)
                        val searchScope = if (currentScope == null) fileScope else fileScope.intersectWith(currentScope)
                        ReferencesSearch.searchOptimized(method, searchScope, true, queryParameters.optimizer, consumer)
                    }
                }
            }
        }
//    }
    }
}