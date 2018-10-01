package com.centurylink.mdw.studio.file.kotlin

import com.centurylink.mdw.studio.file.AttributeVirtualFile
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope
import com.intellij.openapi.module.impl.scopes.ModuleWithDependenciesScope.*
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.psi.ResolveScopeEnlarger
import com.intellij.psi.ResolveScopeProvider
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.UseScopeEnlarger
import com.intellij.psi.util.PsiTreeUtil

class KtsResolveScopeEnlarger : ResolveScopeEnlarger() {
    override fun getAdditionalResolveScope(file: VirtualFile, project: Project?): SearchScope? {
        return if (file is AttributeVirtualFile && file.getExt() == ".kts") {
            project?.let { proj ->
                val projectSetup = proj.getComponent(ProjectSetup::class.java)
                ModuleUtilCore.findModuleForFile(projectSetup.assetDir, proj)?.let { mod ->
                    ModuleWithDependenciesScope(mod, COMPILE_ONLY or MODULES or LIBRARIES)
                }
            }
        }
        else {
            null
        }
    }
}

class KtsUseScopeEnlarger : UseScopeEnlarger() {
    override fun getAdditionalUseScope(element: PsiElement): SearchScope? {
        val file = element.containingFile?.virtualFile
        return if (file is AttributeVirtualFile && file.getExt() == ".kts") {
            ProjectManager.getInstance().getOpenProjects()[0]?.let { proj ->
                val projectSetup = proj.getComponent(ProjectSetup::class.java)
                // TODO
                null
            }
        }
        else {
            null
        }
    }
}