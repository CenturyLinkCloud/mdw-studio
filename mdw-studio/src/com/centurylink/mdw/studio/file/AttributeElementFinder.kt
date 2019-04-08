package com.centurylink.mdw.studio.file

import com.centurylink.mdw.model.workflow.Process
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassOwner
import com.intellij.psi.PsiElementFinder
import com.intellij.psi.PsiJavaFile
import com.intellij.psi.search.GlobalSearchScope
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
                    projectSetup.findAssetFromNormalizedName(pkg, cls.substring(0, underscore), "proc")?.let { asset ->
                        val activityId = cls.substring(underscore + 1)
                        val process = Process(JSONObject(String(asset.contents)))
                        process.name = asset.rootName
                        process.packageName = pkg
                        process.id = asset.id
                        process.activities.find { it.logicalId == activityId }?.let { activity ->
                            activity.getAttribute("Java")?.let { _ ->
                                AttributeVirtualFileSystem.instance.findFileByPath("$pkg/$cls.java", project)?.let { file ->
                                    val attributeVirtualFile = file as AttributeVirtualFile
                                    // attributeVirtualFile.workflowObj.isReadOnly = true // https://github.com/CenturyLinkCloud/mdw-studio/issues/72
                                    attributeVirtualFile.psiFile?.let { psiFile ->
                                        if (psiFile is PsiJavaFile && psiFile.classes.isNotEmpty()) {
                                            return psiFile.classes[0]
                                        }
                                    }
                                }
                            }
                            activity.getAttribute("Rule")?.let { _ ->
                                activity.getAttribute("SCRIPT")?.let { scriptAttr ->
                                    AttributeVirtualFile.getScriptExt(scriptAttr).let { ext ->
                                        AttributeVirtualFileSystem.instance.findFileByPath("$pkg/$cls.$ext", project)?.let { file ->
                                            val attributeVirtualFile = file as AttributeVirtualFile
                                            // attributeVirtualFile.workflowObj.isReadOnly = true // https://github.com/CenturyLinkCloud/mdw-studio/issues/72
                                            attributeVirtualFile.psiFile?.let { psiFile ->
                                                if (psiFile is PsiClassOwner && psiFile.classes.isNotEmpty()) {
                                                    return psiFile.classes[0]
                                                }
                                            }
                                        }
                                    }
                                }
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