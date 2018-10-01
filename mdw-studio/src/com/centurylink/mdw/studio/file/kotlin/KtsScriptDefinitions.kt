package com.centurylink.mdw.studio.file.kotlin

import com.centurylink.mdw.model.workflow.ActivityRuntimeContext
import com.intellij.execution.console.IdeConsoleRootType
import com.intellij.ide.scratch.ScratchFileService
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.JavaSdk
import com.intellij.openapi.projectRoots.ProjectJdkTable
import com.intellij.openapi.projectRoots.ex.PathUtilEx
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.kotlin.idea.caches.project.SdkInfo
import org.jetbrains.kotlin.idea.caches.project.getScriptRelatedModuleInfo
import org.jetbrains.kotlin.idea.core.script.BundledKotlinScriptDependenciesResolver
import org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor
import org.jetbrains.kotlin.script.KotlinScriptDefinition
import org.jetbrains.kotlin.script.util.scriptCompilationClasspathFromContextOrStlib
import org.jetbrains.kotlin.utils.PathUtil
import java.io.File
import kotlin.script.dependencies.Environment
import kotlin.script.dependencies.ScriptContents
import kotlin.script.experimental.dependencies.DependenciesResolver
import kotlin.script.experimental.dependencies.ScriptDependencies
import kotlin.script.experimental.dependencies.asSuccess

class KtsDefinitionContributor(private val project: Project) : ScriptDefinitionContributor {

    override val id = "mdwKtsDefinitionContributor"

    override fun getDefinitions(): List<KotlinScriptDefinition> {
        return listOf(KtsDefinition(project))
    }
}

class KtsDefinition(project: Project) : KotlinScriptDefinition(KtsScriptTemplate::class) {

    override val dependencyResolver = BundledKotlinScriptDependenciesResolver(project)

    override fun isScript(fileName: String): Boolean {
        println("SCRIPT: " + fileName)
        return super.isScript(fileName)
    }
}

class KtsScriptTemplate(var variables: MutableMap<String,Any>, val runtimeContext: ActivityRuntimeContext) {

}

class KtsDependenciesResolver(private val project: Project) : DependenciesResolver {
    override fun resolve(
            scriptContents: ScriptContents,
            environment: Environment
    ): DependenciesResolver.ResolveResult {
        val virtualFile = scriptContents.file?.let { VfsUtil.findFileByIoFile(it, true) }

        val javaHome = getScriptSDK(project, virtualFile)

        var classpath = with(PathUtil.kotlinPathsForIdeaPlugin) {
            listOf(reflectPath, stdlibPath, scriptRuntimePath)
        }
        if (ScratchFileService.getInstance().getRootType(virtualFile) is IdeConsoleRootType) {
            classpath = scriptCompilationClasspathFromContextOrStlib(wholeClasspath = true) + classpath
        }

        return ScriptDependencies(javaHome = javaHome?.let(::File), classpath = classpath).asSuccess()
    }

    private fun getScriptSDK(project: Project, virtualFile: VirtualFile?): String? {
        if (virtualFile != null) {
            val dependentModuleSourceInfo = getScriptRelatedModuleInfo(project, virtualFile)
            val sdk = dependentModuleSourceInfo?.dependencies()?.filterIsInstance<SdkInfo>()?.singleOrNull()?.sdk
            if (sdk != null) {
                return sdk.homePath
            }
        }

        val jdk = ProjectRootManager.getInstance(project).projectSdk
                ?: ProjectJdkTable.getInstance().allJdks.firstOrNull { sdk -> sdk.sdkType is JavaSdk }
                ?: PathUtilEx.getAnyJdk(project)
        return jdk?.homePath
    }
}