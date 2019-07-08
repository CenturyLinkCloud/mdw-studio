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
        if (queryParameters.scopeDeterminedByUser is GlobalSearchScope) {
            val element = queryParameters.elementToSearch
            if (element.containingFile.virtualFile !is AttributeVirtualFile) {
                element.project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                    AttributeVirtualFileSystem.instance.virtualFiles.values.forEach { file ->
                        file.psiFile?.let { psiFile ->
                            val fileScope = LocalSearchScope(psiFile)
                            ReferencesSearch.searchOptimized(element, fileScope, true, queryParameters.optimizer, consumer)
                        }
                    }
                }
            }
        }
    }
}

class AttributeMethodUsagesSearch : QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters>(true) {
    override fun processQuery(queryParameters: MethodReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        if (queryParameters.scopeDeterminedByUser is GlobalSearchScope) {
            val method = queryParameters.method
            if (method.containingFile.virtualFile !is AttributeVirtualFile) {
                method.project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                    AttributeVirtualFileSystem.instance.virtualFiles.values.forEach { file ->
                        file.psiFile?.let { psiFile ->
                            val fileScope = LocalSearchScope(psiFile)
                            ReferencesSearch.searchOptimized(method, fileScope, false, queryParameters.optimizer, consumer)
                        }
                    }
                }
            }
        }
    }
}