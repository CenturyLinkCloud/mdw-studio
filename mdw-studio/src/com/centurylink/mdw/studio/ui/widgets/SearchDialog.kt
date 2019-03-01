package com.centurylink.mdw.studio.ui.widgets

import com.centurylink.mdw.cli.Fetch
import com.centurylink.mdw.draw.edit.label
import com.centurylink.mdw.model.asset.Pagelet
import com.centurylink.mdw.studio.proj.ProjectSetup
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.DocumentAdapter
import com.intellij.ui.JBColor
import com.intellij.ui.SearchTextField
import com.intellij.ui.components.JBList
import com.intellij.util.concurrency.SwingWorker
import com.intellij.util.ui.UIUtil
import com.jayway.jsonpath.JsonPath
import com.jayway.jsonpath.PathNotFoundException
import com.jayway.jsonpath.ReadContext
import org.json.JSONArray
import org.json.JSONObject
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.net.URL
import javax.swing.*
import javax.swing.event.DocumentEvent

class SearchDialog(projectSetup: ProjectSetup, private val widget: Pagelet.Widget) :
        DialogWrapper(projectSetup.project) {

    private val centerPanel = JPanel(BorderLayout())
    private val okButton: JButton?
        get() = getButton(okAction)

    private val searchText = SearchTextField()

    private val listModel = DefaultListModel<SearchResult>()
    private val resultsList = object: JBList<SearchResult>(listModel) {
        override fun getMinimumSize(): Dimension {
            return Dimension(350, 450)
        }

        override fun getToolTipText(event: MouseEvent): String? {
            val idx = locationToIndex(event.point)
            if (idx > -1) {
                listModel.elementAt(idx)?.let { searchResult ->
                    return searchResult.description
                }
            }
            return super.getToolTipText(event)
        }
    }

    var selectedResult: SearchResult? = null

    private val jsonArray: JSONArray by lazy {
        val searchUrl = widget.attributes["searchUrl"]
        if (searchUrl == null) {
            JSONArray()
        } else {
            JSONArray(Fetch(URL(searchUrl)).get())
        }
    }

    override fun createCenterPanel(): JComponent {
        return centerPanel
    }

    override fun getPreferredFocusedComponent(): JComponent? {

        object: SwingWorker() {
            override fun construct(): Any? {
                return try {
                    jsonArray // initialize
                    null
                } catch (ex: Exception) {
                    LOG.warn(ex)
                    ex
                }
            }
            override fun finished() {
                resultsList.setEmptyText("Nothing to show")
                resultsList.repaint()
                val res = get()
                if (res is Exception) {
                    JOptionPane.showMessageDialog(centerPanel, res.message,
                            "Search Load Error", JOptionPane.PLAIN_MESSAGE, AllIcons.General.ErrorDialog)
                } else {
                    searchText.isEnabled = true
                    searchText.grabFocus()
                }
            }
        }.start()

        return super.getPreferredFocusedComponent()
    }

    init {
        init()
        title = widget.label

        searchText.isEnabled = false
        okButton?.isEnabled = false

        val searchProps = widget.attributes["searchProps"]?.split(",")
        searchText.addDocumentListener(object: DocumentAdapter() {
            override fun textChanged(e: DocumentEvent) {
                listModel.removeAllElements()
                resultsList.repaint()
                val search = e.document.getText(0, e.document.length)
                if (search.length > 1) {
                    jsonArray.let { ja ->
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
                        null
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
        resultsList.addMouseListener(object: MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                val index = resultsList.locationToIndex(event.point)
                if (index > -1 && !listModel.isEmpty) {
                    selectedResult = listModel.elementAt(index)
                    okButton?.isEnabled = true
                    if (event.clickCount >= 2) {
                        okButton?.doClick()
                    }
                }
                else {
                    selectedResult = null
                    okButton?.isEnabled = false
                }
            }
        })

        listPanel.add(resultsList, BorderLayout.CENTER)
        centerPanel.add(listPanel, BorderLayout.CENTER)

        resultsList.setEmptyText("Loading...")
    }

    companion object {
        val LOG = Logger.getInstance(SearchDialog::class.java)
    }
}

class SearchResult(val json: JSONObject, val name: String, val descrip: String?) : Comparable<SearchResult> {

    val label: String by lazy {
        if (isPath(name)) {
            evalPath(name)
        } else {
            name
        }
    }

    val description: String? by lazy {
        descrip?.let {
            if (isPath(it)) {
                evalPath(it)
            } else {
                it
            }
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
            if (path == "\$") {
                json.toString()
            }
            else {
                try {
                    val value = StringBuilder()
                    path.split("/", "\n").forEach { segment ->
                        if (value.isNotEmpty()) {
                            if (path.contains("\n")) {
                                value.append("\n")
                            } else {
                                value.append(" / ")
                            }
                        }
                        value.append((readContext.read(segment) as Any).toString())
                    }
                    value.toString()
                } catch (ex: PathNotFoundException) {
                    LOG.warn(ex)
                    ""
                }
            }
        } else {
            path
        }
    }

    override fun compareTo(other: SearchResult): Int {
        return label.compareTo(other.label)
    }

    companion object {
        val LOG = Logger.getInstance(SearchResult::class.java)
        fun isPath(name: String) = name.contains("\$.") || name == "\$"
    }
}