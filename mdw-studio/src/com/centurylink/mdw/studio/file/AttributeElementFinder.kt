package com.centurylink.mdw.studio.file

import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.ide.highlighter.JavaFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.LocalTimeCounter
import org.json.JSONObject

class AttributeElementFinder(private val project: Project) : PsiElementFinder() {

    override fun findClass(qualifiedName: String, scope: GlobalSearchScope): PsiClass? {
        val lastDot = qualifiedName.lastIndexOf(".")
        if (lastDot > 0 && lastDot < qualifiedName.length - 1) {
            val pkg = qualifiedName.substring(0, lastDot)
            val cls = qualifiedName.substring(lastDot + 1)
            val underscore = cls.lastIndexOf("_")
            if (underscore > 0) {
                project.getComponent(ProjectSetup::class.java)?.let { projectSetup ->
                    projectSetup.getAsset("$pkg/${cls.substring(0, underscore)}.proc")?.let { asset ->
                        val activityId = cls.substring(underscore + 1)
                        val process = Process(JSONObject(String(asset.contents)))
                        process.name = asset.name
                        process.packageName = pkg
                        process.id = asset.id
                        process.activities.find{ it.logicalId == activityId }?.let { activity ->
                            activity.getAttribute("Java")?.let { java ->
                                val psiJavaFile = PsiFileFactory.getInstance(project).createFileFromText(cls, JavaFileType.INSTANCE, java, LocalTimeCounter.currentTime(), true) as PsiJavaFile
                                return psiJavaFile.classes[0]
                            }
                            activity.getAttribute("Rule")?.let { rule ->
                            }
                        }
                    }
                }
            }
        }
        return null
    }

    override fun findClasses(qualifiedName: String, scope: GlobalSearchScope): Array<PsiClass> {
        findClass(qualifiedName, scope)?.let {
            return arrayOf(it)
        }
        return arrayOf<PsiClass>()
    }
}