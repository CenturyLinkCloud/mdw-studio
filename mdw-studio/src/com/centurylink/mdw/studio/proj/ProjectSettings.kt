package com.centurylink.mdw.studio.proj

import com.centurylink.mdw.studio.MdwSettings
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.project.Project

/**
 * Project-level settings (see MdwSettings for globals).
 */
class ProjectSettings(private val project: Project) {

    var isSuppressPreCommitAssetVercheck: Boolean
        get() = PropertiesComponent.getInstance(project).getBoolean(SUPPRESS_PRECOMMIT_ASSET_VERCHECK, false)
        set(value) {
            PropertiesComponent.getInstance(project).setValue(SUPPRESS_PRECOMMIT_ASSET_VERCHECK, value)
        }

    var gitUser: String
        get() = PropertiesComponent.getInstance(project).getValue(GIT_USER, "")
        set(value) {
            PropertiesComponent.getInstance(project).setValue(GIT_USER, value)
        }

    var gitPassword: String
        get() = PropertiesComponent.getInstance(project).getValue(GIT_PASSWORD, "")
        set(value) {
            PropertiesComponent.getInstance(project).setValue(GIT_PASSWORD, value)
        }


    companion object {
        const val ID = "com.centurylink.mdw.studio.proj"

        // vcs
        private const val SUPPRESS_PRECOMMIT_ASSET_VERCHECK = "${MdwSettings.ID}.suppressPreCommitAssetVercheck"
        private const val GIT_USER = "$ID.gitUser"
        private const val GIT_PASSWORD = "$ID.gitPassword"
    }

}