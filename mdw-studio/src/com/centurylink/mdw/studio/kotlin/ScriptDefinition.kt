package com.centurylink.mdw.studio.kotlin

import org.jetbrains.kotlin.scripting.definitions.KotlinScriptDefinition

/**
 *  Add this in plugin.xml when ready:
 *   <extensions defaultExtensionNs="org.jetbrains.kotlin">
 *     <scriptDefinitionContributor implementation="com.centurylink.mdw.studio.kotlin.ScriptDefinitionContributer"/>
 *   </extensions>
 */

class ScriptDefinitionContributer : org.jetbrains.kotlin.idea.core.script.ScriptDefinitionContributor {
    override val id: String
        get() = ID

    override fun getDefinitions(): List<KotlinScriptDefinition> {
        return listOf()
    }

    companion object {
        const val ID = "com.centurylink.mdw.studio.kotlin.scriptDefinitionContributer"
    }
}