package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.annotations.Activity
import com.centurylink.mdw.draw.Impl
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.psi.*
import com.intellij.psi.impl.PsiExpressionEvaluator
import com.intellij.psi.util.PsiTypesUtil
import org.json.JSONObject
import java.util.*

class Implementors(val projectSetup : ProjectSetup) : LinkedHashMap<String, Impl>() {

    init {
        for (implAsset in projectSetup.findAssetsOfType("impl")) {
            add(Impl(implAsset.path, JSONObject(String(implAsset.contents))))
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
        for (pseudoImpl in Impl.PSEUDO_IMPLS) {
            pseudoImpl.icon = projectSetup.getIconAsset(pseudoImpl.iconName)
            add(pseudoImpl)
        }
    }

    private fun add(impl: Impl) {
        var iconAsset = impl.iconName
        if (iconAsset != null && !iconAsset.startsWith("shape:")) {
            var iconPkg = Impl.BASE_PKG
            val slash = iconAsset.lastIndexOf('/')
            if (slash > 0) {
                iconPkg = iconAsset.substring(0, slash)
                iconAsset = iconAsset.substring(slash + 1)
            }
            else {
                // find in impl package, if present
                impl.assetPath?.let {
                    val implAsset = projectSetup.getAsset(it)
                    if (implAsset != null) {
                        val pkgIconAsset = projectSetup.getAssetFile("${implAsset.pkg.name}/$iconAsset")
                        if (pkgIconAsset != null) {
                            iconPkg = implAsset.pkg.name
                        }
                    }
                }
            }
            impl.icon = projectSetup.getIconAsset("$iconPkg/$iconAsset")
        }
        put(impl.implementorClassName, impl)
    }

    fun toSortedList(): List<Impl> {
       return this.values.sortedBy { it.label }
    }

    companion object {
        /**
         * Find annotation-based implementors (return null if not found)
         */
        fun getImpl(psiFile: PsiFile): Impl? {
            if (psiFile is PsiClassOwner) {
                for (psiClass in psiFile.classes) {
                    AnnotationUtil.findAnnotation(psiClass, Activity::class.java.name)?.let { psiAnnotation ->
                        psiClass.qualifiedName?.let { implClass ->
                            val label = psiAnnotation.findAttributeValue("value")?.let {
                                (it as PsiLiteralExpression).value as String
                            }
                            val category = psiAnnotation.findAttributeValue("category")?.let {
                                PsiTypesUtil.getPsiClass((it as PsiClassObjectAccessExpression).type)?.qualifiedName
                            }
                            val icon = psiAnnotation.findAttributeValue("icon")?.let {
                                (it as PsiLiteralExpression).value as String
                            }
                            val pagelet = psiAnnotation.findAttributeValue("pagelet")?.let {
                                PsiExpressionEvaluator().computeConstantExpression(it as PsiElement, true) as String
                            }
                            if (label != null && category != null && icon != null) {
                                return Impl(category, label, icon, implClass, pagelet)
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