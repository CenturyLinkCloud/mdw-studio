package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.annotations.Activity
import com.centurylink.mdw.model.project.Data
import com.centurylink.mdw.model.workflow.ActivityImplementor
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.psi.*
import com.intellij.psi.impl.PsiExpressionEvaluator
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTypesUtil
import org.json.JSONObject
import java.io.FileNotFoundException
import java.util.*

class Implementors(val projectSetup : ProjectSetup) : LinkedHashMap<String,ActivityImplementor>() {

    init {
        for (implAsset in projectSetup.findAssetsOfType("impl")) {
            add(ActivityImplementor(JSONObject(String(implAsset.contents))))
        }
        val javaPsiFacade = JavaPsiFacade.getInstance(projectSetup.project)
        val annotationPsi = javaPsiFacade.findClass(Activity::class.java.name, GlobalSearchScope.allScope(projectSetup.project))
        if (annotationPsi != null) {
            for ((psiClass, psiAnnotation) in projectSetup.findAnnotatedSource(annotationPsi)) {
                val implClass = psiClass.qualifiedName
                if (implClass != null && psiClass.language.id != "Groovy") {
                    getImpl(projectSetup, implClass, psiAnnotation)?.let { add(it) }
                }
            }
        }
        for (pseudoImpl in Data.Implementors.PSEUDO_IMPLEMENTORS) {
            if (!pseudoImpl.icon.startsWith("shape:")) {
                pseudoImpl.imageIcon = projectSetup.getIconAsset(pseudoImpl.icon)
            }
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

        const val ACTIVITY_IMPLEMENTOR = "mdwActivityImplementor"
        val IMPLEMENTOR_DATA_KEY = DataKey.create<ActivityImplementor>(ACTIVITY_IMPLEMENTOR)

        /**
         * Get implementor from annotated asset (null if not found or missing attributes)
         */
        fun getImpl(projectSetup: ProjectSetup, implClass: String, psiAnnotation: PsiAnnotation): ActivityImplementor? {
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
                val value = PsiExpressionEvaluator().computeConstantExpression(it as PsiElement, true) as String
                if (value.startsWith("{")) {
                    value
                }
                else {
                    val pageletAssetPath = if (value.indexOf("/") > 0) {
                        value
                    } else {
                        implClass.substring(0, implClass.lastIndexOf(".")) + "/$value"
                    }
                    val pageletAssetFile = projectSetup.getAssetFile(pageletAssetPath)
                    pageletAssetFile ?: throw FileNotFoundException("No pagelet asset: " + pageletAssetPath)
                    String(pageletAssetFile.contentsToByteArray())
                }
            }
            if (label != null) {
                return ActivityImplementor(implClass, category, label, icon, pagelet)
            }
            return null
        }
    }
}

interface ImplementorChangeListener {
    fun onChange(implementors: Implementors)
}