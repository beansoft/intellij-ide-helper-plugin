package org.intellij.plugins.java.unscramble;

import com.intellij.execution.Executor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.impl.ConsoleViewUtil;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.execution.ui.ExecutionConsole;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.actionSystem.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.unscramble.AnalyzeStacktraceUtil;
import com.intellij.unscramble.UnscrambleDialog;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.regex.Pattern;

/**
 * @see AnalyzeStacktraceUtil
 */
public class AnalyzeStacktraceUtilEx {
    private static final Pattern STACKTRACE_LINE =
            Pattern.compile(
                    "[\t]*at [[_a-zA-Z0-9/]+\\.]+[_a-zA-Z$0-9/]+\\.[a-zA-Z0-9_/]+\\([A-Za-z0-9_/]+\\.(java|kt):[\\d]+\\)+[ [~]*\\[[a-zA-Z0-9\\.\\:/]\\]]*");


    /**
     * If a text contains stacktrace.
     * @see com.intellij.unscramble.UnscrambleListener#canHandle(String)
     * @param value
     * @return
     */
    public static boolean canHandle(@NotNull String value) {
        value = UnscrambleDialog.normalizeText(value);
        int linesCount = 0;
        for (String line : value.split("\n")) {
            line = line.trim();
            if (line.length() == 0) continue;
            line = StringUtil.trimEnd(line, "\r");
            if (STACKTRACE_LINE.matcher(line).matches()) {
                linesCount++;
            }
            else {
                linesCount = 0;
            }
            if (linesCount > 2) return true;
        }
        return false;
    }

    public static RunContentDescriptor addConsole(Project project,
                                                  @Nullable AnalyzeStacktraceUtil.ConsoleFactory consoleFactory,
                                                  final @NlsContexts.TabTitle String tabTitle,
                                                  String text,
                                                  @Nullable Icon icon) {
        final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        builder.filters(AnalyzeStacktraceUtil.EP_NAME.getExtensions(project));
        final ConsoleView consoleView = builder.getConsole();

        final DefaultActionGroup toolbarActions = new DefaultActionGroup();
        JComponent consoleComponent = consoleFactory != null
                ? consoleFactory.createConsoleComponent(consoleView, toolbarActions)
                : new MyConsolePanel(consoleView, toolbarActions);
        final RunContentDescriptor descriptor =
                new RunContentDescriptor(consoleView, null, consoleComponent, tabTitle, icon) {
                    @Override
                    public boolean isContentReuseProhibited() {
                        return true;
                    }
                };

        final Executor executor = DefaultRunExecutor.getRunExecutorInstance();
        for (AnAction action: consoleView.createConsoleActions()) {
            toolbarActions.add(action);
        }
        final ConsoleViewImpl console = (ConsoleViewImpl)consoleView;
        // 如果上面不创建组件 createConsoleComponent， 这里Editor 就是NPE
        ConsoleViewUtil.enableReplaceActionForConsoleViewEditor(console.getEditor());
        console.getEditor().getSettings().setCaretRowShown(true);
        toolbarActions.add(ActionManager.getInstance().getAction("AnalyzeStacktraceToolbar"));

        // RunContentManager.getInstance(project).showRunContent(executor, descriptor);
        consoleView.allowHeavyFilters();
        if (consoleFactory == null) {
            AnalyzeStacktraceUtil.printStacktrace(consoleView, text);
        }
        return descriptor;
    }

    /**
     * 创建堆栈分析控制台
     * @param project
     * @param consoleFactory
     * @param text
     * @return
     */
    public static JComponent addConsole(Project project,
                                                  @Nullable AnalyzeStacktraceUtil.ConsoleFactory consoleFactory,
                                             @Nullable String text) {
        final TextConsoleBuilder builder = TextConsoleBuilderFactory.getInstance().createBuilder(project);
        builder.filters(AnalyzeStacktraceUtil.EP_NAME.getExtensions(project));
        final ConsoleView consoleView = builder.getConsole();

        final DefaultActionGroup toolbarActions = new DefaultActionGroup();
        JComponent consoleComponent = consoleFactory != null
                ? consoleFactory.createConsoleComponent(consoleView, toolbarActions)
                : new MyConsolePanel(consoleView, toolbarActions);

        for (AnAction action: consoleView.createConsoleActions()) {
            toolbarActions.add(action);
        }

        final ConsoleViewImpl console = (ConsoleViewImpl)consoleView;
        // 如果上面不创建组件 createConsoleComponent， 这里Editor 就是NPE
        ConsoleViewUtil.enableReplaceActionForConsoleViewEditor(console.getEditor());
        console.getEditor().getSettings().setCaretRowShown(true);
        toolbarActions.add(ActionManager.getInstance().getAction("AnalyzeStacktraceToolbar"));

        consoleView.allowHeavyFilters();
        if (consoleFactory == null && text != null) {
            AnalyzeStacktraceUtil.printStacktrace(consoleView, text);
        }

        return consoleComponent;
        // return createStacktraceConsoleComponent(consoleFactory, consoleView);
    }

    /**
     * Create the StacktraceConsoleComponent 创建堆栈分析控制台 Java 组件
     * @param consoleFactory AnalyzeStacktraceUtil.ConsoleFactory
     * @param consoleView ConsoleView
     * @return ConsoleView component
     */
    public static JComponent createStacktraceConsoleComponent(@Nullable AnalyzeStacktraceUtil.ConsoleFactory consoleFactory,
                                                              ConsoleView consoleView) {
        final DefaultActionGroup toolbarActions = new DefaultActionGroup();
        for (AnAction action: consoleView.createConsoleActions()) {
            toolbarActions.add(action);
        }
        toolbarActions.add(ActionManager.getInstance().getAction("AnalyzeStacktraceToolbar"));

        return consoleFactory != null
                ? consoleFactory.createConsoleComponent(consoleView, toolbarActions)
                : new MyConsolePanel(consoleView, toolbarActions);
    }

    private static final class MyConsolePanel extends JPanel {
        MyConsolePanel(ExecutionConsole consoleView, ActionGroup toolbarActions) {
            super(new BorderLayout());
            JPanel toolbarPanel = new JPanel(new BorderLayout());
            ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(ActionPlaces.ANALYZE_STACKTRACE_PANEL_TOOLBAR, toolbarActions, false);
            toolbar.setTargetComponent(consoleView.getComponent());
            toolbarPanel.add(toolbar.getComponent());
            add(toolbarPanel, BorderLayout.WEST);
            add(consoleView.getComponent(), BorderLayout.CENTER);
        }
    }
}
