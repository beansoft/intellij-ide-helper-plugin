// Copyright 2000-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.intellij.plugins.java.ui.stacktrace;

import com.intellij.openapi.fileEditor.TextEditor;
import com.intellij.openapi.fileEditor.TextEditorWithPreview;
import com.intellij.openapi.project.ProjectUtil;

import org.jetbrains.annotations.NotNull;

/**
 * Text and stacktrace preview editor.
 * @author Jacky Liu(beansoft@126.com)
 */
public final class StacktraceEditorWithPreview extends TextEditorWithPreview {

  public StacktraceEditorWithPreview(@NotNull TextEditor editor, @NotNull JavaStacktraceFileEditor preview) {
    super(
      editor,
      preview,
      "Stack Trace",
      Layout.SHOW_EDITOR_AND_PREVIEW,
      false
    );

    preview.setMainEditor(editor.getEditor());

    final var project = ProjectUtil.currentOrDefaultProject(editor.getEditor().getProject());
    // final var settings = MarkdownSettings.getInstance(project);
    // myAutoScrollPreview = settings.isAutoScrollEnabled();

      // getTextEditor().getEditor().getScrollingModel().addVisibleAreaListener(new MyVisibleAreaListener(), this);
  }

  @Override
  protected void onLayoutChange(Layout oldValue, Layout newValue) {
    super.onLayoutChange(oldValue, newValue);
    // Editor tab will lose focus after switching to JCEF preview for some reason.
    // So we should explicitly request focus for our editor here.
    if (newValue == Layout.SHOW_PREVIEW) {
      requestFocusForPreview();
    }
  }

  private void requestFocusForPreview() {
    final var preferredComponent = myPreview.getPreferredFocusedComponent();
    if (preferredComponent != null) {
      preferredComponent.requestFocus();
      return;
    }
    myPreview.getComponent().requestFocus();
  }

}
