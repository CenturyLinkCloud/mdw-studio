package com.centurylink.mdw.studio.action

import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.centurylink.mdw.studio.file.AssetOpener
import com.centurylink.mdw.studio.proj.Implementors
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.OpenSourceUtil

class ImplementorSource : AnAction("Go To Implementor") {

    override fun actionPerformed(event: AnActionEvent) {
        getProject(event)?.let { project ->
            project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                getImplementor(event)?.let { implementor ->
                    val implClass = implementor.implementorClass
                    val lastDot = implClass.lastIndexOf('.')
                    val assetPath = implClass.substring(0, lastDot) + '/' + implClass.substring(lastDot + 1)
                    projectSetup.getAsset("$assetPath.kt")?.let { ktAsset ->
                        AssetOpener(projectSetup, ktAsset).doOpen()
                        return
                    }
                    projectSetup.getAsset("$assetPath.java")?.let { javaAsset ->
                        AssetOpener(projectSetup, javaAsset).doOpen()
                        return
                    }
                    val scope = GlobalSearchScope.allScope(projectSetup.project)
                    val psiFacade = JavaPsiFacade.getInstance(projectSetup.project)
                    val psiClass = psiFacade.findClass(implClass, scope)
                    if (psiClass != null) {
                        OpenSourceUtil.navigate(psiClass)
                    }
                }
            }
        }
    }

    override fun update(event: AnActionEvent) {
        val applicable = getImplementor(event) != null && getProject(event) != null
        event.presentation.isVisible = applicable
        event.presentation.isEnabled = applicable
    }

    private fun getImplementor(event: AnActionEvent): ActivityImplementor? {
        return Implementors.IMPLEMENTOR_DATA_KEY.getData(event.dataContext)
    }

    private fun getProject(event: AnActionEvent): Project? {
        return event.getData(CommonDataKeys.PROJECT)
    }
}