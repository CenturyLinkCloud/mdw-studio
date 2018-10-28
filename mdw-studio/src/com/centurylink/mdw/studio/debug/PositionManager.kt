package com.centurylink.mdw.studio.debug

import com.intellij.debugger.PositionManager
import com.intellij.debugger.SourcePosition
import com.intellij.debugger.engine.DebugProcess
import com.intellij.debugger.engine.DebugProcessImpl
import com.intellij.debugger.engine.PositionManagerImpl
import com.intellij.debugger.requests.ClassPrepareRequestor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.sun.jdi.Location
import com.sun.jdi.ReferenceType
import com.sun.jdi.request.ClassPrepareRequest

class PositionManagerFactory : com.intellij.debugger.PositionManagerFactory() {
    override fun createPositionManager(process: DebugProcess): PositionManager? {
        return if (process is DebugProcessImpl) PositionManager(process) else null
    }
}

class PositionManager(process: DebugProcessImpl) : PositionManagerImpl(process) {

    override fun getPsiFileByLocation(project: Project?, location: Location?): PsiFile? {
        val psiFile = super.getPsiFileByLocation(project, location)
        return psiFile
    }

    override fun findMethod(container: PsiElement?, className: String?, methodName: String?, methodSignature: String?): PsiMethod? {
        val psiMethod = super.findMethod(container, className, methodName, methodSignature)
        return psiMethod
    }

    override fun locationsOfLine(type: ReferenceType, position: SourcePosition): MutableList<Location> {
        val locations = super.locationsOfLine(type, position)
        return locations
    }

    override fun getDebugProcess(): DebugProcess {
        return super.getDebugProcess()
    }

    override fun createPrepareRequests(requestor: ClassPrepareRequestor, position: SourcePosition): MutableList<ClassPrepareRequest> {
        val prepareRequests = super.createPrepareRequests(requestor, position)
        return prepareRequests
    }

    override fun getAllClasses(position: SourcePosition): MutableList<ReferenceType> {
        val classes = super.getAllClasses(position)
        return classes
    }

    override fun createPrepareRequest(requestor: ClassPrepareRequestor, position: SourcePosition): ClassPrepareRequest? {
        val prepareRequest = super.createPrepareRequest(requestor, position)
        return prepareRequest
    }

    override fun getSourcePosition(location: Location?): SourcePosition? {
        val sourcePosition = super.getSourcePosition(location)
        return sourcePosition
    }
}