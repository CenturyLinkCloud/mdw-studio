package com.centurylink.mdw.draw.model

class Data {

    companion object {
        // TODO these values should not be static and should not be hardcoded
        const val SOURCE_REPO_URL = "https://github.com/CenturyLinkCloud/mdw"
        const val HELP_LINK_URL = "http://centurylinkcloud.github.io/mdw/docs"
        const val BASE_PKG = "com.centurylink.mdw.base"

        val documentTypes = mapOf(
                "org.w3c.dom.Document" to "xml",
                "org.apache.xmlbeans.XmlObject" to "xml",
                "java.lang.Object" to "java",
                "org.json.JSONObject" to "json",
                "groovy.util.Node" to "xml",
                "com.centurylink.mdw.xml.XmlBeanWrapper" to "xml",
                "com.centurylink.mdw.model.StringDocument" to "text",
                "com.centurylink.mdw.model.HTMLDocument" to "html",
                "javax.xml.bind.JAXBElement" to "xml",
                "org.apache.camel.component.cxf.CxfPayload" to "xml",
                "com.centurylink.mdw.common.service.Jsonable" to "json",
                "com.centurylink.mdw.model.Jsonable" to "json",
                "org.yaml.snakeyaml.Yaml" to "yaml",
                "java.lang.Exception" to "json",
                "java.util.List<Integer>" to "json",
                "java.util.List<Long>" to "json",
                "java.util.List<String>" to "json",
                "java.util.Map<String,String>" to "json"
        )

        // TODO better groups handling
        // (make this configurable somehow by the user)
        val workgroups = listOf("MDW Support", "Site Admin", "Developers")
        val categories = mapOf(
                "Ordering" to "ORD",
                "General Inquiry" to "GEN",
                "Billing" to "BIL",
                "Complaint" to "COM",
                "Portal Support" to "POR",
                "Training" to "TRN",
                "Repair" to "RPR",
                "Inventory" to "INV",
                "Test" to "TST",
                "Vacation Planning" to "VAC",
                "Customer Contact" to "CNT"
        )
    }
}