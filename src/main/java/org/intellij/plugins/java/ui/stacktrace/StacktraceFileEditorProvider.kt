package org.intellij.plugins.java.ui.stacktrace

import org.intellij.plugins.java.unscramble.AnalyzeStacktraceUtilEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.WeighedFileEditorProvider
import com.intellij.openapi.fileTypes.FileTypeRegistry
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * 重型文件编辑器
 */
class StacktraceFileEditorProvider: WeighedFileEditorProvider() {
  override fun accept(project: Project, file: VirtualFile): Boolean {
    return FileTypeRegistry.getInstance().isFileOfType(file, PlainTextFileType.INSTANCE) && isStackTraceFile(project,
    file)
  }

  private fun isStackTraceFile(project: Project, file: VirtualFile): Boolean {
    val document = checkNotNull(FileDocumentManager.getInstance().getDocument(file))
    val text = document.text
    return AnalyzeStacktraceUtilEx.canHandle(text)
  }

  override fun createEditor(project: Project, file: VirtualFile): FileEditor {
    return JavaStacktraceFileEditor(project, file)
  }

  override fun getEditorTypeId(): String {
    return "stacktrace-preview-editor"
  }

  override fun getPolicy(): FileEditorPolicy {
    return FileEditorPolicy.PLACE_AFTER_DEFAULT_EDITOR
  }
}
