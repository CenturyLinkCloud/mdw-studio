package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.annotations.Activity
import com.centurylink.mdw.draw.Step
import com.centurylink.mdw.draw.model.Data
import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.*
import com.intellij.psi.impl.PsiExpressionEvaluator
import com.intellij.psi.util.PsiTypesUtil
import org.json.JSONObject
import java.util.*

class Implementors(val projectSetup : ProjectSetup) : LinkedHashMap<String,ActivityImplementor>() {

    init {
        for (implAsset in projectSetup.findAssetsOfType("impl")) {
            add(ActivityImplementor(JSONObject(String(implAsset.contents))))
        }
        for (javaAsset in projectSetup.findAssetsOfType("java")) {
            PsiManager.getInstance(projectSetup.project).findFile(javaAsset.file)?.let { psiFile ->
                getImpl(psiFile)?.let { add(it) }
            }
        }
        for (ktAsset in projectSetup.findAssetsOfType("kt")) {
            PsiManager.getInstance(projectSetup.project).findFile(ktAsset.file)?.let { psiFile ->
                getImpl(psiFile)?.let { add(it) }
            }
        }
        for (pseudoImpl in Data.Implementors.PSEUDO_IMPLS) {
            pseudoImpl.imageIcon = projectSetup.getIconAsset(pseudoImpl.icon)
            add(pseudoImpl)
        }
    }

    private fun add(implementor: ActivityImplementor) {
        var iconAsset = implementor.icon
        if (iconAsset != null && !iconAsset.startsWith("shape:")) {
            var iconPkg = Data.BASE_PKG
            val slash = iconAsset.lastIndexOf('/')
            if (slash > 0) {
                iconPkg = iconAsset.substring(0, slash)
                iconAsset = iconAsset.substring(slash + 1)
            }
            else {
                // find in implementor package, if present
                val lastDot = implementor.implementorClass.lastIndexOf('.')
                if (lastDot > 0) {
                    val pkg = implementor.implementorClass.substring(0, lastDot)
                    projectSetup.getAssetFile("$pkg/$iconAsset")?.let {
                        iconPkg = pkg
                    }
                }
            }
            implementor.imageIcon = projectSetup.getIconAsset("$iconPkg/$iconAsset")
        }
        put(implementor.implementorClass, implementor)
    }

    fun toSortedList(): List<ActivityImplementor> {
       return this.values.sortedBy { it.label }
    }

    companion object {
        /**
         * Find annotation-based implementors (return null if not found)
         */
        fun getImpl(psiFile: PsiFile): ActivityImplementor? {
            if (psiFile is PsiClassOwner) {
                for (psiClass in psiFile.classes) {
                    AnnotationUtil.findAnnotation(psiClass, Activity::class.java.name)?.let { psiAnnotation ->
                        psiClass.qualifiedName?.let { implClass ->
                            val label = psiAnnotation.findAttributeValue("value")?.let {
                                (it as PsiLiteralExpression).value as String
                            }
                            val category = psiAnnotation.findAttributeValue("category")?.let {
                                PsiTypesUtil.getPsiClass((it as PsiClassObjectAccessExpression).operand.type)?.qualifiedName
                            }
                            val icon = psiAnnotation.findAttributeValue("icon")?.let {
                                (it as PsiLiteralExpression).value as String
                            }
                            val pagelet = psiAnnotation.findAttributeValue("pagelet")?.let {
                                PsiExpressionEvaluator().computeConstantExpression(it as PsiElement, true) as String
                            }
                            if (label != null && category != null && icon != null) {
                                return ActivityImplementor(implClass, category, label, icon, pagelet)
                            }
                        }
                    }
                }
            }
            return null
        }
    }
}

interface ImplementorChangeListener {
    fun onChange(implementors: Implementors)
}

/**
 * For asset-driven activities
 */
class AssociatedAsset(type: String) {

}

val Step.asset: AssociatedAsset?
    get() {
        val attr: String? = null
        if (implementor.category.endsWith("InvokeProcessActivity")) {

        }
        return null
    }