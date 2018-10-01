package com.centurylink.mdw.studio

import com.intellij.openapi.help.WebHelpProvider

class MdwHelp : WebHelpProvider() {

    override fun getHelpPageUrl(helpTopicId: String) = Topics[helpTopicId]

    companion object {
        val PREFIX = "${MdwHelp::class.java.`package`.name}.help"
        val CREATE_PROJECT = "$PREFIX.createProject"

        val Topics = mapOf(
            CREATE_PROJECT to "http://centurylinkcloud.github.io/mdw/docs/guides/mdw-studio/#12-create-and-open-a-project"
        )

    }

}