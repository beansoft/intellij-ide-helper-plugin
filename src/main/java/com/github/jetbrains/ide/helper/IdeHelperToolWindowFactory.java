package com.github.jetbrains.ide.helper;

import com.intellij.DynamicBundle;
import com.intellij.ide.IdeBundle;
import com.intellij.openapi.application.PathManager;
import com.intellij.openapi.extensions.PluginDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowFactory;
import com.intellij.ui.components.JBScrollPane;
import com.intellij.ui.components.JBTextArea;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import org.jetbrains.annotations.NotNull;

import java.util.Properties;

/**
 * Main entry for the Tool window.
 */
public class IdeHelperToolWindowFactory implements ToolWindowFactory, DumbAware {
//    private static final Icon FIND_ICON =
//            ExperimentalUI.isNewUI() ? IconManager.getInstance().getIcon
//                    ("newui/jsx_13_gray_20x20.svg", RNToolWindowFactory.class)
//                    : IconManager.getInstance().getIcon
//                    ("newui/jsx_13_gray.svg", RNToolWindowFactory.class);

    public static final String TOOL_WINDOW_ID = "IDE Helper";
    public final static String PLUGIN_ID = "com.github.jetbrains.ide.helper";

    public IdeHelperToolWindowFactory() {
//        String cName = "#" + CodeStyleSettings.class.getName();
//        System.out.println(cName);
    }

    @Override
    public void createToolWindowContent(@NotNull Project project, @NotNull ToolWindow toolWindow) {
//        toolWindow.setIcon(FIND_ICON);
        JBTextArea output = new JBTextArea();
        Content content = ContentFactory.getInstance().createContent(new JBScrollPane(output), "Paths", false);
        content.setCloseable(false);
//        content.setDisplayName(NotesBundle.message("tab.evernote.title.viewer"));
//        content.setDescription(NotesBundle.message("tooltip.syncs.with.evernote.view.content"));
//        content.setIcon(PluginIcons.Evernote);
//        content.setPopupIcon(PluginIcons.Evernote);
//        content.putUserData(ToolWindow.SHOW_CONTENT_ICON, Boolean.TRUE);
        toolWindow.getContentManager().addContent(content);

        output.append("" + PathManager.PROPERTY_HOME_PATH + "=" + FileUtil.toSystemIndependentName(PathManager.getHomePath()));
        output.append("\n" + PathManager.PROPERTY_SYSTEM_PATH + "=" +  FileUtil.toSystemIndependentName(PathManager.getSystemPath()));
        output.append("\n" + PathManager.PROPERTY_CONFIG_PATH + "=" +  FileUtil.toSystemIndependentName(PathManager.getConfigPath()));
        output.append("\n" + PathManager.PROPERTY_PLUGINS_PATH + "=" + FileUtil.toSystemIndependentName(PathManager.getPluginsPath()));
        output.append("\n" + PathManager.PROPERTY_LOG_PATH + "=" + FileUtil.toSystemIndependentName(PathManager.getLogPath()));
        output.append("\nindex_root_path=" + FileUtil.toSystemIndependentName(PathManager.getIndexRoot().toString()));

        final DynamicBundle.LanguageBundleEP languageBundle = DynamicBundle.findLanguageBundle();
        if (languageBundle != null) {
            final PluginDescriptor pluginDescriptor = languageBundle.pluginDescriptor;
            final ClassLoader loader = pluginDescriptor == null ? null : pluginDescriptor.getClassLoader();
            final String bundlePath = loader == null ? null : PathManager.getResourceRoot(loader, "META-INF/plugin.xml");
            if (bundlePath != null) {
                output.append("\nLANGUAGE_BUNDLE=" + FileUtil.toSystemIndependentName(bundlePath));
            }
        }

        Properties properties = System.getProperties();
        output.append("\njava.home=" + properties.getProperty("java.home"));
        String javaVersion = properties.getProperty("java.runtime.version", properties.getProperty("java.version", "unknown"));
        String arch = properties.getProperty("os.arch", "");
        String jreInfo = IdeBundle.message("about.box.jre", javaVersion, arch);
        output.append("\n" +jreInfo);

        String vmVersion = properties.getProperty("java.vm.name", "unknown");
        String vmVendor = properties.getProperty("java.vendor", "unknown");
        String vmVendorInfo = IdeBundle.message("about.box.vm", vmVersion, vmVendor);
        output.append("\n" + vmVendorInfo);

        System.out.println(output.getText());
    }



    public boolean isApplicable(@NotNull Project project) {
        return true;
    }
}
