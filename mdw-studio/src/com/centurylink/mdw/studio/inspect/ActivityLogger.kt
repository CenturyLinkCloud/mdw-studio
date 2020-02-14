package com.centurylink.mdw.studio.inspect

import com.centurylink.mdw.model.project.Data
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType

class ActivityLogger : LocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return ActivityVisitor(holder)
    }
}

class ActivityVisitor(private val problemsHolder : ProblemsHolder) : JavaElementVisitor() {

    override fun visitField(field: PsiField) {
        super.visitField(field)
        if (field.parent is PsiClass && isActivityImpl(field.parent as PsiClass)) {
            if (field.type is PsiClassReferenceType) {
                if ((field.type as PsiClassReferenceType).reference.qualifiedName == "com.centurylink.mdw.util.log.StandardLogger") {
                    problemsHolder.registerProblem(field.nameIdentifier, "Activity logging bypassed by local logger (see ${Data.DOCS_URL}/help/implementor.html#logging)",
                            ProblemHighlightType.ERROR, null as LocalQuickFix?)
                }
            }
        }
    }

    private fun isActivityImpl(psiClass: PsiClass): Boolean {
        psiClass.extendsList?.let { psiRefList ->
            for (extRef in psiRefList.referenceElements) {
                if ("com.centurylink.mdw.workflow.activity.DefaultActivityImpl" == extRef.qualifiedName) {
                    return true;
                }
            }

        }
        return false
    }
}
