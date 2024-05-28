// Copyright 2000-2021 JetBrains s.r.o. and contributors. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.java.ui.stacktrace

import org.intellij.plugins.html.util.JavaStackTracePluginScope
import org.intellij.plugins.java.unscramble.AnalyzeStacktraceUtilEx
import org.intellij.plugins.java.unscramble.UnscrambleDialogEx
import com.intellij.execution.ui.RunContentDescriptor
import org.intellij.plugins.java.freezeAnalyzer.FreezeAnalyzer
import com.intellij.openapi.application.EDT
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.options.advanced.AdvancedSettings
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolder
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.content.Content
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.content.ContentManager
import com.intellij.ui.content.TabbedPaneContentUI
import com.intellij.util.concurrency.annotations.RequiresEdt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.awt.BorderLayout
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.beans.PropertyChangeListener
import java.lang.ref.WeakReference
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingConstants

class JavaStacktraceFileEditor(private val project: Project, private val file: VirtualFile) :
  UserDataHolder by UserDataHolderBase(), FileEditor {
  private val document = checkNotNull(FileDocumentManager.getInstance().getDocument(file))

  private val mainPanelWrapper: JPanel = JPanel(BorderLayout()).apply { addComponentListener(AttachPanelOnVisibilityChangeListener()) }

  /**
   * 真正用来渲染UI 的组件所在的 Pane.
   */
  private var panel: JComponent? = null

  private var mainEditor = MutableStateFlow<Editor?>(null)

  private var isDisposed: Boolean = false

  private val coroutineScope = JavaStackTracePluginScope.createChildScope(project)

  private var myContentManager: ContentManager? = null

  private var oldDescriptor:RunContentDescriptor? = null

  init {
    println("JavaStacktraceFileEditor init()")
    document.addDocumentListener(ReparseContentDocumentListener(), this)

    coroutineScope.launch(Dispatchers.EDT) { attachStacktracePanel() }
  }

  fun setMainEditor(editor: Editor) {
    check(mainEditor.value == null)
    mainEditor.value = editor
  }


  override fun getComponent(): JComponent {
    println("JavaStacktraceFileEditor getComponent() this.myContentManager =" + this.myContentManager)
//    if(this.myContentManager != null) {
//      return this.myContentManager!!.component
//    }

    return mainPanelWrapper
  }

  override fun getPreferredFocusedComponent(): JComponent? {
    if(this.myContentManager != null) {
      return this.myContentManager!!.component
    }
    return mainPanelWrapper
  }

  override fun getName(): String {
    return "Stack Trace"
  }

  override fun setState(state: FileEditorState) {}

  override fun isModified(): Boolean {
    return false
  }

  override fun isValid(): Boolean {
    return true
  }

  override fun selectNotify() {
    if (panel != null) {
      coroutineScope.launch(Dispatchers.EDT) { updateStacktracePane() }
    }
  }

  override fun addPropertyChangeListener(listener: PropertyChangeListener) {}

  override fun removePropertyChangeListener(listener: PropertyChangeListener) {}

  override fun getFile(): VirtualFile {
    return file
  }

  override fun dispose() {
    if (panel != null) {
      detachStacktracePanel()
    }

    myContentManager?.dispose()

    isDisposed = true
    coroutineScope.cancel()
  }

  @RequiresEdt
  private suspend fun updateStacktracePane() {
    val contentManager = this.myContentManager ?: return
    if (!file.isValid || isDisposed) {
      return
    }

    detachStacktracePanel()

    val editor = mainEditor.firstOrNull() ?: return
    val offset = editor.caretModel.offset
    val descriptor = UnscrambleDialogEx.showUnscrambledText(
      null, null, null, project, document.text ) ?: return
//      panel.setHtml(lastRenderedHtml, offset, file)

    // Save for future dispose
    oldDescriptor = descriptor

    var content = createNewContent(descriptor)

    content.executionId = descriptor.executionId
    content.component = descriptor.component
    content.setPreferredFocusedComponent(descriptor.preferredFocusComputable)
    content.putUserData(RunContentDescriptor.DESCRIPTOR_KEY, descriptor)
//    content.putUserData(EXECUTOR_KEY, executor)
    content.displayName = descriptor.displayName

//    descriptor.displayNameProperty.afterChange(descriptor) {
//      application.invokeLater {
//        content.displayName = it
//      }
//    }

    descriptor.setAttachedContent(content)

//    val content = ContentFactory.getInstance().createContent(pane, "Stack Trace", true)


    contentManager.addContent(content)


    val freezeAnalysisResult = FreezeAnalyzer.analyzeFreeze(document.text)
    if (freezeAnalysisResult != null) {
      val freezeAnalysisComponent = AnalyzeStacktraceUtilEx.addConsole(project, null, freezeAnalysisResult.message +
              "\n======= Stack Trace: ========= \n" + freezeAnalysisResult.stackTrace )
      content = ContentFactory.getInstance().createContent(freezeAnalysisComponent, "Freeze Analyzer", true)
      content.icon = icons.Icons.Freeze.icon
      contentManager.addContent(content)
    }


//    this.panel = JBScrollPane(pane)
//    putUserData(PREVIEW_BROWSER, WeakReference(panel))
    mainPanelWrapper.add(contentManager.component, BorderLayout.CENTER)

    if (mainPanelWrapper.isShowing) mainPanelWrapper.validate()
    mainPanelWrapper.repaint()

//    val panel = this.panel ?: return

  }

  private fun createNewContent(descriptor: RunContentDescriptor): Content {
    val content = ContentFactory.getInstance().createContent(descriptor.component, descriptor.displayName, true)
    content.putUserData(ToolWindow.SHOW_CONTENT_ICON, true)
    if (AdvancedSettings.getBoolean("start.run.configurations.pinned")) content.isPinned = true
    content.icon = descriptor.icon
    return content
  }

  @RequiresEdt
  private fun detachStacktracePanel() {
    val contentManager = this.myContentManager ?: return
    contentManager.removeAllContents(true)
    // Avoid mem leak?
    oldDescriptor?.dispose()
  }

  @RequiresEdt
  private suspend fun attachStacktracePanel() {
//    val settings = MarkdownSettings.getInstance(project)
//    val panel = retrievePanelProvider(settings).createHtmlPanel(project, file)
    val contentFactory = ContentFactory.getInstance()
    this.myContentManager = contentFactory.createContentManager(
      TabbedPaneContentUI(SwingConstants.TOP),
      false, project)
    updateStacktracePane()
  }

  // Updated when the source doc is changed
  private inner class ReparseContentDocumentListener : DocumentListener {
    override fun documentChanged(event: DocumentEvent) {
      coroutineScope.launch(Dispatchers.EDT) { updateStacktracePane() }
    }
  }

  private inner class AttachPanelOnVisibilityChangeListener : ComponentAdapter() {
    override fun componentShown(event: ComponentEvent) {
      if (panel == null) {
//        coroutineScope.launch(Dispatchers.EDT) { attachStacktracePanel() }
      }
    }

    override fun componentHidden(event: ComponentEvent) {
      if (panel != null) {
//        detachStacktracePanel()
      }
    }

    override fun componentResized(event: ComponentEvent) {
      if (panel != null) {
//        panel!!.revalidate()
//        if (mainPanelWrapper.isShowing) mainPanelWrapper.revalidate()
//        mainPanelWrapper.repaint()

//        println("mainPanelWrapper width=${mainPanelWrapper.width}")
//        println("mainPanelWrapper preferredWidth=${mainPanelWrapper.preferredWidth}")
//        println("panel width=${panel!!.width}")
//        println("panel preferredWidth=${panel!!.preferredWidth}")

//        coroutineScope.launch(Dispatchers.EDT) {
//          panel!!.revalidate()
//        }

//        detachHtmlPanel()
//        coroutineScope.launch(Dispatchers.EDT) {
//          attachHtmlPanel()
//          mainPanelWrapper.add(panel, BorderLayout.CENTER)
//
//          if (mainPanelWrapper.isShowing) mainPanelWrapper.validate()
//          mainPanelWrapper.repaint()
//        }
      }
    }

  }

  companion object {
    val PREVIEW_BROWSER: Key<WeakReference<JComponent>> = Key.create("STACKTRACE_BROWSER")
  }
}
