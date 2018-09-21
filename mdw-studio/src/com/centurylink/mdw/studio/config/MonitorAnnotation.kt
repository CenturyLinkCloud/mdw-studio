package com.centurylink.mdw.studio.config

import com.centurylink.mdw.studio.file.Asset
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClassObjectAccessExpression
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.util.PsiTypesUtil
import org.json.JSONArray

/**
 * Model class for Monitor annotation values.
 */
class MonitorAnnotation(private val psiAnnotation: PsiAnnotation, private val srcAsset: Asset) {

    val category: String?
        get() {
            return psiAnnotation.findAttributeValue("category")?.let {
                PsiTypesUtil.getPsiClass((it as PsiClassObjectAccessExpression).operand.type)?.qualifiedName
            }
        }

    val isDefaultEnabled: Boolean
        get() {
            val defaultEnabled = psiAnnotation.findAttributeValue("defaultEnabled")
            return if (defaultEnabled == null) false else (defaultEnabled as PsiLiteralExpression).value == true
        }

    val name: String
        get() {
            val name = psiAnnotation.findAttributeValue("value")
            return if (name == null) "" else (name as PsiLiteralExpression).value as String
        }

    val assetPath: String
        get() = srcAsset.path

    val defaultOptions: String
        get() {
            val defaultOptions = psiAnnotation.findAttributeValue("defaultOptions")
            return if (defaultOptions == null) "" else (defaultOptions as PsiLiteralExpression).value as String
        }

    val defaultAttributeValue: JSONArray
        get() {
            val cols = JSONArray()
            cols.put(isDefaultEnabled.toString())
            cols.put(name)
            cols.put(assetPath)
            cols.put(defaultOptions)
            return cols
        }
}