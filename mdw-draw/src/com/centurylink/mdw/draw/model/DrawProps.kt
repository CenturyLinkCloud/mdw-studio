package com.centurylink.mdw.draw.model

import com.centurylink.mdw.config.PropertyGroup

data class DrawProps(
        val isReadonly: Boolean = false,
        val isYaml: Boolean = false,
        val milestoneGroups: PropertyGroup? = null
)