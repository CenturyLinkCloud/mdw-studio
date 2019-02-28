package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.cli.Download
import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.util.ui.UIUtil
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.ReadContext
import org.json.JSONArray
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.io.File
import java.net.URL
import java.nio.file.Files
import javax.swing.*
import javax.swing.event.DocumentEvent

class SearchDialog(projectSetup: ProjectSetup, private val widget: Pagelet.Widget) :
        DialogWrapper(projectSetup.project, false) {

    private val centerPanel = JPanel(BorderLayout())
    private val okButton: JButton? = getButton(okAction)

    private val searchText = SearchTextField()

    private val listModel = DefaultListModel<SearchResult>()
    private val resultsList = object: JBList<SearchResult>(listModel) {
        override fun getMinimumSize(): Dimension {
            return Dimension(350, 450)
        }
    }

    var selectedResult: SearchResult? = null

    private val jsonArray: JSONArray? by lazy {
        widget.attributes["searchUrl"]?.let { searchUrl ->
            val url = URL(searchUrl)
            val tempDir = Files.createTempDirectory("mdw-studio-${widget.name}")
            val fileName = File(url.file).name
            val jsonFile = File("$tempDir/$fileName")
            if (!jsonFile.isFile) {
                resultsList.setEmptyText("Loading...")
                resultsList.repaint()
                Download(url, jsonFile).run()
                resultsList.setEmptyText("Nothing to show")
                resultsList.repaint()
            }
            JSONArray(String(Files.readAllBytes(jsonFile.toPath())))
        }
    }

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    override fun getPreferredFocusedComponent(): JComponent {
        return searchText
    }

    init {
        init()
        title = widget.label
        okButton?.isEnabled = false

        val searchProps = widget.attributes["searchProps"]?.split(",")
        searchText.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                listModel.removeAllElements()
                resultsList.repaint()
                val search = e.document.getText(0, e.document.length)
                if (search.length > 1) {
                    jsonArray?.let { ja ->
                        val searchResults = mutableListOf<SearchResult>()
                        for (i in 0 until ja.length()) {
                            val json = ja.getJSONObject(i)
                            for (name in JSONObject.getNames(json)) {
                                if (searchProps == null || searchProps.contains(name)) {
                                    val value = json.optString(name)
                                    if (value.toLowerCase().contains(search.toLowerCase())) {
                                        val paths = widget.widgets.map { it.attributes["path"] }
                                        searchResults.add(SearchResult(json, paths[0]!!, paths[1]))
                                        break
                                    }
                                }
                            }
                        }
                        searchResults.sort()
                        searchResults.forEach { listModel.addElement(it) }
                        resultsList.repaint()
                    }
                }
            }
        })

        centerPanel.add(searchText, BorderLayout.NORTH)

        val listPanel = JPanel(BorderLayout())
        listPanel.border = BorderFactory.createEmptyBorder(4, 4, 4, 4)
        val borderColor = if (UIUtil.isUnderDarcula()) Color.GRAY else JBColor.border()
        resultsList.border = BorderFactory.createLineBorder(borderColor)
        resultsList.selectionMode = ListSelectionModel.SINGLE_SELECTION
        resultsList.addListSelectionListener { event ->
            selectedResult = listModel.elementAt(event.firstIndex)
            okButton?.isEnabled = true
        }
        listPanel.add(resultsList, BorderLayout.CENTER)
        centerPanel.add(listPanel, BorderLayout.CENTER)
    }
}

class SearchResult(val json: JSONObject, val name: String, val description: String?) : Comparable<SearchResult> {

    val label: String by lazy {
        if (isPath(name)) {
            evalPath(name)
        } else {
            name
        }
    }

    private val readContext: ReadContext by lazy {
        JsonPath.parse(json.toString())
    }

    override fun toString(): String {
        return label
    }

    fun evalPath(path: String): String {
        return if (isPath(path)) {
            val value = StringBuilder()
            path.split("/").forEach { segment ->
                if (value.isNotEmpty()) {
                    value.append("/")
                }
                value.append(readContext.read(segment) as String)
            }

            return value.toString()
        } else {
            path
        }
    }

    override fun compareTo(other: SearchResult): Int {
        return label.compareTo(other.label)
    }

    companion object {
        fun isPath(name: String) = name.contains("\$.")
    }

}