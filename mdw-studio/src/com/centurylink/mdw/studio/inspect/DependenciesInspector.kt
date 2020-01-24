package com.centurylink.mdw.studio.inspect

import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.vfs.VirtualFile

interface DependenciesInspector {
    fun doInspect(projectSetup: ProjectSetup, files: List<VirtualFile>)
}