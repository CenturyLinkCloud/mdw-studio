package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.annotations.Variable
import com.centurylink.mdw.dataaccess.MdwVariableTypes
import com.centurylink.mdw.model.variable.VariableType
import com.centurylink.mdw.translator.DocumentReferenceTranslator
import com.intellij.openapi.diagnostic.Logger
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.search.GlobalSearchScope
import java.util.*

class VariableTypes(val projectSetup: ProjectSetup) : LinkedHashMap<String,VariableType>() {

    init {
        // mdw built-in variable types
        val mdwVariableTypes = MdwVariableTypes()
        for (mdwVariableType in mdwVariableTypes.variableTypes) {
            add(mdwVariableType)
        }
        // annotated custom variable types
        val javaPsiFacade = JavaPsiFacade.getInstance(projectSetup.project)
        val annotationPsi = javaPsiFacade.findClass(Variable::class.java.name, GlobalSearchScope.allScope(projectSetup.project))
        if (annotationPsi != null) {
            for ((psiClass, psiAnnotation) in projectSetup.findAnnotatedSource(annotationPsi)) {
                val translatorClass = psiClass.qualifiedName
                if (translatorClass != null && psiClass.language.id != "Groovy") {
                    getVariableType(psiClass, psiAnnotation)?.let { add(it) }
                }
            }
        }
    }

    private fun add(variableType: VariableType) {
        put(variableType.name, variableType)
    }

    companion object {

        val LOG = Logger.getInstance(VariableTypes::class.java)

        /**
         * Get variable type from annotated asset (null if not found or missing type attribute)
         */
        fun getVariableType(translatorClass: PsiClass, psiAnnotation: PsiAnnotation): VariableType? {
            val type = psiAnnotation.findAttributeValue("type")?.let {
                (it as PsiLiteralExpression).value as String
            }
            if (type != null) {
                val javaPsiFacade = JavaPsiFacade.getInstance(translatorClass.project)
                val docRefTransClass = javaPsiFacade.findClass(DocumentReferenceTranslator::class.java.name,
                        GlobalSearchScope.allScope(translatorClass.project))
                val isDoc = docRefTransClass?.let { translatorClass.isInheritor(it, true) } ?: false
                return VariableType(type, translatorClass.qualifiedName, isDoc)
            }
            return null
        }
    }
}