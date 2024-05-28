package icons

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

enum class Icons(private val path: String) {
    Freeze("/newui/freeze.svg");

    @JvmField
    val icon: Icon

    init {
        icon = IconLoader.getIcon(path, javaClass)
    }
}