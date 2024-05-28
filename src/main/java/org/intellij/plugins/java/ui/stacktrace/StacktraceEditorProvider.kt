package org.intellij.plugins.java.ui.stacktrace

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreviewProvider

/**
 * Requires IDEA 2024.1+
 */
class StacktraceEditorProvider : TextEditorWithPreviewProvider(StacktraceFileEditorProvider()) {

  override fun createSplitEditor(firstEditor: TextEditor, secondEditor: FileEditor): FileEditor {
    require(secondEditor is JavaStacktraceFileEditor) { "Secondary editor should be JavaStacktraceFileEditor" }
    return StacktraceEditorWithPreview(firstEditor, secondEditor)
  }

}